# SplitSmart frontend handoff

## Status

Expense, history and summary frontend source is implemented against the v2.1
division-of-labor contracts. This is **not an end-to-end working app yet**.
Latest upstream main at 2ced75d2c11126daaf942f39e8c6b0ad934e3489 now has AccountService,
GroupService, ExpenseService and SplitCalculator skeletons, plus their application
getters. ExpenseService and SplitCalculator still throw UnsupportedOperationException.
History/Summary services, their application getters and GroupDetail navigation
remain teammate-owned dependencies. No existing shared production files were modified.
The proposed integration patch is separate and has not been applied.

The original local workspace was reconstructed through GitHub file APIs because shell git
networking was unavailable. Its baseline commit is a local snapshot, not the
original Git history. Apply the supplied frontend patch to your real checkout;
do not push this reconstructed history. The remote feature branch was created
from upstream main at 2ced75d and contains only these frontend additions for
review. The shared wiring changes remain an unapplied proposal.

## Sources read

- SplitSmart_Division_of_Labor_v2.1.pdf: all 16 pages, especially K3-K12,
  Section 2 signatures, C1-C6, integration steps and testing checklist.
- Project2.3_Implementation.pdf: both pages.
- Project 2.1 CS 310.pdf: all 14 pages, including FR 2, 3, 6 and 7.
- SplitSmart_2.2_edited.pdf: all 11 pages (Revised v1.1).
- All Java domain models, exceptions and repository interfaces in upstream main;
  repository implementation constructors and the Android project configuration.

The uploaded 2.2 document ends after Figure 1 (the domain-model diagram).
It describes five class-diagram figures and sequence diagrams, but the remaining
figures and sequences are not included. Full UI/service diagram conformance
therefore remains unverified; service signatures still come from the labor plan.

## Your source files

| Package | Files | Behavior |
| --- | --- | --- |
| ui.common | AmountParser, MoneyFormatter | Integer cents; exact parsing; USD display; no floating-point money conversion |
| ui.expense | AddEditExpenseActivity / ViewModel | Create/edit form, participant selection, equal/exact/percentage input, service preview, validation, saved draft |
| ui.expense | ExpenseDetailActivity / ViewModel | All expense fields and shares, creator name, creator-only edit/delete, deletion confirmation |
| ui.history | HistoryFragment / ViewModel | Mixed transaction rows, keyword/type/category/member filters, newest/oldest order, debounced reads, expense navigation |
| ui.history | MonthlySummaryFragment / ViewModel | Month picker, previous/next, expense count, total and category breakdown |
| test/ui.common | MoneyTest | 17 JUnit tests |

All network-backed service calls run on ViewModel executors. previewShares is
the only service method called on the main thread; the contract defines it as
pure and fast. There are no UI imports of Firebase or repositories, no app
fake services, and no backend implementations added.

## Integration dependencies

1. Alex implements the existing ExpenseService and SplitCalculator skeletons.
   Their current signatures match the frontend calls.
2. Somya merges HistoryService, HistoryFilter, SummaryService and MonthlySummary.
3. Rosycela adds getHistoryService() and getSummaryService() to the application
   wiring object; getExpenseService() already exists on current main.
4. Simra registers the two expense activities, adds lifecycle dependencies and
   supplies group/member context from GroupDetail's shared ViewModel.

The UI currently assumes HistoryFilter and MonthlySummary are in the
service package with JavaBean getters on MonthlySummary:
getExpenseCount(), getTotalCents(), getCategoryTotals(). Their package/getter
names are not specified by the supplied plan. Confirm these with Somya and
adjust UI imports/getters if her actual classes differ.

### Proposed shared wiring

integration/frontend-wiring.patch contains proposed edits to:

- app/build.gradle.kts: lifecycle ViewModel, saved-state, LiveData and RecyclerView dependencies.
- AndroidManifest.xml: two non-exported expense activities.
- SplitSmartApp.java: construction and getters for the three required services.

Do not apply it before the real services exist. Reconcile it with Rosycela's
wiring if her work has since merged; keep one instance of each repository and
service. It is a proposal for the owners, not an additional service layer.

### GroupDetail integration

Once GroupDetailViewModel has loaded the group and members, construct a
LinkedHashMap in the displayed member order:

```java
LinkedHashMap<String, String> names = new LinkedHashMap<>();
for (User member : members) names.put(member.getUserId(), member.getName());

HistoryFragment history = HistoryFragment.newInstance(groupId, userId, names);
MonthlySummaryFragment summary =
        MonthlySummaryFragment.newInstance(groupId, userId, names);
```

Pass these to the existing tab adapter. Do not recreate fragments every time
LiveData emits. The host should create them only after its shared group data
is ready; it owns authentication and navigation.

The group and user IDs are passed with extras named groupId and userId.
Members are parallel String ArrayList extras named memberIds and memberNames.
Both expense activities expose intent(...) helpers, so call those rather
than duplicating extra names. expenseId is absent for creation.

