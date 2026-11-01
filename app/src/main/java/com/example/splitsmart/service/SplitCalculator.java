package com.example.splitsmart.service;

import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.Share;
import com.example.splitsmart.model.SplitMethod;

import java.util.List;
import java.util.Map;

/**
 * Turns an expense total into one share per participant. Pure logic, safe to call
 * on the main thread.
 */
public class SplitCalculator {

    /**
     * Calculates each participant's share of a total.
     *
     * @param totalCents the expense total in cents
     * @param method how the total is split
     * @param participantIds the participants, in display order
     * @param inputs participant ID to input: ignored for EQUAL, cents for EXACT_AMOUNT,
     *               percent for PERCENTAGE
     * @return one share per participant, in the same order
     * @throws ValidationException if there are fewer than 2 participants, or the inputs do
     *                             not add up to the total
     */
    public List<Share> calculate(long totalCents, SplitMethod method,
                                 List<String> participantIds, Map<String, Double> inputs) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Checks that a list of shares adds up to a total.
     *
     * @param totalCents the expense total in cents
     * @param shares the shares to check
     * @return true only if the shares sum to the total
     */
    public boolean validateShares(long totalCents, List<Share> shares) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
