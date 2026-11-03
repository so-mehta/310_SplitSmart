package com.example.splitsmart.ui.common;

import com.example.splitsmart.exception.ValidationException;
import java.math.BigDecimal;

/** Parses USD input exactly; never rounds or converts money through double. */
public final class AmountParser {
    private AmountParser() {}

    public static long parse(String input) {
        long cents = parseNonNegative(input);
        if (cents == 0) throw new ValidationException("Amount must be greater than zero.");
        return cents;
    }

    /** A participant can owe zero cents, even though the expense total must be positive. */
    public static long parseNonNegative(String input) {
        String value = input == null ? "" : input.trim();
        if (value.startsWith("$")) value = value.substring(1);
        if (!value.matches("[0-9]+(\\.[0-9]{1,2})?")) {
            throw new ValidationException("Enter a non-negative amount with at most two decimal places.");
        }
        try {
            return new BigDecimal(value).movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new ValidationException("Amount is too large.");
        }
    }
}
