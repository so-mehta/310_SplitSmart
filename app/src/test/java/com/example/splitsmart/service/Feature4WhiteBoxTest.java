package com.example.splitsmart.service;

import com.example.splitsmart.model.Category;
import com.example.splitsmart.model.Expense;
import com.example.splitsmart.model.Settlement;
import com.example.splitsmart.model.Share;
import com.example.splitsmart.model.SplitMethod;
import com.example.splitsmart.repository.ExpenseRepository;
import com.example.splitsmart.repository.SettlementRepository;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Feature 4 (Balance Calculation and Display) graded white-box tests (Christine).
 */
public class Feature4WhiteBoxTest {

    private static final String GROUP_ID = "group-1";
    private static final String ALICE = "alice";
    private static final String BOB = "bob";
    private static final String CAROL = "carol";
    private static final LocalDate DATE = LocalDate.of(2026, 1, 15);

    private ExpenseRepository expenseRepository;
    private SettlementRepository settlementRepository;
    private BalanceService balanceService;
    private List<Expense> expenses;
    private List<Settlement> settlements;

    @Before
    public void setUp() {
        expenseRepository = mock(ExpenseRepository.class);
        settlementRepository = mock(SettlementRepository.class);
        balanceService = new BalanceService(expenseRepository, settlementRepository);
        expenses = new ArrayList<>();
        settlements = new ArrayList<>();

        when(expenseRepository.findByGroup(GROUP_ID)).thenReturn(expenses);
        when(settlementRepository.findByGroup(GROUP_ID)).thenReturn(settlements);
    }

    @Test
    public void christine_samePairExpensesAccumulateAndOffset() {
        expenses.add(expense("dinner", BOB, share(BOB, 1000), share(ALICE, 1000)));
        expenses.add(expense("parking", BOB, share(BOB, 500), share(ALICE, 500)));
        expenses.add(expense("coffee", ALICE, share(ALICE, 400), share(BOB, 400)));

        assertEquals(-1100, balanceService.getIndividualBalance(GROUP_ID, ALICE, BOB));
        assertEquals(1100, balanceService.getIndividualBalance(GROUP_ID, BOB, ALICE));
    }

    @Test
    public void christine_chainOfDebtsDoesNotCreateThirdDebt() {
        expenses.add(expense("dinner", BOB, share(BOB, 1000), share(ALICE, 1000)));
        expenses.add(expense("ride", CAROL, share(CAROL, 1000), share(BOB, 1000)));

        BalanceSummary summary = balanceService.getBalanceSummary(GROUP_ID, BOB);
        assertEquals(1000, summary.getTotalToPayCents());
        assertEquals(1000, summary.getTotalToReceiveCents());
        assertEquals(0, balanceService.getIndividualBalance(GROUP_ID, ALICE, CAROL));
    }

    @Test
    public void christine_settlementEditAndDeletionRecomputeBalance() {
        expenses.add(expense("dinner", BOB, share(BOB, 1100), share(ALICE, 1100)));
        settlements.add(settlement("payment", ALICE, BOB, 500));
        assertEquals(-600, balanceService.getIndividualBalance(GROUP_ID, ALICE, BOB));

        settlements.set(0, settlement("payment", ALICE, BOB, 800));
        assertEquals(-300, balanceService.getIndividualBalance(GROUP_ID, ALICE, BOB));

        settlements.clear();
        assertEquals(-1100, balanceService.getIndividualBalance(GROUP_ID, ALICE, BOB));
    }

    @Test
    public void christine_editAfterFullSettlementReopensDebt() {
        expenses.add(expense("dinner", BOB, share(BOB, 1000), share(ALICE, 1000)));
        settlements.add(settlement("payment", ALICE, BOB, 1000));
        assertEquals(0, balanceService.getIndividualBalance(GROUP_ID, ALICE, BOB));

        expenses.set(0, expense("dinner", BOB, share(BOB, 1200), share(ALICE, 1200)));
        assertEquals(-200, balanceService.getIndividualBalance(GROUP_ID, ALICE, BOB));
    }

    @Test
    public void christine_deletingExpenseAfterPaymentReversesBalance() {
        expenses.add(expense("dinner", BOB, share(BOB, 1000), share(ALICE, 1000)));
        settlements.add(settlement("payment", ALICE, BOB, 1000));
        assertEquals(0, balanceService.getIndividualBalance(GROUP_ID, ALICE, BOB));

        expenses.clear();
        BalanceSummary summary = balanceService.getBalanceSummary(GROUP_ID, ALICE);
        assertEquals(0, summary.getTotalToPayCents());
        assertEquals(1000, summary.getTotalToReceiveCents());
        assertEquals(1000, balanceService.getIndividualBalance(GROUP_ID, ALICE, BOB));
    }

    private static Expense expense(String id, String payerId, Share... shares) {
        long amountCents = 0;
        for (Share share : shares) {
            amountCents += share.getShareCents();
        }
        return new Expense(id, GROUP_ID, payerId, amountCents, DATE, "", "Expense",
                Category.OTHER, payerId, SplitMethod.EXACT_AMOUNT, Arrays.asList(shares));
    }

    private static Settlement settlement(String id, String payerId, String receiverId,
                                         long amountCents) {
        return new Settlement(id, GROUP_ID, payerId, amountCents, DATE, "", payerId, receiverId);
    }

    private static Share share(String userId, long shareCents) {
        return new Share(userId, shareCents, 0);
    }
}
