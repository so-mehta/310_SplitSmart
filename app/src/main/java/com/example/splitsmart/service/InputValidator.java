package com.example.splitsmart.service;

/**
 * Format checks for user input. Pure logic, safe to call on the main thread.
 */
public class InputValidator {

    /**
     * Checks that an email is a USC address.
     *
     * @param email the email to check
     * @return true if it ends with @usc.edu (any case) with text before the @
     */
    public boolean isValidUscEmail(String email) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Checks that an ID is a USC ID.
     *
     * @param id the ID to check
     * @return true if it is exactly 10 digits
     */
    public boolean isValidUscId(String id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Checks that an amount can be used for an expense or settlement.
     *
     * @param cents the amount in cents
     * @return true if it is greater than 0
     */
    public boolean isValidAmount(long cents) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
