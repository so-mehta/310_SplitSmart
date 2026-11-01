package com.example.splitsmart.service;

import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.Share;
import com.example.splitsmart.model.SplitMethod;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class SplitCalculatorTest {

    private static final List<String> TWO = Arrays.asList("alice", "bob");
    private static final List<String> THREE = Arrays.asList("alice", "bob", "carol");
    private static final double PERCENT_DELTA = 0.0001;

    private final SplitCalculator calculator = new SplitCalculator();

    @Test
    public void equal_tenDollarsAmongThree_givesLeftoverCentToFirstParticipant() {
        List<Share> shares = calculator.calculate(1000, SplitMethod.EQUAL, THREE, null);

        assertCents(shares, 334, 333, 333);
    }

    @Test
    public void equal_oneCentBetweenTwo_givesItToFirstParticipant() {
        List<Share> shares = calculator.calculate(1, SplitMethod.EQUAL, TWO, null);

        assertCents(shares, 1, 0);
    }

    @Test
    public void equal_evenTotal_hasNoLeftover() {
        List<Share> shares = calculator.calculate(1000, SplitMethod.EQUAL, TWO, null);

        assertCents(shares, 500, 500);
    }

    @Test
    public void shares_keepParticipantOrder() {
        List<Share> shares = calculator.calculate(1000, SplitMethod.EQUAL, THREE, null);

        assertEquals("alice", shares.get(0).getUserId());
        assertEquals("bob", shares.get(1).getUserId());
        assertEquals("carol", shares.get(2).getUserId());
    }

    @Test
    public void equal_ignoresInputs() {
        List<Share> shares = calculator.calculate(1000, SplitMethod.EQUAL, TWO, inputs(TWO, 900.0, -5.0));

        assertCents(shares, 500, 500);
    }

    @Test
    public void exact_amountsThatSumToTotal_areUsedAsGiven() {
        List<Share> shares = calculator.calculate(1000, SplitMethod.EXACT_AMOUNT, THREE,
                inputs(THREE, 700.0, 200.0, 100.0));

        assertCents(shares, 700, 200, 100);
    }

    @Test
    public void exact_zeroForOneParticipant_isAccepted() {
        List<Share> shares = calculator.calculate(1000, SplitMethod.EXACT_AMOUNT, TWO,
                inputs(TWO, 1000.0, 0.0));

        assertCents(shares, 1000, 0);
    }

    @Test
    public void exact_amountsBelowTotal_areRejected() {
        assertRejected(1000, SplitMethod.EXACT_AMOUNT, TWO, inputs(TWO, 500.0, 499.0));
    }

    @Test
    public void exact_amountsAboveTotal_areRejected() {
        assertRejected(1000, SplitMethod.EXACT_AMOUNT, TWO, inputs(TWO, 500.0, 501.0));
    }

    @Test
    public void exact_negativeAmount_isRejectedEvenIfTheSumMatches() {
        assertRejected(1000, SplitMethod.EXACT_AMOUNT, TWO, inputs(TWO, -100.0, 1100.0));
    }

    @Test
    public void exact_fractionalCents_areRejected() {
        assertRejected(1000, SplitMethod.EXACT_AMOUNT, TWO, inputs(TWO, 500.5, 499.5));
    }

    @Test
    public void exact_missingInput_isRejected() {
        Map<String, Double> inputs = new HashMap<>();
        inputs.put("alice", 1000.0);

        assertRejected(1000, SplitMethod.EXACT_AMOUNT, TWO, inputs);
    }

    @Test
    public void exact_nullInputs_areRejected() {
        assertRejected(1000, SplitMethod.EXACT_AMOUNT, TWO, null);
    }

    @Test
    public void exact_nonFiniteInput_isRejected() {
        assertRejected(1000, SplitMethod.EXACT_AMOUNT, TWO, inputs(TWO, Double.NaN, 1000.0));
        assertRejected(1000, SplitMethod.EXACT_AMOUNT, TWO,
                inputs(TWO, Double.POSITIVE_INFINITY, 0.0));
    }

    @Test
    public void percentage_fiftyThirtyTwentyOfOneHundredDollars() {
        List<Share> shares = calculator.calculate(10000, SplitMethod.PERCENTAGE, THREE,
                inputs(THREE, 50.0, 30.0, 20.0));

        assertCents(shares, 5000, 3000, 2000);
    }

    @Test
    public void percentage_isStoredInEachShare() {
        List<Share> shares = calculator.calculate(10000, SplitMethod.PERCENTAGE, THREE,
                inputs(THREE, 50.0, 30.0, 20.0));

        assertEquals(50.0, shares.get(0).getPercent(), PERCENT_DELTA);
        assertEquals(30.0, shares.get(1).getPercent(), PERCENT_DELTA);
        assertEquals(20.0, shares.get(2).getPercent(), PERCENT_DELTA);
    }

    @Test
    public void percentage_givesLeftoverCentToFirstParticipant() {
        List<Share> shares = calculator.calculate(1000, SplitMethod.PERCENTAGE, THREE,
                inputs(THREE, 33.34, 33.33, 33.33));

        assertCents(shares, 334, 333, 333);
    }

    @Test
    public void percentage_isExactWhereDoubleArithmeticWouldLoseACent() {
        // 0.57 * 10000 / 100 is 56.99999... as a double, which floors to 56.
        List<Share> shares = calculator.calculate(10000, SplitMethod.PERCENTAGE, TWO,
                inputs(TWO, 99.43, 0.57));

        assertCents(shares, 9943, 57);
    }

    @Test
    public void percentage_sumMatchesInHundredthsDespiteDoubleError() {
        // These add up to 99.99999999999999 as doubles.
        List<Share> shares = calculator.calculate(10000, SplitMethod.PERCENTAGE, THREE,
                inputs(THREE, 31.58, 33.12, 35.30));

        assertCents(shares, 3158, 3312, 3530);
    }

    @Test
    public void percentage_thatSumsBelowOneHundred_isRejected() {
        assertRejected(10000, SplitMethod.PERCENTAGE, THREE, inputs(THREE, 33.33, 33.33, 33.33));
    }

    @Test
    public void percentage_thatSumsAboveOneHundred_isRejected() {
        assertRejected(10000, SplitMethod.PERCENTAGE, TWO, inputs(TWO, 60.0, 50.0));
    }

    @Test
    public void percentage_negativeValue_isRejectedEvenIfTheSumMatches() {
        assertRejected(10000, SplitMethod.PERCENTAGE, TWO, inputs(TWO, -10.0, 110.0));
    }

    @Test
    public void percentage_missingInput_isRejected() {
        Map<String, Double> inputs = new HashMap<>();
        inputs.put("alice", 100.0);

        assertRejected(10000, SplitMethod.PERCENTAGE, TWO, inputs);
    }

    @Test
    public void percentage_nonFiniteInput_isRejected() {
        assertRejected(10000, SplitMethod.PERCENTAGE, TWO, inputs(TWO, Double.NaN, 100.0));
    }

    @Test
    public void fewerThanTwoParticipants_isRejectedForEveryMethod() {
        List<String> one = Collections.singletonList("alice");
        Map<String, Double> inputs = new HashMap<>();
        inputs.put("alice", 1000.0);

        assertRejected(1000, SplitMethod.EQUAL, one, inputs);
        assertRejected(1000, SplitMethod.EXACT_AMOUNT, one, inputs);
        assertRejected(1000, SplitMethod.PERCENTAGE, one, inputs);
    }

    @Test
    public void noParticipants_isRejected() {
        assertRejected(1000, SplitMethod.EQUAL, Collections.emptyList(), null);
        assertRejected(1000, SplitMethod.EQUAL, null, null);
    }

    @Test
    public void duplicateParticipant_isRejected() {
        assertRejected(1000, SplitMethod.EQUAL, Arrays.asList("alice", "alice"), null);
    }

    @Test
    public void totalOfZeroOrLess_isRejected() {
        assertRejected(0, SplitMethod.EQUAL, TWO, null);
        assertRejected(-1000, SplitMethod.EQUAL, TWO, null);
    }

    @Test
    public void missingMethod_isRejected() {
        assertRejected(1000, null, TWO, null);
    }

    @Test
    public void calculatedShares_alwaysSumToTheTotal() {
        List<String> participants = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            participants.add("member" + i);
            if (participants.size() < 2) {
                continue;
            }
            for (long total = 1; total <= 2500; total += 7) {
                List<Share> shares = calculator.calculate(total, SplitMethod.EQUAL, participants, null);
                assertTrue(calculator.validateShares(total, shares));
            }
        }
    }

    @Test
    public void validateShares_trueWhenTheySumToTheTotal() {
        assertTrue(calculator.validateShares(1000, shares(334, 333, 333)));
    }

    @Test
    public void validateShares_falseWhenOffByOneCent() {
        assertFalse(calculator.validateShares(1000, shares(334, 333, 332)));
        assertFalse(calculator.validateShares(1000, shares(334, 333, 334)));
    }

    @Test
    public void validateShares_falseForNegativeShareEvenIfTheSumMatches() {
        assertFalse(calculator.validateShares(1000, shares(-100, 1100)));
    }

    @Test
    public void validateShares_trueForAZeroShare() {
        assertTrue(calculator.validateShares(1, shares(1, 0)));
    }

    @Test
    public void validateShares_falseForNullOrEmptyShares() {
        assertFalse(calculator.validateShares(1000, null));
        assertFalse(calculator.validateShares(1000, Collections.emptyList()));
    }

    private void assertRejected(long total, SplitMethod method, List<String> participants,
                                Map<String, Double> inputs) {
        assertThrows(ValidationException.class,
                () -> calculator.calculate(total, method, participants, inputs));
    }

    private static void assertCents(List<Share> shares, long... expected) {
        assertEquals(expected.length, shares.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], shares.get(i).getShareCents());
        }
    }

    private static Map<String, Double> inputs(List<String> participants, Double... values) {
        Map<String, Double> inputs = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            inputs.put(participants.get(i), values[i]);
        }
        return inputs;
    }

    private static List<Share> shares(long... cents) {
        List<Share> shares = new ArrayList<>();
        for (int i = 0; i < cents.length; i++) {
            shares.add(new Share("member" + i, cents[i], 0));
        }
        return shares;
    }
}
