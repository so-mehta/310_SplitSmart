package com.example.splitsmart.service;

import com.example.splitsmart.model.Category;
import com.example.splitsmart.model.Expense;
import com.example.splitsmart.repository.ExpenseRepository;

import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Computes expense totals by month and category.
 * Repository-backed methods must not run on the main thread.
 */
public class SummaryService {

    private final ExpenseRepository expenseRepository;

    public SummaryService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    /**
     * Gets the expense summary for a group and calendar month.
     *
     * @param groupId the group to summarize
     * @param yearMonth the month to summarize
     * @return the monthly expense summary
     */
    public MonthlySummary getMonthlySummary(String groupId, YearMonth yearMonth) {
        int expenseCount = 0;
        long totalCents = 0;
        Map<Category, Long> categoryTotals = new EnumMap<>(Category.class);
        List<Expense> expenses = expenseRepository.findByGroup(groupId);

        if (expenses != null) {
            for (Expense expense : expenses) {
                if (expense == null || expense.getDate() == null || yearMonth == null
                        || !YearMonth.from(expense.getDate()).equals(yearMonth)) {
                    continue;
                }

                expenseCount++;
                totalCents += expense.getAmountCents();
                if (expense.getCategory() != null) {
                    categoryTotals.merge(expense.getCategory(), expense.getAmountCents(), Long::sum);
                }
            }
        }

        return new MonthlySummary(expenseCount, totalCents, categoryTotals);
    }
}
