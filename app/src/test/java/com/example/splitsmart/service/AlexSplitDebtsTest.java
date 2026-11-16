package com.example.splitsmart.service;

import com.example.splitsmart.model.Category;
import com.example.splitsmart.model.Expense;
import com.example.splitsmart.model.Share;
import com.example.splitsmart.model.SplitMethod;
import com.example.splitsmart.repository.ExpenseRepository;
import com.example.splitsmart.repository.GroupRepository;
import com.example.splitsmart.repository.SettlementRepository;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Feature 3 "assign debts correctly": the shares ExpenseService previews are the shares
 * BalanceService turns into debts.
 */
public class AlexSplitDebtsTest {

    private static final String GROUP_ID = "group-1";
    private static final String ALEX = "alex";
    private static final String SIMRA = "simra";
    private static final String CHRISTINE = "christine";

    @Test
    public void alex_previewedEqualSplit_makesEachOtherParticipantOweOnlyThePayer() {
        ExpenseService expenseService = new ExpenseService(new SplitCalculator(),
                mock(ExpenseRepository.class), mock(GroupRepository.class));
        Map<String, Double> inputs = new LinkedHashMap<>();
        inputs.put(ALEX, 0.0);
        inputs.put(SIMRA, 0.0);
        inputs.put(CHRISTINE, 0.0);
        List<Share> shares = expenseService.previewShares(3000, SplitMethod.EQUAL, inputs);

        List<Expense> expenses = new ArrayList<>();
        expenses.add(new Expense("pizza", GROUP_ID, ALEX, 3000, LocalDate.of(2026, 11, 16), "",
                "Pizza", Category.FOOD, ALEX, SplitMethod.EQUAL, shares));
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        SettlementRepository settlementRepository = mock(SettlementRepository.class);
        when(expenseRepository.findByGroup(GROUP_ID)).thenReturn(expenses);
        when(settlementRepository.findByGroup(GROUP_ID)).thenReturn(Collections.emptyList());
        BalanceService balanceService = new BalanceService(expenseRepository, settlementRepository);

        BalanceSummary payer = balanceService.getBalanceSummary(GROUP_ID, ALEX);
        assertEquals(Long.valueOf(1000), payer.getIndividualBalances().get(SIMRA));
        assertEquals(Long.valueOf(1000), payer.getIndividualBalances().get(CHRISTINE));
        assertFalse(payer.getIndividualBalances().containsKey(ALEX));
        assertEquals(2000, payer.getTotalToReceiveCents());
        assertEquals(0, payer.getTotalToPayCents());

        BalanceSummary participant = balanceService.getBalanceSummary(GROUP_ID, SIMRA);
        assertEquals(Long.valueOf(-1000), participant.getIndividualBalances().get(ALEX));
        assertEquals(0L, participant.getIndividualBalances().getOrDefault(CHRISTINE, 0L).longValue());
        assertEquals(1000, participant.getTotalToPayCents());
        assertEquals(0, participant.getTotalToReceiveCents());
    }
}
