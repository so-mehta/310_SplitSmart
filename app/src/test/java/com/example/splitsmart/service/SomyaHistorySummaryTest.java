package com.example.splitsmart.service;

import com.example.splitsmart.model.*;
import com.example.splitsmart.repository.ExpenseRepository;
import com.example.splitsmart.repository.SettlementRepository;
import org.junit.Before;
import org.junit.Test;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Somya Mehta: five additional white-box cases; no Android or Firebase required. */
public class SomyaHistorySummaryTest {
    private static final String GROUP = "somya-history";
    private ExpenseRepository expenses;
    private SettlementRepository settlements;
    private HistoryService history;
    private SummaryService summaries;

    @Before public void setUp() {
        expenses = mock(ExpenseRepository.class);
        settlements = mock(SettlementRepository.class);
        history = new HistoryService(expenses, settlements);
        summaries = new SummaryService(expenses);
    }

    @Test public void history_nullSourcesAndNullRows_preserveTheAvailableTransactions() {
        when(expenses.findByGroup(GROUP)).thenReturn(null);
        when(settlements.findByGroup(GROUP)).thenReturn(null);
        assertTrue(history.getHistory(GROUP, null, null).isEmpty());

        Settlement payment = new Settlement("payment", GROUP, "alex", 500,
                LocalDate.of(2026, 11, 2), "", "alex", "somya");
        when(settlements.findByGroup(GROUP)).thenReturn(Arrays.asList(null, payment));
        assertEquals(Collections.singletonList(payment), history.getHistory(GROUP, null, null));

        Expense dinner = expense("dinner", LocalDate.of(2026, 11, 1), 1000, Category.FOOD);
        when(expenses.findByGroup(GROUP)).thenReturn(Arrays.asList(null, dinner));
        when(settlements.findByGroup(GROUP)).thenReturn(null);
        assertEquals(Collections.singletonList(dinner), history.getHistory(GROUP, null, null));
        verify(expenses, times(3)).findByGroup(GROUP);
        verify(settlements, times(3)).findByGroup(GROUP);
    }

    @Test public void history_blankFiltersAndNullText_useTheCorrectShortCircuitPaths() {
        Expense unnamed = expense("unnamed", LocalDate.of(2026, 11, 1), 100, Category.OTHER);
        unnamed.setTitle(null); unnamed.setNote(null);
        Expense titled = expense("titled", LocalDate.of(2026, 11, 2), 200, Category.FOOD);
        titled.setTitle("Campus Pizza"); titled.setNote(null);
        Settlement payment = new Settlement("payment", GROUP, "alex", 100,
                LocalDate.of(2026, 11, 3), null, "alex", "somya");
        when(expenses.findByGroup(GROUP)).thenReturn(Arrays.asList(unnamed, titled));
        when(settlements.findByGroup(GROUP)).thenReturn(Collections.singletonList(payment));

        assertEquals(Arrays.asList(payment, titled, unnamed), history.getHistory(GROUP,
                new HistoryFilter(" \t ", null, null, "  "), SortOrder.NEWEST_FIRST));
        // Nonblank search must not crash on either null field or invent a settlement title.
        assertEquals(Collections.singletonList(titled), history.getHistory(GROUP,
                new HistoryFilter(" PIZZA ", null, null, null), SortOrder.NEWEST_FIRST));
    }

    @Test public void history_nullDatesAndIds_sortLastInBothDirections() {
        LocalDate date = LocalDate.of(2026, 11, 1);
        Expense a = expense("a", date, 100, Category.FOOD);
        Expense b = expense("b", date, 100, Category.FOOD);
        Expense missingId = expense(null, date, 100, Category.FOOD);
        Expense missingDate = expense("old-import", null, 100, Category.OTHER);
        when(expenses.findByGroup(GROUP)).thenReturn(Arrays.asList(missingDate, missingId, b, a));
        when(settlements.findByGroup(GROUP)).thenReturn(Collections.emptyList());

        // Equal timestamps reach the ID tie-breaker; null dates/IDs are always last.
        List<Transaction> expected = Arrays.asList(a, b, missingId, missingDate);
        assertEquals(expected, history.getHistory(GROUP, null, SortOrder.NEWEST_FIRST));
        assertEquals(expected, history.getHistory(GROUP, null, SortOrder.OLDEST_FIRST));
    }

    @Test public void summary_missingDataAndMissingCategory_followTheGuardBranches() {
        YearMonth month = YearMonth.of(2026, 11);
        when(expenses.findByGroup(GROUP)).thenReturn(null);
        assertZero(summaries.getMonthlySummary(GROUP, month));
        Expense undated = expense("undated", null, 9999, Category.FOOD);
        Expense imported = expense("import", month.atDay(1), 123, null);
        Expense valid = expense("valid", month.atEndOfMonth(), 200, Category.FOOD);
        when(expenses.findByGroup(GROUP)).thenReturn(Arrays.asList(null, undated, imported, valid));
        MonthlySummary result = summaries.getMonthlySummary(GROUP, month);
        assertEquals(2, result.getExpenseCount());
        assertEquals(323, result.getTotalCents());
        assertEquals(Collections.singletonMap(Category.FOOD, 200L), result.getCategoryTotals());
        // Legacy missing-category records count toward spending but have no category bucket.
        assertZero(summaries.getMonthlySummary(GROUP, null));
    }

    @Test public void summary_leapFebruary_matchesTheYearAndKeepsExactCents() {
        when(expenses.findByGroup(GROUP)).thenReturn(Arrays.asList(
                expense("wrong-year-before", LocalDate.of(2023, 2, 28), 90000, Category.FOOD),
                expense("first", LocalDate.of(2024, 2, 1), 1, Category.FOOD),
                expense("leap-day", LocalDate.of(2024, 2, 29), 2, Category.FOOD),
                expense("other-category", LocalDate.of(2024, 2, 29), 4, Category.OTHER),
                expense("march", LocalDate.of(2024, 3, 1), 80000, Category.OTHER),
                expense("wrong-year-after", LocalDate.of(2025, 2, 1), 70000, Category.FOOD)));
        MonthlySummary result = summaries.getMonthlySummary(GROUP, YearMonth.of(2024, 2));
        assertEquals(3, result.getExpenseCount());
        assertEquals(7, result.getTotalCents());
        assertEquals(Long.valueOf(3), result.getCategoryTotals().get(Category.FOOD));
        assertEquals(Long.valueOf(4), result.getCategoryTotals().get(Category.OTHER));
        assertEquals(7L, result.getCategoryTotals().values().stream().mapToLong(Long::longValue).sum());
    }

    private static void assertZero(MonthlySummary result) {
        assertEquals(0, result.getExpenseCount());
        assertEquals(0, result.getTotalCents());
        assertTrue(result.getCategoryTotals().isEmpty());
    }

    private static Expense expense(String id, LocalDate date, long cents, Category category) {
        return new Expense(id, GROUP, "somya", cents, date, "", "Expense " + id,
                category, "somya", SplitMethod.EQUAL, Collections.emptyList());
    }
}
