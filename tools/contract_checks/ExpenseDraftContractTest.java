package com.example.splitsmart.ui.expense;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import androidx.lifecycle.SavedStateHandle;
import com.example.splitsmart.model.*;
import com.example.splitsmart.service.ExpenseService;
import org.junit.*;
import java.util.*;
import static org.junit.Assert.*;

/** UI-only checks compiled against the check script's temporary service signature.
 * This file is intentionally outside Android source sets; it does not test a real backend.
 */
public class ExpenseDraftContractTest {
    private AddEditExpenseViewModel vm;
    private int calls;
    @Before public void setup() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override public void executeOnDiskIO(Runnable task) { task.run(); }
            @Override public void postToMainThread(Runnable task) { task.run(); }
            @Override public boolean isMainThread() { return true; }
        });
        ExpenseService service = new ExpenseService() {
            @Override public List<Share> previewShares(long total, SplitMethod method, Map<String,Double> inputs) {
                calls++;
                // Fixed valid response, not an implementation of the real split calculator.
                return Arrays.asList(new Share("a", 500, 50), new Share("b", 500, 50));
            }
        };
        vm = new AddEditExpenseViewModel(service, new SavedStateHandle());
        LinkedHashMap<String,String> members = new LinkedHashMap<>();
        members.put("a", "Alice"); members.put("b", "Bob"); members.put("c", "Carol");
        vm.initialize("group", "a", null, members);
    }
    @After public void cleanup() { vm.onCleared(); ArchTaskExecutor.getInstance().setDelegate(null); }
    @Test public void defaultsOtherAndIncludesPayer() {
        assertEquals("OTHER", vm.text("category")); assertEquals("a", vm.text("payer"));
        assertTrue(vm.participants().contains("a"));
    }
    @Test public void payerCannotBeDeselected() {
        vm.select("a", false); assertTrue(vm.participants().contains("a"));
        vm.setText("payer", "b"); assertTrue(vm.participants().contains("b"));
        vm.select("b", false); assertTrue(vm.participants().contains("b"));
    }
    @Test public void exactMismatchShowsDifferenceAndBlocksPreview() {
        vm.select("b", true); vm.setText("amount", "10"); vm.setText("method", "EXACT_AMOUNT");
        vm.input("a", "5"); int before = calls; vm.input("b", "4.99");
        assertFalse(vm.valid.getValue()); assertTrue(vm.previewText.getValue().contains("$0.01 short"));
        assertEquals(before, calls);
        vm.input("b", "5.01"); assertTrue(vm.previewText.getValue().contains("$0.01 over"));
    }
    @Test public void percentageMismatchShowsActualTotal() {
        vm.select("b", true); vm.select("c", true); vm.setText("amount", "10");
        vm.setText("method", "PERCENTAGE");
        vm.input("a", "33.33"); vm.input("b", "33.33"); vm.input("c", "33.33");
        assertFalse(vm.valid.getValue()); assertTrue(vm.previewText.getValue().contains("99.99%"));
    }
    @Test public void titleIsRequiredAndValidDraftEnablesSave() {
        vm.select("b", true); vm.setText("amount", "10");
        assertFalse(vm.valid.getValue()); assertTrue(vm.previewText.getValue().contains("Enter an expense title"));
        vm.setText("title", "Dinner"); assertTrue(vm.valid.getValue());
    }
    @Test public void savedStateRestoresFieldsAndCustomInputs() {
        SavedStateHandle state = new SavedStateHandle();
        ExpenseService unused = new ExpenseService();
        AddEditExpenseViewModel first = new AddEditExpenseViewModel(unused, state);
        first.setText("title", "Rent"); first.setText("method", "EXACT_AMOUNT"); first.input("b", "450");
        AddEditExpenseViewModel restored = new AddEditExpenseViewModel(unused, state);
        assertEquals("Rent", restored.text("title")); assertEquals("450", restored.input("b"));
        first.onCleared(); restored.onCleared();
    }
}
