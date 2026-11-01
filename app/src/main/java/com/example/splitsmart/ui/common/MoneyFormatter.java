package com.example.splitsmart.ui.common;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyFormatter {
    private MoneyFormatter() {}

    public static String format(long cents) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(BigDecimal.valueOf(cents, 2));
    }

    /** Plain editable dollars, without currency symbols or grouping separators. */
    public static String editable(long cents) {
        return BigDecimal.valueOf(cents, 2).toPlainString();
    }
}
