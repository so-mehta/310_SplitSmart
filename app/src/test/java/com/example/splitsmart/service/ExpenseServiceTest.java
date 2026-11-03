package com.example.splitsmart.service;

import com.example.splitsmart.exception.PermissionException;
import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.Category;
import com.example.splitsmart.model.Expense;
import com.example.splitsmart.model.Group;
import com.example.splitsmart.model.Share;
import com.example.splitsmart.model.SplitMethod;
import com.example.splitsmart.repository.ExpenseRepository;
import com.example.splitsmart.repository.GroupRepository;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ExpenseServiceTest {

    private static final String GROUP_ID = "group-1";
    private static final String OTHER_GROUP_ID = "group-2";
    private static final String EXPENSE_ID = "expense-1";
    private static final String ALICE = "alice";
    private static final String BOB = "bob";
    private static final String CAROL = "carol";
    private static final String OUTSIDER = "outsider";
    private static final LocalDate ORIGINAL_DATE = LocalDate.of(2026, 1, 15);

    private ExpenseRepository expenseRepository;
    private GroupRepository groupRepository;
    private ExpenseService expenseService;
    private Expense stored;

    @Before
    public void setUp() {
        expenseRepository = mock(ExpenseRepository.class);
        groupRepository = mock(GroupRepository.class);
        expenseService = new ExpenseService(new SplitCalculator(), expenseRepository, groupRepository);

        when(groupRepository.findById(GROUP_ID)).thenReturn(new Group(GROUP_ID, "Apartment", "",
                ALICE, new ArrayList<>(Arrays.asList(ALICE, BOB, CAROL))));

        stored = newExpense();
        stored.setTransactionId(EXPENSE_ID);
        stored.setCreatorId(BOB);
        stored.setDate(ORIGINAL_DATE);
        when(expenseRepository.findById(EXPENSE_ID)).thenReturn(stored);
    }

    @Test
    public void createExpense_savesItDatedTodayWithTheCallerAsCreator() {
        Expense expense = newExpense();

        Expense created = expenseService.createExpense(BOB, expense);

        assertSame(expense, created);
        assertEquals(BOB, created.getCreatorId());
        assertEquals(LocalDate.now(), created.getDate());
        verify(expenseRepository).save(expense);
    }

    @Test
    public void createExpense_ignoresCreatorAndDateOnTheExpensePassedIn() {
        Expense expense = newExpense();
        expense.setCreatorId(ALICE);
        expense.setDate(ORIGINAL_DATE);

        expenseService.createExpense(BOB, expense);

        assertEquals(BOB, expense.getCreatorId());
        assertEquals(LocalDate.now(), expense.getDate());
    }

    @Test
    public void createExpense_payerWithNoShare_isAccepted() {
        Expense expense = newExpense();
        expense.setShares(shares(BOB, 500, CAROL, 500));

        expenseService.createExpense(ALICE, expense);

        verify(expenseRepository).save(expense);
    }

    @Test
    public void createExpense_zeroCentShare_isAccepted() {
        Expense expense = newExpense();
        expense.setAmountCents(1);
        expense.setShares(shares(ALICE, 1, BOB, 0));

        expenseService.createExpense(ALICE, expense);

        verify(expenseRepository).save(expense);
    }

    @Test
    public void createExpense_byNonMember_isRejected() {
        assertThrows(PermissionException.class,
                () -> expenseService.createExpense(OUTSIDER, newExpense()));

        verify(expenseRepository, never()).save(any());
    }

    @Test
    public void createExpense_missingGroup_isRejected() {
        Expense expense = newExpense();
        expense.setGroupId(OTHER_GROUP_ID);

        assertCreateRejected(expense);
    }

    @Test
    public void createExpense_nullGroupId_isRejectedBeforeAnyLookup() {
        Expense expense = newExpense();
        expense.setGroupId(null);

        assertCreateRejected(expense);
        verify(groupRepository, never()).findById(any());
    }

    @Test
    public void createExpense_emptyTitle_isRejected() {
        for (String title : Arrays.asList("", "   ", null)) {
            Expense expense = newExpense();
            expense.setTitle(title);

            assertCreateRejected(expense);
        }
    }

    @Test
    public void createExpense_amountOfZeroOrLess_isRejected() {
        for (long cents : new long[]{0, -1000}) {
            Expense expense = newExpense();
            expense.setAmountCents(cents);

            assertCreateRejected(expense);
        }
    }

    @Test
    public void createExpense_payerWhoIsNotAMember_isRejected() {
        Expense expense = newExpense();
        expense.setPayerId(OUTSIDER);

        assertCreateRejected(expense);
    }

    @Test
    public void createExpense_noPayer_isRejected() {
        Expense expense = newExpense();
        expense.setPayerId(null);

        assertCreateRejected(expense);
    }

    @Test
    public void createExpense_participantWhoIsNotAMember_isRejected() {
        Expense expense = newExpense();
        expense.setShares(shares(ALICE, 500, OUTSIDER, 500));

        assertCreateRejected(expense);
    }

    @Test
    public void createExpense_fewerThanTwoParticipants_isRejected() {
        Expense expense = newExpense();
        expense.setShares(Collections.singletonList(new Share(ALICE, 1000, 0)));

        assertCreateRejected(expense);
    }

    @Test
    public void createExpense_noShares_isRejected() {
        Expense emptyShares = newExpense();
        emptyShares.setShares(new ArrayList<>());
        Expense nullShares = newExpense();
        nullShares.setShares(null);

        assertCreateRejected(emptyShares);
        assertCreateRejected(nullShares);
    }

    @Test
    public void createExpense_twoSharesForOneParticipant_isRejected() {
        Expense expense = newExpense();
        expense.setShares(Arrays.asList(
                new Share(ALICE, 300, 0), new Share(ALICE, 300, 0), new Share(BOB, 400, 0)));

        assertCreateRejected(expense);
    }

    @Test
    public void createExpense_nullShareInTheList_isRejected() {
        Expense expense = newExpense();
        expense.setShares(Arrays.asList(new Share(ALICE, 500, 0), new Share(BOB, 500, 0), null));

        assertCreateRejected(expense);
    }

    @Test
    public void createExpense_noCategory_isRejected() {
        Expense expense = newExpense();
        expense.setCategory(null);

        assertCreateRejected(expense);
    }

    @Test
    public void createExpense_noSplitMethod_isRejected() {
        Expense expense = newExpense();
        expense.setSplitMethod(null);

        assertCreateRejected(expense);
    }

    @Test
    public void createExpense_sharesThatDoNotSumToTheAmount_areRejected() {
        Expense expense = newExpense();
        expense.setShares(shares(ALICE, 500, BOB, 499));

        assertCreateRejected(expense);
    }

    @Test
    public void createExpense_negativeShare_isRejected() {
        Expense expense = newExpense();
        expense.setShares(shares(ALICE, -100, BOB, 1100));

        assertCreateRejected(expense);
    }

    @Test
    public void updateExpense_savesTheNewValues() {
        Expense edited = editedExpense();
        edited.setTitle("Groceries");
        edited.setAmountCents(2000);
        edited.setShares(shares(BOB, 1000, CAROL, 1000));

        expenseService.updateExpense(BOB, edited);

        verify(expenseRepository).update(edited);
        assertEquals("Groceries", edited.getTitle());
        assertEquals(2000, edited.getAmountCents());
    }

    @Test
    public void updateExpense_keepsTheStoredCreatorGroupAndDate() {
        Expense edited = editedExpense();
        edited.setCreatorId(null);
        edited.setDate(LocalDate.now());
        edited.setGroupId(OTHER_GROUP_ID);

        expenseService.updateExpense(BOB, edited);

        assertEquals(BOB, edited.getCreatorId());
        assertEquals(ORIGINAL_DATE, edited.getDate());
        assertEquals(GROUP_ID, edited.getGroupId());
    }

    @Test
    public void updateExpense_byNonCreator_isRejected() {
        assertThrows(PermissionException.class,
                () -> expenseService.updateExpense(CAROL, editedExpense()));

        verify(expenseRepository, never()).update(any());
    }

    @Test
    public void updateExpense_fakedCreatorOnTheExpensePassedIn_isRejected() {
        Expense edited = editedExpense();
        edited.setCreatorId(CAROL);

        assertThrows(PermissionException.class, () -> expenseService.updateExpense(CAROL, edited));

        verify(expenseRepository, never()).update(any());
    }

    @Test
    public void updateExpense_creatorNoLongerInTheGroup_isRejected() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(new Group(GROUP_ID, "Apartment", "",
                ALICE, new ArrayList<>(Arrays.asList(ALICE, CAROL))));
        Expense edited = editedExpense();
        edited.setShares(shares(ALICE, 500, CAROL, 500));

        assertThrows(PermissionException.class, () -> expenseService.updateExpense(BOB, edited));

        verify(expenseRepository, never()).update(any());
    }

    @Test
    public void updateExpense_missingExpense_isRejected() {
        when(expenseRepository.findById(EXPENSE_ID)).thenReturn(null);

        assertThrows(ValidationException.class,
                () -> expenseService.updateExpense(BOB, editedExpense()));

        verify(expenseRepository, never()).update(any());
    }

    @Test
    public void updateExpense_noExpenseId_isRejectedBeforeAnyLookup() {
        Expense edited = editedExpense();
        edited.setTransactionId(null);

        assertThrows(ValidationException.class, () -> expenseService.updateExpense(BOB, edited));

        verify(expenseRepository, never()).findById(any());
        verify(expenseRepository, never()).update(any());
    }

    @Test
    public void updateExpense_missingGroup_isRejected() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(null);

        assertThrows(ValidationException.class,
                () -> expenseService.updateExpense(BOB, editedExpense()));

        verify(expenseRepository, never()).update(any());
    }

    @Test
    public void updateExpense_runsTheSameChecksAsCreate() {
        Expense emptyTitle = editedExpense();
        emptyTitle.setTitle("");
        Expense badAmount = editedExpense();
        badAmount.setAmountCents(0);
        Expense outsiderPayer = editedExpense();
        outsiderPayer.setPayerId(OUTSIDER);
        Expense outsiderParticipant = editedExpense();
        outsiderParticipant.setShares(shares(BOB, 500, OUTSIDER, 500));
        Expense oneParticipant = editedExpense();
        oneParticipant.setShares(Collections.singletonList(new Share(BOB, 1000, 0)));
        Expense noCategory = editedExpense();
        noCategory.setCategory(null);
        Expense badShares = editedExpense();
        badShares.setShares(shares(BOB, 500, CAROL, 499));

        for (Expense invalid : Arrays.asList(emptyTitle, badAmount, outsiderPayer,
                outsiderParticipant, oneParticipant, noCategory, badShares)) {
            assertThrows(ValidationException.class, () -> expenseService.updateExpense(BOB, invalid));
        }

        verify(expenseRepository, never()).update(any());
    }

    @Test
    public void updateExpense_failedCheck_leavesTheExpensePassedInUnchanged() {
        Expense edited = editedExpense();
        edited.setCreatorId(null);
        edited.setDate(null);
        edited.setTitle("");

        assertThrows(ValidationException.class, () -> expenseService.updateExpense(BOB, edited));

        assertEquals(null, edited.getCreatorId());
        assertEquals(null, edited.getDate());
    }

    @Test
    public void deleteExpense_byCreator_deletesIt() {
        expenseService.deleteExpense(BOB, EXPENSE_ID);

        verify(expenseRepository).delete(EXPENSE_ID);
    }

    @Test
    public void deleteExpense_byNonCreator_isRejected() {
        assertThrows(PermissionException.class, () -> expenseService.deleteExpense(CAROL, EXPENSE_ID));

        verify(expenseRepository, never()).delete(any());
    }

    @Test
    public void deleteExpense_byGroupOwnerWhoIsNotTheCreator_isRejected() {
        assertThrows(PermissionException.class, () -> expenseService.deleteExpense(ALICE, EXPENSE_ID));

        verify(expenseRepository, never()).delete(any());
    }

    @Test
    public void deleteExpense_missingExpense_isRejected() {
        when(expenseRepository.findById(EXPENSE_ID)).thenReturn(null);

        assertThrows(ValidationException.class, () -> expenseService.deleteExpense(BOB, EXPENSE_ID));

        verify(expenseRepository, never()).delete(any());
    }

    @Test
    public void getExpense_returnsIt() {
        assertSame(stored, expenseService.getExpense(EXPENSE_ID));
    }

    @Test
    public void getExpense_missingExpense_isRejected() {
        when(expenseRepository.findById(EXPENSE_ID)).thenReturn(null);

        assertThrows(ValidationException.class, () -> expenseService.getExpense(EXPENSE_ID));
    }

    @Test
    public void previewShares_splitsInInputOrderWithNoRepositoryCalls() {
        ExpenseRepository unusedExpenses = mock(ExpenseRepository.class);
        GroupRepository unusedGroups = mock(GroupRepository.class);
        ExpenseService service = new ExpenseService(new SplitCalculator(), unusedExpenses, unusedGroups);

        List<Share> shares = service.previewShares(1000, SplitMethod.EQUAL, inputs(CAROL, 0.0, ALICE, 0.0, BOB, 0.0));

        assertEquals(CAROL, shares.get(0).getUserId());
        assertEquals(334, shares.get(0).getShareCents());
        assertEquals(ALICE, shares.get(1).getUserId());
        assertEquals(333, shares.get(1).getShareCents());
        assertEquals(BOB, shares.get(2).getUserId());
        assertEquals(333, shares.get(2).getShareCents());
        verifyNoInteractions(unusedExpenses, unusedGroups);
    }

    @Test
    public void previewShares_usesExactAmounts() {
        List<Share> shares = expenseService.previewShares(1000, SplitMethod.EXACT_AMOUNT,
                inputs(ALICE, 700.0, BOB, 300.0));

        assertEquals(700, shares.get(0).getShareCents());
        assertEquals(300, shares.get(1).getShareCents());
    }

    @Test
    public void previewShares_usesPercentages() {
        List<Share> shares = expenseService.previewShares(10000, SplitMethod.PERCENTAGE,
                inputs(ALICE, 75.0, BOB, 25.0));

        assertEquals(7500, shares.get(0).getShareCents());
        assertEquals(2500, shares.get(1).getShareCents());
    }

    @Test
    public void previewShares_invalidInputs_areRejected() {
        assertThrows(ValidationException.class,
                () -> expenseService.previewShares(1000, SplitMethod.EXACT_AMOUNT, inputs(ALICE, 500.0, BOB, 499.0)));
    }

    @Test
    public void previewShares_nullInputs_areRejected() {
        assertThrows(ValidationException.class,
                () -> expenseService.previewShares(1000, SplitMethod.EQUAL, null));
    }

    private void assertCreateRejected(Expense expense) {
        assertThrows(ValidationException.class, () -> expenseService.createExpense(BOB, expense));

        verify(expenseRepository, never()).save(any());
    }

    /** A valid expense that has not been saved: no ID, creator or date yet. */
    private static Expense newExpense() {
        Expense expense = new Expense("Dinner", Category.FOOD, ALICE, SplitMethod.EQUAL,
                shares(ALICE, 334, BOB, 333, CAROL, 333));
        expense.setGroupId(GROUP_ID);
        expense.setAmountCents(1000);
        return expense;
    }

    /** What the edit screen sends for the stored expense. */
    private static Expense editedExpense() {
        Expense expense = newExpense();
        expense.setTransactionId(EXPENSE_ID);
        expense.setCreatorId(BOB);
        expense.setDate(ORIGINAL_DATE);
        return expense;
    }

    private static List<Share> shares(Object... idsAndCents) {
        List<Share> shares = new ArrayList<>();
        for (int i = 0; i < idsAndCents.length; i += 2) {
            shares.add(new Share((String) idsAndCents[i], (Integer) idsAndCents[i + 1], 0));
        }
        return shares;
    }

    private static Map<String, Double> inputs(Object... idsAndValues) {
        Map<String, Double> inputs = new LinkedHashMap<>();
        for (int i = 0; i < idsAndValues.length; i += 2) {
            inputs.put((String) idsAndValues[i], (Double) idsAndValues[i + 1]);
        }
        return inputs;
    }
}
