package com.example.splitsmart.service;

import com.example.splitsmart.model.Expense;
import com.example.splitsmart.model.Settlement;
import com.example.splitsmart.model.Share;
import com.example.splitsmart.repository.ExpenseRepository;
import com.example.splitsmart.repository.SettlementRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Computes pairwise balances and balance summaries for a group (SD5).
 * Repository-backed methods must not run on the main thread.
 */
public class BalanceService {

    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;

    public BalanceService(ExpenseRepository expenseRepository,
                          SettlementRepository settlementRepository) {
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
    }

    /**
     * Gets the user's total amounts to pay and receive, plus their balance with each member.
     *
     * @param groupId the group to summarize
     * @param userId the user whose balances are requested
     * @return the user's balance summary
     */
    public BalanceSummary getBalanceSummary(String groupId, String userId) {
        Map<String, Long> individualBalances = calculateBalances(groupId, userId);
        long totalToPayCents = 0;
        long totalToReceiveCents = 0;

        for (long balance : individualBalances.values()) {
            if (balance > 0) {
                totalToReceiveCents += balance;
            } else if (balance < 0) {
                totalToPayCents += -balance;
            }
        }

        return new BalanceSummary(totalToPayCents, totalToReceiveCents, individualBalances);
    }

    /**
     * Gets the balance between two users. A positive value means userB owes userA; a negative
     * value means userA owes userB.
     *
     * @param groupId the group containing the transactions
     * @param userA the first user
     * @param userB the second user
     * @return the pairwise balance in cents
     */
    public long getIndividualBalance(String groupId, String userA, String userB) {
        return calculateBalances(groupId, userA).getOrDefault(userB, 0L);
    }

    private Map<String, Long> calculateBalances(String groupId, String userId) {
        Map<String, Long> balances = new LinkedHashMap<>();
        List<Expense> expenses = expenseRepository.findByGroup(groupId);
        List<Settlement> settlements = settlementRepository.findByGroup(groupId);

        if (expenses != null) {
            for (Expense expense : expenses) {
                addExpenseBalances(balances, userId, expense);
            }
        }
        if (settlements != null) {
            for (Settlement settlement : settlements) {
                addSettlementBalance(balances, userId, settlement);
            }
        }
        return balances;
    }

    private static void addExpenseBalances(Map<String, Long> balances, String userId,
                                           Expense expense) {
        if (expense == null || expense.getPayerId() == null || expense.getShares() == null) {
            return;
        }

        String payerId = expense.getPayerId();
        for (Share share : expense.getShares()) {
            if (share == null || share.getUserId() == null
                    || Objects.equals(payerId, share.getUserId())) {
                continue;
            }

            if (Objects.equals(userId, payerId)) {
                addBalance(balances, share.getUserId(), share.getShareCents());
            } else if (Objects.equals(userId, share.getUserId())) {
                addBalance(balances, payerId, -share.getShareCents());
            }
        }
    }

    private static void addSettlementBalance(Map<String, Long> balances, String userId,
                                             Settlement settlement) {
        if (settlement == null || settlement.getPayerId() == null
                || settlement.getReceiverId() == null
                || Objects.equals(settlement.getPayerId(), settlement.getReceiverId())) {
            return;
        }

        if (Objects.equals(userId, settlement.getReceiverId())) {
            addBalance(balances, settlement.getPayerId(), -settlement.getAmountCents());
        } else if (Objects.equals(userId, settlement.getPayerId())) {
            addBalance(balances, settlement.getReceiverId(), settlement.getAmountCents());
        }
    }

    private static void addBalance(Map<String, Long> balances, String otherUserId,
                                   long changeCents) {
        balances.merge(otherUserId, changeCents, Long::sum);
    }
}
