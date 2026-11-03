package com.example.splitsmart.service;

import java.util.regex.Pattern;

/**
 * Format checks for user input. Pure logic, safe to call on the main thread.
 */
public class InputValidator {

    private static final Pattern USC_EMAIL =
            Pattern.compile("[^@\\s]+@usc\\.edu", Pattern.CASE_INSENSITIVE);
    private static final Pattern USC_ID = Pattern.compile("\\d{10}");

    /**
     * Checks that an email is a USC address.
     *
     * @param email the email to check
     * @return true if it ends with @usc.edu (any case) with text before the @
     */
    public boolean isValidUscEmail(String email) {
        return email != null && USC_EMAIL.matcher(email).matches();
    }

    /**
     * Checks that an ID is a USC ID.
     *
     * @param id the ID to check
     * @return true if it is exactly 10 digits
     */
    public boolean isValidUscId(String id) {
        return id != null && USC_ID.matcher(id).matches();
    }

    /**
     * Checks that an amount can be used for an expense or settlement.
     *
     * @param cents the amount in cents
     * @return true if it is greater than 0
     */
    public boolean isValidAmount(long cents) {
        return cents > 0;
    }
}
