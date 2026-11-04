package com.example.splitsmart.service;

import com.example.splitsmart.model.Category;
import com.example.splitsmart.model.Expense;
import com.example.splitsmart.model.Settlement;
import com.example.splitsmart.model.Share;
import com.example.splitsmart.model.SortOrder;
import com.example.splitsmart.model.SplitMethod;
import com.example.splitsmart.model.Transaction;
import com.example.splitsmart.model.TransactionType;
import com.example.splitsmart.repository.ExpenseRepository;
import com.example.splitsmart.repository.SettlementRepository;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HistoryServiceTest {

    private static final String GROUP_ID = "group-1";
    private static final String ALICE = "alice";
    private static final String BOB = "bob";
    private static final String CAROL = "carol";

    private ExpenseRepository expenseRepository;
    private SettlementRepository settlementRepository;
    private HistoryService historyService;
    private List<Expense> expenses;
    private List<Settlement> settlements;

    @Before
    public void setUp() {
        expenseRepository = mock(ExpenseRepository.class);
        settlementRepository = mock(SettlementRepository.class);
        historyService = new HistoryService(expenseRepository, settlementRepository);

        expenses = new ArrayList<>(Arrays.asList(
                expense("expense-dinner", LocalDate.of(2026, 1, 10), "Dinner",
                        "Italian food", Category.FOOD, ALICE, ALICE, BOB),
                expense("expense-rent", LocalDate.of(2026, 2, 1), "Rent",
                        "February apartment", Category.HOUSING, BOB, BOB, CAROL)));
        settlements = new ArrayList<>(Arrays.asList(
                settlement("settlement-dinner", LocalDate.of(2026, 1, 15),
                        "Dinner repayment", BOB, ALICE),
                settlement("settlement-carol", LocalDate.of(2026, 2, 5),
                        "Paid back", CAROL, ALICE)));
        when(expenseRepository.findByGroup(GROUP_ID)).thenReturn(expenses);
        when(settlementRepository.findByGroup(GROUP_ID)).thenReturn(settlements);
    }

    @Test
    public void getHistory_mergesBothRepositoriesAndReadsEachOnce() {
        List<Transaction> history = historyService.getHistory(GROUP_ID,
                new HistoryFilter("", null, null, null), SortOrder.NEWEST_FIRST);

        assertEquals(4, history.size());
        verify(expenseRepository).findByGroup(GROUP_ID);
        verify(settlementRepository).findByGroup(GROUP_ID);
    }

    @Test
    public void keywordFilter_isCaseInsensitiveAndSearchesSupportedTextFields() {
        List<Transaction> history = historyService.getHistory(GROUP_ID,
                new HistoryFilter("DINNER", null, null, null), SortOrder.NEWEST_FIRST);

        assertEquals(Arrays.asList("settlement-dinner", "expense-dinner"), ids(history));
    }

    @Test
    public void typeFilter_returnsOnlyTheRequestedTransactionType() {
        List<Transaction> expensesOnly = historyService.getHistory(GROUP_ID,
                new HistoryFilter("", TransactionType.EXPENSE, null, null),
                SortOrder.NEWEST_FIRST);
        List<Transaction> settlementsOnly = historyService.getHistory(GROUP_ID,
                new HistoryFilter("", TransactionType.SETTLEMENT, null, null),
                SortOrder.NEWEST_FIRST);

        assertEquals(Arrays.asList("expense-rent", "expense-dinner"), ids(expensesOnly));
        assertEquals(Arrays.asList("settlement-carol", "settlement-dinner"),
                ids(settlementsOnly));
    }

    @Test
    public void categoryFilter_matchesExpensesAndDropsSettlements() {
        List<Transaction> history = historyService.getHistory(GROUP_ID,
                new HistoryFilter("", null, Category.FOOD, null), SortOrder.NEWEST_FIRST);

        assertEquals(Arrays.asList("expense-dinner"), ids(history));
    }

    @Test
    public void memberFilter_usesTransactionInvolves() {
        List<Transaction> history = historyService.getHistory(GROUP_ID,
                new HistoryFilter("", null, null, CAROL), SortOrder.NEWEST_FIRST);

        assertEquals(Arrays.asList("settlement-carol", "expense-rent"), ids(history));
    }

    @Test
    public void combinedFilters_allMustMatch() {
        List<Transaction> history = historyService.getHistory(GROUP_ID,
                new HistoryFilter("italian", TransactionType.EXPENSE, Category.FOOD, BOB),
                SortOrder.NEWEST_FIRST);

        assertEquals(Arrays.asList("expense-dinner"), ids(history));
    }

    @Test
    public void newestFirst_sortsByDescendingDate() {
        List<Transaction> history = historyService.getHistory(GROUP_ID, null,
                SortOrder.NEWEST_FIRST);

        assertEquals(Arrays.asList("settlement-carol", "expense-rent",
                "settlement-dinner", "expense-dinner"), ids(history));
    }

    @Test
    public void oldestFirst_sortsByAscendingDate() {
        List<Transaction> history = historyService.getHistory(GROUP_ID, null,
                SortOrder.OLDEST_FIRST);

        assertEquals(Arrays.asList("expense-dinner", "settlement-dinner",
                "expense-rent", "settlement-carol"), ids(history));
    }

    @Test
    public void equalDates_areOrderedByTransactionId() {
        expenses.clear();
        settlements.clear();
        LocalDate date = LocalDate.of(2026, 3, 1);
        expenses.add(expense("transaction-b", date, "B", "", Category.OTHER,
                ALICE, ALICE, BOB));
        settlements.add(settlement("transaction-a", date, "", BOB, ALICE));

        List<Transaction> history = historyService.getHistory(GROUP_ID, null,
                SortOrder.NEWEST_FIRST);

        assertEquals(Arrays.asList("transaction-a", "transaction-b"), ids(history));
    }

    @Test
    public void equalDates_newestFirst_putsTheLaterCreatedTransactionFirst() {
        expenses.clear();
        settlements.clear();
        LocalDate date = LocalDate.of(2026, 3, 1);
        Expense first = expense("transaction-a", date, "First", "", Category.OTHER,
                ALICE, ALICE, BOB);
        first.setCreatedAtMillis(1000);
        Expense second = expense("transaction-b", date, "Second", "", Category.OTHER,
                ALICE, ALICE, BOB);
        second.setCreatedAtMillis(2000);
        Settlement third = settlement("transaction-0", date, "", BOB, ALICE);
        third.setCreatedAtMillis(3000);
        expenses.add(first);
        expenses.add(second);
        settlements.add(third);

        List<Transaction> history = historyService.getHistory(GROUP_ID, null,
                SortOrder.NEWEST_FIRST);

        assertEquals(Arrays.asList("transaction-0", "transaction-b", "transaction-a"),
                ids(history));
    }

    @Test
    public void equalDates_oldestFirst_putsTheEarlierCreatedTransactionFirst() {
        expenses.clear();
        settlements.clear();
        LocalDate date = LocalDate.of(2026, 3, 1);
        Expense first = expense("transaction-b", date, "First", "", Category.OTHER,
                ALICE, ALICE, BOB);
        first.setCreatedAtMillis(1000);
        Expense second = expense("transaction-a", date, "Second", "", Category.OTHER,
                ALICE, ALICE, BOB);
        second.setCreatedAtMillis(2000);
        expenses.add(first);
        expenses.add(second);

        List<Transaction> history = historyService.getHistory(GROUP_ID, null,
                SortOrder.OLDEST_FIRST);

        assertEquals(Arrays.asList("transaction-b", "transaction-a"), ids(history));
    }

    @Test
    public void noMatches_returnsAnEmptyList() {
        List<Transaction> history = historyService.getHistory(GROUP_ID,
                new HistoryFilter("not present", null, null, null),
                SortOrder.NEWEST_FIRST);

        assertEquals(0, history.size());
    }

    private static List<String> ids(List<Transaction> transactions) {
        return transactions.stream().map(Transaction::getTransactionId)
                .collect(Collectors.toList());
    }

    private static Expense expense(String id, LocalDate date, String title, String note,
                                   Category category, String payerId, String... participants) {
        List<Share> shares = new ArrayList<>();
        for (String participant : participants) {
            shares.add(new Share(participant, 500, 0));
        }
        return new Expense(id, GROUP_ID, payerId, shares.size() * 500L, date, note, title,
                category, payerId, SplitMethod.EXACT_AMOUNT, shares);
    }

    private static Settlement settlement(String id, LocalDate date, String note,
                                         String payerId, String receiverId) {
        return new Settlement(id, GROUP_ID, payerId, 500, date, note, payerId, receiverId);
    }
}
