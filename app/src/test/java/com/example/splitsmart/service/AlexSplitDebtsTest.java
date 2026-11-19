package com.example.splitsmart.service;

import com.example.splitsmart.exception.ValidationException;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
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
    private static final List<String> THREE = Arrays.asList("alice", "bob", "carol");

    private final SplitCalculator calculator = new SplitCalculator();

    @Test
    public void alex_percentage_twentyThirtyFiftyOfThirtyDollars() {
        List<Share> shares = calculator.calculate(3000, SplitMethod.PERCENTAGE, THREE,
                inputs(THREE, 20.0, 30.0, 50.0));

        assertCents(shares, 600, 900, 1500);
    }

    @Test
    public void alex_percentage_zeroPercentShare_isAccepted() {
        List<Share> shares = calculator.calculate(3000, SplitMethod.PERCENTAGE, THREE,
                inputs(THREE, 0.0, 50.0, 50.0));

        assertCents(shares, 0, 1500, 1500);
    }

    @Test
    public void alex_percentage_leftoverCentGoesToFirstEvenWhenLastHasTheLargestPercent() {
        // Each share floors to 333, so one cent is left over and goes to the first participant.
        List<Share> shares = calculator.calculate(1000, SplitMethod.PERCENTAGE, THREE,
                inputs(THREE, 33.33, 33.33, 33.34));

        assertCents(shares, 334, 333, 333);
        assertTrue(calculator.validateShares(1000, shares));
    }

    @Test
    public void alex_exact_oneCentOverTheTotal_isRejectedWithTheSumMessage() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> calculator.calculate(3000, SplitMethod.EXACT_AMOUNT, THREE,
                        inputs(THREE, 500.0, 1000.0, 1501.0)));

        assertEquals("Amounts must add up to the total.", error.getMessage());
    }

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

    private static Map<String, Double> inputs(List<String> participants, Double... values) {
        Map<String, Double> inputs = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            inputs.put(participants.get(i), values[i]);
        }
        return inputs;
    }

    private static void assertCents(List<Share> shares, long... expected) {
        assertEquals(expected.length, shares.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], shares.get(i).getShareCents());
        }
    }
}
