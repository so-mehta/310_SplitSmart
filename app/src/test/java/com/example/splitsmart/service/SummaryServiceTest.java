package com.example.splitsmart.service;

import com.example.splitsmart.model.Category;
import com.example.splitsmart.model.Expense;
import com.example.splitsmart.repository.ExpenseRepository;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SummaryServiceTest {

    private static final String GROUP_ID = "group-1";

    private ExpenseRepository expenseRepository;
    private SummaryService summaryService;
    private List<Expense> expenses;

    @Before
    public void setUp() {
        expenseRepository = mock(ExpenseRepository.class);
        summaryService = new SummaryService(expenseRepository);
        expenses = new ArrayList<>(Arrays.asList(
                expense("december", LocalDate.of(2025, 12, 31), 900, Category.FOOD),
                expense("january-1", LocalDate.of(2026, 1, 1), 1200, Category.FOOD),
                expense("january-31", LocalDate.of(2026, 1, 31), 3000, Category.HOUSING),
                expense("february", LocalDate.of(2026, 2, 1), 700, Category.FOOD)));
        when(expenseRepository.findByGroup(GROUP_ID)).thenReturn(expenses);
    }

    @Test
    public void monthlySummary_includesBothMonthBoundariesAndTotalsByCategory() {
        MonthlySummary summary = summaryService.getMonthlySummary(
                GROUP_ID, YearMonth.of(2026, 1));

        assertEquals(2, summary.getExpenseCount());
        assertEquals(4200, summary.getTotalCents());
        assertEquals(Long.valueOf(1200), summary.getCategoryTotals().get(Category.FOOD));
        assertEquals(Long.valueOf(3000), summary.getCategoryTotals().get(Category.HOUSING));
        verify(expenseRepository).findByGroup(GROUP_ID);
    }

    @Test
    public void monthlySummary_combinesMultipleExpensesInTheSameCategory() {
        expenses.add(expense("january-food-2", LocalDate.of(2026, 1, 20),
                800, Category.FOOD));

        MonthlySummary summary = summaryService.getMonthlySummary(
                GROUP_ID, YearMonth.of(2026, 1));

        assertEquals(3, summary.getExpenseCount());
        assertEquals(5000, summary.getTotalCents());
        assertEquals(Long.valueOf(2000), summary.getCategoryTotals().get(Category.FOOD));
    }

    @Test
    public void emptyMonth_returnsZerosAndAnEmptyCategoryMap() {
        MonthlySummary summary = summaryService.getMonthlySummary(
                GROUP_ID, YearMonth.of(2026, 3));

        assertEquals(0, summary.getExpenseCount());
        assertEquals(0, summary.getTotalCents());
        assertEquals(0, summary.getCategoryTotals().size());
    }

    private static Expense expense(String id, LocalDate date, long amountCents,
                                   Category category) {
        Expense expense = new Expense();
        expense.setTransactionId(id);
        expense.setGroupId(GROUP_ID);
        expense.setDate(date);
        expense.setAmountCents(amountCents);
        expense.setCategory(category);
        return expense;
    }
}
