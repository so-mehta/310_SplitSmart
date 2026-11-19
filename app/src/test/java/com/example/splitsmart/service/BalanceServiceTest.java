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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BalanceServiceTest {

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
    public void example1_alicePaysThirty_bobAndCarolEachOweTen() {
        expenses.add(expense("expense-1", ALICE,
                share(ALICE, 1000), share(BOB, 1000), share(CAROL, 1000)));

        BalanceSummary summary = balanceService.getBalanceSummary(GROUP_ID, ALICE);

        assertEquals(0, summary.getTotalToPayCents());
        assertEquals(2000, summary.getTotalToReceiveCents());
        assertEquals(Long.valueOf(1000), summary.getIndividualBalances().get(BOB));
        assertEquals(Long.valueOf(1000), summary.getIndividualBalances().get(CAROL));
    }

    @Test
    public void example2_carolOwesAliceTenAndBobSix_withoutNettingAcrossPairs() {
        expenses.add(expense("expense-1", ALICE,
                share(ALICE, 1000), share(BOB, 1000), share(CAROL, 1000)));
        expenses.add(expense("expense-2", BOB,
                share(BOB, 600), share(CAROL, 600)));

        BalanceSummary summary = balanceService.getBalanceSummary(GROUP_ID, CAROL);

        assertEquals(1600, summary.getTotalToPayCents());
        assertEquals(0, summary.getTotalToReceiveCents());
        assertEquals(Long.valueOf(-1000), summary.getIndividualBalances().get(ALICE));
        assertEquals(Long.valueOf(-600), summary.getIndividualBalances().get(BOB));
    }

    @Test
    public void example3_bobSettlesFiveWithAlice_bobStillOwesFive() {
        expenses.add(expense("expense-1", ALICE,
                share(ALICE, 1000), share(BOB, 1000), share(CAROL, 1000)));
        settlements.add(settlement("settlement-1", BOB, ALICE, 500));

        assertEquals(500, balanceService.getIndividualBalance(GROUP_ID, ALICE, BOB));
        assertEquals(-500, balanceService.getIndividualBalance(GROUP_ID, BOB, ALICE));
    }

    @Test
    public void unevenSplit_usesEachStoredShareInCents() {
        expenses.add(expense("expense-1", ALICE,
                share(ALICE, 334), share(BOB, 333), share(CAROL, 333)));

        BalanceSummary summary = balanceService.getBalanceSummary(GROUP_ID, ALICE);

        assertEquals(666, summary.getTotalToReceiveCents());
        assertEquals(Long.valueOf(333), summary.getIndividualBalances().get(BOB));
        assertEquals(Long.valueOf(333), summary.getIndividualBalances().get(CAROL));
    }

    @Test
    public void percentageSplit_usesTheCalculatedCentShares() {
        Expense percentageExpense = expense("expense-1", ALICE,
                new Share(ALICE, 5000, 50),
                new Share(BOB, 3000, 30),
                new Share(CAROL, 2000, 20));
        percentageExpense.setSplitMethod(SplitMethod.PERCENTAGE);
        expenses.add(percentageExpense);

        BalanceSummary summary = balanceService.getBalanceSummary(GROUP_ID, ALICE);

        assertEquals(5000, summary.getTotalToReceiveCents());
        assertEquals(Long.valueOf(3000), summary.getIndividualBalances().get(BOB));
        assertEquals(Long.valueOf(2000), summary.getIndividualBalances().get(CAROL));
    }

    @Test
    public void fullyClearedDebt_keepsBothTotalsAtZero() {
        expenses.add(expense("expense-1", ALICE, share(ALICE, 1000), share(BOB, 1000)));
        settlements.add(settlement("settlement-1", BOB, ALICE, 1000));

        BalanceSummary summary = balanceService.getBalanceSummary(GROUP_ID, BOB);

        assertEquals(0, summary.getTotalToPayCents());
        assertEquals(0, summary.getTotalToReceiveCents());
        assertEquals(Long.valueOf(0), summary.getIndividualBalances().get(ALICE));
    }

    @Test
    public void settlementInOppositeDirection_increasesTheExistingDebt() {
        expenses.add(expense("expense-1", ALICE, share(ALICE, 1000), share(BOB, 1000)));
        settlements.add(settlement("settlement-1", ALICE, BOB, 200));

        assertEquals(1200, balanceService.getIndividualBalance(GROUP_ID, ALICE, BOB));
    }

    @Test
    public void payerWhoIsNotAParticipant_isStillOwedEveryParticipantShare() {
        expenses.add(expense("expense-1", ALICE, share(BOB, 1000), share(CAROL, 2000)));

        BalanceSummary summary = balanceService.getBalanceSummary(GROUP_ID, ALICE);

        assertEquals(3000, summary.getTotalToReceiveCents());
        assertEquals(Long.valueOf(1000), summary.getIndividualBalances().get(BOB));
        assertEquals(Long.valueOf(2000), summary.getIndividualBalances().get(CAROL));
    }

    @Test
    public void amountsToPayAndReceive_areNotNettedAcrossDifferentPeople() {
        expenses.add(expense("expense-1", ALICE, share(ALICE, 1000), share(CAROL, 1000)));
        expenses.add(expense("expense-2", BOB, share(BOB, 500), share(ALICE, 500)));

        BalanceSummary summary = balanceService.getBalanceSummary(GROUP_ID, ALICE);

        assertEquals(500, summary.getTotalToPayCents());
        assertEquals(1000, summary.getTotalToReceiveCents());
        assertEquals(Long.valueOf(-500), summary.getIndividualBalances().get(BOB));
        assertEquals(Long.valueOf(1000), summary.getIndividualBalances().get(CAROL));
    }

    @Test
    public void balanceSummary_readsEachRepositoryOnce() {
        balanceService.getBalanceSummary(GROUP_ID, ALICE);

        verify(expenseRepository).findByGroup(GROUP_ID);
        verify(settlementRepository).findByGroup(GROUP_ID);
    }

    @Test
    public void noTransactions_returnsZeroTotalsAndNoIndividualBalances() {
        BalanceSummary summary = balanceService.getBalanceSummary(GROUP_ID, ALICE);

        assertEquals(0, summary.getTotalToPayCents());
        assertEquals(0, summary.getTotalToReceiveCents());
        assertEquals(0, summary.getIndividualBalances().size());
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
