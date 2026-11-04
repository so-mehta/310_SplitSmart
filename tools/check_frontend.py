#!/usr/bin/env python3
"""Isolated API compilation + money tests. Does NOT build an APK or test real services.

Missing service contracts exist only inside a temporary directory. They have no
implementation and are never copied into the Android app.
"""
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
CACHE = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")) / "caches/modules-2/files-2.1"
SDK = Path(os.environ.get("ANDROID_HOME", os.environ.get("ANDROID_SDK_ROOT", Path.home() / "Library/Android/sdk")))
JAVA = Path(os.environ["JAVA_HOME"]) / "bin" if "JAVA_HOME" in os.environ else None


def java_tool(name):
    value = str(JAVA / name) if JAVA else shutil.which(name)
    if not value:
        raise SystemExit("Set JAVA_HOME to an installed JDK.")
    return value


def run(command):
    subprocess.run(command, cwd=ROOT, check=True)


with tempfile.TemporaryDirectory(prefix="splitsmart-api-check-") as folder:
    tmp = Path(folder)
    platforms = sorted(SDK.glob("platforms/android-*/android.jar"),
                       key=lambda p: tuple(int(x) for x in re.findall(r"\d+", p.parent.name)))
    if not platforms or not CACHE.is_dir():
        raise SystemExit("Install an Android SDK and resolve AndroidX/JUnit dependencies first.")
    cp = [str(platforms[-1])]
    for group in sorted(CACHE.iterdir()):
        if not (group.name.startswith(("androidx.", "org.jetbrains", "com.google.android.material"))
                or group.name in ("junit", "org.hamcrest")):
            continue
        for artifact in sorted(group.iterdir()):
            versions = sorted(artifact.iterdir(),
                              key=lambda p: tuple(int(x) for x in re.findall(r"\d+", p.name)),
                              reverse=True)
            for version in versions:
                aars = list(version.rglob("*.aar"))
                jars = [p for p in version.rglob("*.jar")
                        if "sources" not in p.name and "javadoc" not in p.name]
                if not aars and not jars:
                    continue
                for aar in aars:
                    with zipfile.ZipFile(aar) as archive:
                        if "classes.jar" in archive.namelist():
                            dest = tmp / (group.name + "-" + artifact.name + ".jar")
                            dest.write_bytes(archive.read("classes.jar"))
                            cp.append(str(dest))
                cp.extend(map(str, jars))
                break

    contracts = {
        "ExpenseService": """
            public Expense createExpense(String caller, Expense e) { throw new AssertionError(); }
            public void updateExpense(String caller, Expense e) { throw new AssertionError(); }
            public void deleteExpense(String caller, String id) { throw new AssertionError(); }
            public Expense getExpense(String id) { throw new AssertionError(); }
            public List<Share> previewShares(long total, SplitMethod method, Map<String,Double> inputs) { throw new AssertionError(); }
        """,
        "HistoryService": "public List<Transaction> getHistory(String group, HistoryFilter filter, SortOrder order) { throw new AssertionError(); }",
        "HistoryFilter": "public HistoryFilter(String keyword, TransactionType type, Category category, String member) {}",
        "SummaryService": "public MonthlySummary getMonthlySummary(String group, java.time.YearMonth month) { throw new AssertionError(); }",
        "MonthlySummary": """
            public int getExpenseCount() { throw new AssertionError(); }
            public long getTotalCents() { throw new AssertionError(); }
            public Map<Category,Long> getCategoryTotals() { throw new AssertionError(); }
        """
    }
    fixture = tmp / "contracts/com/example/splitsmart"
    service = fixture / "service"
    service.mkdir(parents=True)
    for name, body in contracts.items():
        (service / (name + ".java")).write_text(
            "package com.example.splitsmart.service; import java.util.*; "
            "import com.example.splitsmart.model.*; public class " + name + " { " + body + " }")
    (fixture / "SplitSmartApp.java").write_text("""
        package com.example.splitsmart;
        import com.example.splitsmart.service.*;
        public class SplitSmartApp extends android.app.Application {
            public ExpenseService getExpenseService() { throw new AssertionError(); }
            public HistoryService getHistoryService() { throw new AssertionError(); }
            public SummaryService getSummaryService() { throw new AssertionError(); }
        }
    """)
    base = ROOT / "app/src/main/java/com/example/splitsmart"
    files = (list(base.glob("ui/**/*.java")) + list(base.glob("model/*.java")) +
             list(base.glob("exception/*.java")) + list((tmp / "contracts").rglob("*.java")) +
             list((ROOT / "app/src/test/java/com/example/splitsmart/ui").rglob("*.java")) +
             list((ROOT / "tools/contract_checks").rglob("*.java")))
    classes = tmp / "classes"
    classes.mkdir()
    classpath = os.pathsep.join(cp)
    print("Compiling against Android APIs + temporary service signatures (no backend behavior).", flush=True)
    run([java_tool("javac"), "--release", "11", "-proc:none", "-Xlint:-classfile",
         "-cp", classpath, "-d", str(classes)] + list(map(str, files)))
    run([java_tool("java"), "-cp", str(classes) + os.pathsep + classpath,
         "org.junit.runner.JUnitCore", "com.example.splitsmart.ui.common.MoneyTest",
         "com.example.splitsmart.ui.expense.ExpenseDraftContractTest"])
    print("PASS: isolated source/API compilation, money tests and expense-draft contract checks. Full app/device integration remains pending.")
