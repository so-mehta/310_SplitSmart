package com.example.splitsmart.service;

import com.example.splitsmart.model.Expense;
import com.example.splitsmart.model.Settlement;
import com.example.splitsmart.model.SortOrder;
import com.example.splitsmart.model.Transaction;
import com.example.splitsmart.repository.ExpenseRepository;
import com.example.splitsmart.repository.SettlementRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Merges, filters and sorts a group's expenses and settlements (SD7).
 * Repository-backed methods must not run on the main thread.
 */
public class HistoryService {

    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;

    public HistoryService(ExpenseRepository expenseRepository,
                          SettlementRepository settlementRepository) {
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
    }

    /**
     * Gets a group's transaction history using the requested filters and ordering.
     *
     * @param groupId the group whose history is requested
     * @param filter the filters to apply
     * @param order the requested date order
     * @return matching transactions in the requested order
     */
    public List<Transaction> getHistory(String groupId, HistoryFilter filter, SortOrder order) {
        List<Transaction> history = new ArrayList<>();
        List<Expense> expenses = expenseRepository.findByGroup(groupId);
        List<Settlement> settlements = settlementRepository.findByGroup(groupId);
        if (expenses != null) {
            history.addAll(expenses);
        }
        if (settlements != null) {
            history.addAll(settlements);
        }

        history.removeIf(transaction -> !matches(transaction, filter));
        history.sort(historyComparator(order));
        return history;
    }

    private static boolean matches(Transaction transaction, HistoryFilter filter) {
        if (transaction == null) {
            return false;
        }
        if (filter == null) {
            return true;
        }
        if (filter.getType() != null && transaction.getType() != filter.getType()) {
            return false;
        }
        if (filter.getCategory() != null
                && (!(transaction instanceof Expense)
                || ((Expense) transaction).getCategory() != filter.getCategory())) {
            return false;
        }
        if (!isBlank(filter.getMemberId()) && !transaction.involves(filter.getMemberId())) {
            return false;
        }
        return matchesKeyword(transaction, filter.getKeyword());
    }

    private static boolean matchesKeyword(Transaction transaction, String keyword) {
        if (isBlank(keyword)) {
            return true;
        }

        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        if (contains(transaction.getNote(), normalizedKeyword)) {
            return true;
        }
        return transaction instanceof Expense
                && contains(((Expense) transaction).getTitle(), normalizedKeyword);
    }

    private static boolean contains(String value, String normalizedKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private static Comparator<Transaction> historyComparator(SortOrder order) {
        Comparator<Transaction> byDate;
        if (order == SortOrder.OLDEST_FIRST) {
            byDate = Comparator.comparing(Transaction::getDate,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        } else {
            byDate = Comparator.comparing(Transaction::getDate,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        }
        Comparator<Transaction> byId = Comparator.comparing(Transaction::getTransactionId,
                Comparator.nullsLast(Comparator.naturalOrder()));
        return byDate.thenComparing(byId);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
