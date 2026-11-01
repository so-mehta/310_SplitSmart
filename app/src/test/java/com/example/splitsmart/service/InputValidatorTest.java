package com.example.splitsmart.service;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InputValidatorTest {

    private final InputValidator validator = new InputValidator();

    @Test
    public void uscEmail_isAccepted() {
        assertTrue(validator.isValidUscEmail("tommy@usc.edu"));
    }

    @Test
    public void uscEmail_anyCaseIsAccepted() {
        assertTrue(validator.isValidUscEmail("A@USC.EDU"));
    }

    @Test
    public void nonUscEmail_isRejected() {
        assertFalse(validator.isValidUscEmail("abc@gmail.com"));
    }

    @Test
    public void lookalikeUscDomains_areRejected() {
        assertFalse(validator.isValidUscEmail("abc@notusc.edu"));
        assertFalse(validator.isValidUscEmail("abc@usc.edu.com"));
        assertFalse(validator.isValidUscEmail("abc@mail.usc.edu"));
    }

    @Test
    public void email_withNoTextBeforeTheAt_isRejected() {
        assertFalse(validator.isValidUscEmail("@usc.edu"));
        assertFalse(validator.isValidUscEmail("   @usc.edu"));
    }

    @Test
    public void email_withTwoAts_isRejected() {
        assertFalse(validator.isValidUscEmail("a@b@usc.edu"));
    }

    @Test
    public void email_withWhitespace_isRejected() {
        assertFalse(validator.isValidUscEmail("a b@usc.edu"));
        assertFalse(validator.isValidUscEmail("ab@usc.edu\n"));
    }

    @Test
    public void nullOrEmptyEmail_isRejected() {
        assertFalse(validator.isValidUscEmail(null));
        assertFalse(validator.isValidUscEmail(""));
    }

    @Test
    public void tenDigitId_isAccepted() {
        assertTrue(validator.isValidUscId("1234567890"));
    }

    @Test
    public void idOfWrongLength_isRejected() {
        assertFalse(validator.isValidUscId("123456789"));
        assertFalse(validator.isValidUscId("12345678901"));
    }

    @Test
    public void idWithNonDigits_isRejected() {
        assertFalse(validator.isValidUscId("12345abcde"));
        assertFalse(validator.isValidUscId("123456789 "));
    }

    @Test
    public void nullOrEmptyId_isRejected() {
        assertFalse(validator.isValidUscId(null));
        assertFalse(validator.isValidUscId(""));
    }

    @Test
    public void positiveAmount_isAccepted() {
        assertTrue(validator.isValidAmount(1));
    }

    @Test
    public void zeroAmount_isRejected() {
        assertFalse(validator.isValidAmount(0));
    }

    @Test
    public void negativeAmount_isRejected() {
        assertFalse(validator.isValidAmount(-1));
    }
}