This implementation accepts a member snapshot from the host instead of
importing a nonexistent GroupDetailViewModel API. The intended data flow
remains GroupDetail shared ViewModel -> member snapshot -> child UI. When
Simra's shared API lands, reconcile fragment subscriptions with K12, especially
if members can change while the group screen is open. No child calls
GroupService or UserRepository.

History and summary refresh on resume; after expense editing, detail reloads.
The form and history/month selections use SavedStateHandle. The form's back
button and Cancel ask before discarding changes.

## Validation performed

- 17/17 MoneyTest tests passed using JUnit 4.13.2 and JDK 21.
- 6/6 isolated expense-draft contract checks passed (Other default, required
  payer, exact/percentage mismatch feedback, title validation, saved draft values).
  These use temporary service signatures and fixed responses, not a real backend.
- All frontend Java sources compiled with Java 11 language compatibility
  against real cached Android/AndroidX API JARs and temporary service contracts.
- The temporary contract classes throw AssertionError; they exist only in a
  temporary check directory and never enter app/src/main.
- Layer import check and patch whitespace check passed.

Repeat the isolated check on a machine with cached AndroidX/JUnit artifacts:

```sh
JAVA_HOME=/path/to/jdk ANDROID_HOME=/path/to/android-sdk python3 tools/check_frontend.py
```

This check does not test service behavior, Firebase, layout rendering, full
Gradle dependency resolution, navigation, or lifecycle behavior on a device.
An APK build and emulator run were not performed. The checked-in upstream
configuration requests SDK 37 and a JDK 25 Gradle daemon; this host has an SDK
37.0 platform and JDK 21/24. Full Gradle toolchain resolution was not verified.
Keep the team's toolchain settings consistent.

After dependencies and shared wiring merge:

```sh
bash gradlew testDebugUnitTest assembleDebug
```

## Device checks still required

- Create a $10.00 equal split for three members: $3.34 / $3.33 / $3.33.
- Create $0.01 across two members: $0.01 / $0.00. Zero exact shares are allowed;
  the total must still be positive.
- Reject an exact sum of $9.99 for a $10.00 total, and reject 33.33% x 3.
- Exact and percentage splits must use previewShares; confirm backend rounding.
- Verify the payer is selected and cannot be unchecked; changing the payer
  must select and lock the new payer. The previous payer remains optional.
- Rotate the form after entering title, note, participant selection and custom
  amounts; keep editing after the back-button warning.
- Edit an existing expense; verify every field, including percentages.
- Open as creator and non-creator; exercise successful delete, denied delete,
  missing expense and connection errors.
- Test every history filter independently and combined, plus no results,
  both sorts and fast repeated searches.
- Verify settlements display payer/receiver names and amounts.
- Change months across a year boundary, confirm empty month = 0 and $0.00,
  and confirm settlements never enter the summary.
- Test a small screen, large font, long titles and 500 transactions.

## Implementation notes for the team's document

Native views are assembled programmatically inside Activities/Fragments.
ViewModels own draft, loading, error and result state. No new top-level app
classes were added outside the allowed screen/ViewModel/helper categories.

Exact shares use the documented Map<String, Double> service contract, which
cannot represent every long cent value. The UI rejects exact shares above
2^53 - 1 cents to prevent silent precision loss. The amount parser and USD
formatter themselves support the entire long range. Any service contract
change to remove Double belongs in a coordinated design change.

The UI verifies non-negative shares, unique participants and the final total,
but service validation remains authoritative for permissions and writes.

Generated by Codex: the new frontend Java files, money tests, isolated check
script, handoff document and proposed wiring patch. Human device checks have
not been completed. The team should record the actual AI tool used rather
than attributing this code to Claude.

## Reconciliation with the supplied requirements

Project 2.1 takes precedence for the frontend where the labor plan differs:

| Requirement | Frontend behavior |
| --- | --- |
| FR 2.5 | Category defaults to Other |
| FR 2.6 | Payer is included and cannot be deselected; at least one other participant is required |
| FR 2.7.2 and add-expense failure flow | Exact shares show the dollar difference, short or over, and block Save |
| FR 2.7.3 | Percentage mismatch shows its current total and blocks Save |
| FR 3.4 | Editing preserves the original date |
| FR 3.5 | Delete confirmation uses the specified question |
| FR 6.2 | History labels Expense/Settlement and displays involved members |
| FR 6.6 | Expenses open their detail activity; settlements open a dialog with all stored fields |
| History failure flow | No transactions yet. for an unfiltered empty group; No transactions found for empty filtered results |
| FR 7.2 and empty-month flow | Count, full expense totals and all five category totals, including zeros |

The labor plan explicitly allowed a payer without a share, which conflicts
with FR 2.6. The updated frontend follows FR 2.6. Alex should also enforce
that rule in ExpenseService; UI enforcement alone is not authorization.
No backend method signatures were changed.

Settlement edit/delete remains in Simra's C4 scope. This update supplies
read-only settlement details from history; it does not implement her settlement
management workflow. The host must refresh its balance data when returning
from an expense or settlement mutation; that code belongs to GroupDetail.

History uses RecyclerView with recycled rows as listed in the design.
