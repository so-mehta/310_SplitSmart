package com.example.splitsmart.service;

import com.example.splitsmart.model.Category;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Expense count and cent totals for a group in one calendar month.
 */
public class MonthlySummary {

    private final int expenseCount;
    private final long totalCents;
    private final Map<Category, Long> categoryTotals;

    public MonthlySummary(int expenseCount, long totalCents,
                          Map<Category, Long> categoryTotals) {
        this.expenseCount = expenseCount;
        this.totalCents = totalCents;
        this.categoryTotals = immutableCopy(categoryTotals);
    }

    public int getExpenseCount() {
        return expenseCount;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public Map<Category, Long> getCategoryTotals() {
        return categoryTotals;
    }

    private static Map<Category, Long> immutableCopy(Map<Category, Long> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }
        EnumMap<Category, Long> copy = new EnumMap<>(Category.class);
        copy.putAll(values);
        return Collections.unmodifiableMap(copy);
    }
}
