package com.example.splitsmart.service;

import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.Share;
import com.example.splitsmart.model.SplitMethod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Turns an expense total into one share per participant. Pure logic, safe to call
 * on the main thread.
 */
public class SplitCalculator {

    private static final int MIN_PARTICIPANTS = 2;
    private static final long HUNDREDTHS_IN_100_PERCENT = 10000;

    private static final String ERROR_AMOUNT = "Amount must be greater than zero.";
    private static final String ERROR_METHOD = "Choose a split method.";
    private static final String ERROR_PARTICIPANTS = "Select at least two participants.";
    private static final String ERROR_DUPLICATE_PARTICIPANT = "Each participant can only be chosen once.";
    private static final String ERROR_EXACT_INPUT = "Enter an amount for each participant.";
    private static final String ERROR_EXACT_SUM = "Amounts must add up to the total.";
    private static final String ERROR_PERCENT_INPUT = "Enter a percentage from 0 to 100 for each participant.";
    private static final String ERROR_PERCENT_SUM = "Percentages must add up to 100.";

    /**
     * Calculates each participant's share of a total.
     *
     * @param totalCents the expense total in cents
     * @param method how the total is split
     * @param participantIds the participants, in display order
     * @param inputs participant ID to input: ignored for EQUAL, cents for EXACT_AMOUNT,
     *               percent for PERCENTAGE
     * @return one share per participant, in the same order
     * @throws ValidationException if the total is not positive, the method is missing, there
     *                             are fewer than 2 distinct participants, an input is missing
     *                             or invalid, or the inputs do not add up to the total
     */
    public List<Share> calculate(long totalCents, SplitMethod method,
                                 List<String> participantIds, Map<String, Double> inputs) {
        if (totalCents <= 0) {
            throw new ValidationException(ERROR_AMOUNT);
        }
        if (method == null) {
            throw new ValidationException(ERROR_METHOD);
        }
        if (participantIds == null || participantIds.size() < MIN_PARTICIPANTS) {
            throw new ValidationException(ERROR_PARTICIPANTS);
        }
        if (new HashSet<>(participantIds).size() != participantIds.size()) {
            throw new ValidationException(ERROR_DUPLICATE_PARTICIPANT);
        }

        switch (method) {
            case EQUAL:
                return splitEqually(totalCents, participantIds);
            case EXACT_AMOUNT:
                return splitByExactAmount(totalCents, participantIds, inputs);
            case PERCENTAGE:
                return splitByPercentage(totalCents, participantIds, inputs);
            default:
                throw new ValidationException(ERROR_METHOD);
        }
    }

    /**
     * Checks that a list of shares adds up to a total.
     *
     * @param totalCents the expense total in cents
     * @param shares the shares to check
     * @return true only if there is at least one share, none is negative, and they sum to
     *         the total
     */
    public boolean validateShares(long totalCents, List<Share> shares) {
        if (shares == null || shares.isEmpty()) {
            return false;
        }
        long sum = 0;
        for (Share share : shares) {
            if (share.getShareCents() < 0) {
                return false;
            }
            sum += share.getShareCents();
        }
        return sum == totalCents;
    }

    private static List<Share> splitEqually(long totalCents, List<String> participantIds) {
        long eachCents = totalCents / participantIds.size();
        List<Share> shares = new ArrayList<>();
        for (String participantId : participantIds) {
            shares.add(new Share(participantId, eachCents, 0));
        }
        giveLeftoverToFirst(totalCents, shares);
        return shares;
    }

    private static List<Share> splitByExactAmount(long totalCents, List<String> participantIds,
                                                  Map<String, Double> inputs) {
        List<Share> shares = new ArrayList<>();
        long sum = 0;
        for (String participantId : participantIds) {
            Double cents = inputs == null ? null : inputs.get(participantId);
            // The range check also rejects NaN and infinity.
            if (cents == null || !(cents >= 0 && cents <= totalCents) || cents != Math.rint(cents)) {
                throw new ValidationException(ERROR_EXACT_INPUT);
            }
            shares.add(new Share(participantId, cents.longValue(), 0));
            sum += cents.longValue();
        }
        if (sum != totalCents) {
            throw new ValidationException(ERROR_EXACT_SUM);
        }
        return shares;
    }

    /**
     * Works in whole hundredths of a percent, because doubles can land just under a whole
     * number of cents: 10000 * 0.57 / 100 floors to 56, not 57.
     */
    private static List<Share> splitByPercentage(long totalCents, List<String> participantIds,
                                                 Map<String, Double> inputs) {
        List<Share> shares = new ArrayList<>();
        long hundredthsSum = 0;
        for (String participantId : participantIds) {
            Double percent = inputs == null ? null : inputs.get(participantId);
            // The range check also rejects NaN and infinity.
            if (percent == null || !(percent >= 0 && percent <= 100)) {
                throw new ValidationException(ERROR_PERCENT_INPUT);
            }
            long hundredths = Math.round(percent * 100);
            hundredthsSum += hundredths;
            shares.add(new Share(participantId, percentOf(totalCents, hundredths), percent));
        }
        if (hundredthsSum != HUNDREDTHS_IN_100_PERCENT) {
            throw new ValidationException(ERROR_PERCENT_SUM);
        }
        giveLeftoverToFirst(totalCents, shares);
        return shares;
    }

    /**
     * Floor of total * hundredths / 10000, divided first so a large total cannot overflow.
     */
    private static long percentOf(long totalCents, long hundredths) {
        return totalCents / HUNDREDTHS_IN_100_PERCENT * hundredths
                + totalCents % HUNDREDTHS_IN_100_PERCENT * hundredths / HUNDREDTHS_IN_100_PERCENT;
    }

    private static void giveLeftoverToFirst(long totalCents, List<Share> shares) {
        long leftover = totalCents;
        for (Share share : shares) {
            leftover -= share.getShareCents();
        }
        Share first = shares.get(0);
        first.setShareCents(first.getShareCents() + leftover);
    }
}
