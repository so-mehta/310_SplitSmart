package com.example.splitsmart;

import android.widget.CheckBox;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.splitsmart.ui.expense.AddEditExpenseActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.LinkedHashMap;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Feature 3 black-box tests on the Add expense form. A new expense's preview never reads the
 * database, so these need no sign-in or seeded data: the form opens for a three-member group
 * with Alex as the person adding the expense.
 */
@RunWith(AndroidJUnit4.class)
public class AlexSplitUiTest {

    private ActivityScenario<AddEditExpenseActivity> scenario;

    @Before
    public void openAddExpenseForm() {
        LinkedHashMap<String, String> members = new LinkedHashMap<>();
        members.put("alex", "Alex");
        members.put("simra", "Simra");
        members.put("christine", "Christine");
        scenario = ActivityScenario.launch(AddEditExpenseActivity.intent(
                ApplicationProvider.getApplicationContext(), "qa-split-group", "alex", members, null));
        type(withHint("Dinner, groceries, rent..."), "Pizza");
        tick("Simra");
        tick("Christine");
    }

    @After
    public void closeForm() {
        if (scenario != null) {
            scenario.close();
        }
    }

    @Test
    public void equalSplit_thirtyDollarsAmongThree_previewsTenEach() {
        amount("30.00");

        onView(withSpinnerText("EQUAL")).check(matches(isDisplayed()));
        assertPreview("Alex: $10.00\nSimra: $10.00\nChristine: $10.00");
        assertSaveEnabled(true);
    }

    @Test
    public void exactSplit_matchingAmountsAndAZeroShare_areAccepted() {
        amount("30.00");
        method("EQUAL", "EXACT AMOUNT");

        shares("$", "5.00", "10.00", "15.00");
        assertPreview("Alex: $5.00\nSimra: $10.00\nChristine: $15.00");
        assertSaveEnabled(true);

        shares("$", "0.00", "15.00", "15.00");
        assertPreview("Alex: $0.00\nSimra: $15.00\nChristine: $15.00");
        assertSaveEnabled(true);
    }

    @Test
    public void exactSplit_mismatchedTotal_isRejectedAndSaveIsDisabled() {
        amount("30.00");
        method("EQUAL", "EXACT AMOUNT");

        shares("$", "5.00", "10.00", "10.00");
        assertPreview("Exact shares are $5.00 short. Increase the shares to match the total.");
        assertSaveEnabled(false);

        shares("$", "5.00", "10.00", "20.00");
        assertPreview("Exact shares are $5.00 over. Decrease the shares to match the total.");
        assertSaveEnabled(false);
    }

    @Test
    public void percentageSplit_acceptsZeroPercentAndRejectsTotalsOtherThanOneHundred() {
        amount("30.00");
        method("EQUAL", "PERCENTAGE");

        shares("%", "20", "30", "50");
        assertPreview("Alex: $6.00\nSimra: $9.00\nChristine: $15.00");
        assertSaveEnabled(true);

        shares("%", "0", "50", "50");
        assertPreview("Alex: $0.00\nSimra: $15.00\nChristine: $15.00");
        assertSaveEnabled(true);

        shares("%", "20", "30", "40");
        assertPreview("Percentages total 90%. Adjust them to total 100%.");
        assertSaveEnabled(false);
    }

    @Test
    public void rounding_tenDollarsThreeWays_sharesStillSumToTheTotal() {
        amount("10.00");
        assertPreview("Alex: $3.34\nSimra: $3.33\nChristine: $3.33");

        method("EQUAL", "PERCENTAGE");
        shares("%", "33.33", "33.33", "33.34");
        assertPreview("Alex: $3.34\nSimra: $3.33\nChristine: $3.33");
        assertSaveEnabled(true);
    }

    private void amount(String dollars) {
        type(withHint("0.00"), dollars);
    }

    private void method(String current, String next) {
        onView(withSpinnerText(current)).perform(scrollTo(), click());
        onData(allOf(is(instanceOf(String.class)), is(next))).perform(click());
    }

    /** Types one value per participant, in member order, into the split inputs for that unit. */
    private void shares(String unit, String alex, String simra, String christine) {
        type(withContentDescription("Alex (" + unit + ")"), alex);
        type(withContentDescription("Simra (" + unit + ")"), simra);
        type(withContentDescription("Christine (" + unit + ")"), christine);
    }

    private void tick(String member) {
        onView(allOf(withText(member), isAssignableFrom(CheckBox.class))).perform(scrollTo(), click());
    }

    private void type(org.hamcrest.Matcher<android.view.View> field, String text) {
        onView(field).perform(scrollTo(), replaceText(text));
    }

    private void assertPreview(String expected) {
        onView(withText(expected)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    private void assertSaveEnabled(boolean enabled) {
        onView(withText("Save expense")).perform(scrollTo())
                .check(matches(enabled ? isEnabled() : not(isEnabled())));
    }
}
