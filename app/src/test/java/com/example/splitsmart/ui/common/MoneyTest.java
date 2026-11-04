package com.example.splitsmart.ui.common;

import com.example.splitsmart.exception.ValidationException;
import org.junit.Test;
import static org.junit.Assert.*;

public class MoneyTest {
    @Test public void acceptsWholeDollars() { assertEquals(1200L, AmountParser.parse("12")); }
    @Test public void acceptsOneDecimal() { assertEquals(1050L, AmountParser.parse("10.5")); }
    @Test public void acceptsTwoDecimals() { assertEquals(1050L, AmountParser.parse("10.50")); }
    @Test public void acceptsCurrencySymbolAndSurroundingSpace() { assertEquals(1050L, AmountParser.parse("  $10.50  ")); }
    @Test public void acceptsOneCent() { assertEquals(1L, AmountParser.parse("0.01")); }
    @Test public void supportsLongBoundaryExactly() { assertEquals(Long.MAX_VALUE, AmountParser.parse("92233720368547758.07")); }
    @Test public void rejectsTooManyDecimalsWithoutRounding() { reject("0.005"); reject("1.999"); reject("1.000"); }
    @Test public void rejectsZeroTotal() { reject("0"); reject("0.00"); reject("$0.00"); }
    @Test public void rejectsEmpty() { reject(""); reject(" "); reject(null); }
    @Test public void rejectsNegativeAndSigns() { reject("-1"); reject("+1"); reject("$-1.00"); }
    @Test public void rejectsMalformedValues() { for (String value : new String[]{"1,000", "1e2", "NaN", "Infinity", ".50", "10.", "$ 10", "1 2", "$$10"}) reject(value); }
    @Test public void rejectsOverflow() { reject("92233720368547758.08"); reject("999999999999999999999999"); }
    @Test public void supportsZeroParticipantShare() { assertEquals(0L, AmountParser.parseNonNegative("$0.00")); }
    @Test public void formatsCents() { assertEquals("$10.50", MoneyFormatter.format(1050)); assertEquals("$0.05", MoneyFormatter.format(5)); assertEquals("$0.00", MoneyFormatter.format(0)); }
    @Test public void formatsNegativeBalance() { assertEquals("-$10.50", MoneyFormatter.format(-1050)); }
    @Test public void formatsLongWithoutOverflowOrPrecisionLoss() { assertEquals("-$92,233,720,368,547,758.08", MoneyFormatter.format(Long.MIN_VALUE)); }
    @Test public void editableValuesRoundTripWithoutDouble() {
        for (long value : new long[]{1, 5, 1050, 9007199254740993L, Long.MAX_VALUE})
            assertEquals(value, AmountParser.parse(MoneyFormatter.editable(value)));
    }
    private static void reject(String value) {
        try { AmountParser.parse(value); fail("Accepted invalid amount: " + value); }
        catch (ValidationException expected) { assertNotNull(expected.getMessage()); }
    }
}
