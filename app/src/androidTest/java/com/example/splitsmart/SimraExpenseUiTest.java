package com.example.splitsmart;

import android.content.Context;
import android.widget.CheckBox;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.splitsmart.model.Category;
import com.example.splitsmart.model.Expense;
import com.example.splitsmart.model.Group;
import com.example.splitsmart.model.Share;
import com.example.splitsmart.model.SortOrder;
import com.example.splitsmart.model.SplitMethod;
import com.example.splitsmart.model.Transaction;
import com.example.splitsmart.model.User;
import com.example.splitsmart.ui.expense.AddEditExpenseActivity;
import com.example.splitsmart.ui.expense.ExpenseDetailActivity;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Feature 2 (expense management) black-box tests. Tests 1 and 2 open the Add expense form
 * directly for a three-member group (Simra adding, with Christine and Alex) and need no sign-in
 * or data. Tests 3 to 5 use the real Firebase backend, so the emulator needs internet access:
 * each creates three accounts with random emails, a group and (for tests 4 and 5) one expense,
 * and deletes the group afterwards. Only what the user sees on screen is checked.
 */
@RunWith(AndroidJUnit4.class)
public class SimraExpenseUiTest {

    private static final String PASSWORD = "secret1";

    private ActivityScenario<?> scenario;
    private Fixture fixture;

    @After
    public void cleanUp() {
        closeScreen();
        if (fixture != null) {
            try {
                fixture.app.getAccountService().login(fixture.simra.getEmail(), PASSWORD);
                fixture.app.getGroupService().deleteGroup(fixture.simra.getUserId(), fixture.groupId);
            } finally {
                fixture.app.getAccountService().logout();
            }
        }
    }

    @Test
    public void form_categoryDefaultsToOtherWithFiveChoices_andAmountRulesAreEnforced() {
        openAddForm(offlineMembers(), "bb-expense-group", "simra");

        // Exactly five categories, defaulting to Other; choosing one replaces the other.
        onView(withSpinnerText("OTHER")).check(matches(isDisplayed()));
        onView(withSpinnerText("OTHER")).perform(scrollTo(), click());
        for (String category : new String[]{"FOOD", "HOUSING", "TRANSPORTATION", "ENTERTAINMENT", "OTHER"}) {
            onData(allOf(is(instanceOf(String.class)), is(category))).check(matches(isDisplayed()));
        }
        onData(allOf(is(instanceOf(String.class)), is("HOUSING"))).perform(click());
        onView(withSpinnerText("HOUSING")).check(matches(isDisplayed()));

        // Amount: zero, a negative amount and three decimals cannot be saved; two decimals can.
        type(withHint("Dinner, groceries, rent..."), "Lunch");
        tick("Christine");

        enterAmount("0");
        assertPreview("Amount must be greater than zero.");
        assertSaveEnabled(false);

        enterAmount("-5");
        assertPreview("Enter a non-negative amount with at most two decimal places.");
        assertSaveEnabled(false);

        enterAmount("10.999");
        assertPreview("Enter a non-negative amount with at most two decimal places.");
        assertSaveEnabled(false);

        enterAmount("10.99");
        assertPreview("Simra: $5.50\nChristine: $5.49");
        assertSaveEnabled(true);
    }

    @Test
    public void form_payerStaysAParticipant_twoAreRequired_andAnotherMemberCanBePayer() {
        openAddForm(offlineMembers(), "bb-expense-group", "simra");
        type(withHint("Dinner, groceries, rent..."), "Rent");
        enterAmount("20.00");

        // Only the payer (Simra) is selected: ticked, locked, and not enough to save.
        onView(checkBox("Simra")).perform(scrollTo()).check(matches(allOf(isChecked(), not(isEnabled()))));
        assertPreview("Select at least two participants.");
        assertSaveEnabled(false);

        // Trying to untick the payer changes nothing.
        onView(checkBox("Simra")).perform(scrollTo(), click());
        onView(checkBox("Simra")).perform(scrollTo()).check(matches(isChecked()));
        assertSaveEnabled(false);

        tick("Christine");
        assertPreview("Simra: $10.00\nChristine: $10.00");
        assertSaveEnabled(true);

        // Simra records that Christine paid, then takes herself out of the split.
        choosePayer("Simra", "Christine");
        onView(checkBox("Christine")).perform(scrollTo()).check(matches(allOf(isChecked(), not(isEnabled()))));
        onView(checkBox("Simra")).perform(scrollTo()).check(matches(isEnabled()));
        onView(checkBox("Simra")).perform(scrollTo(), click());
        tick("Alex");

        onView(checkBox("Simra")).perform(scrollTo()).check(matches(not(isChecked())));
        assertPreview("Christine: $10.00\nAlex: $10.00");
        assertSaveEnabled(true);
    }

    @Test
    public void createThroughForm_savesEveryField_andShowsThemWhenReopened() {
        fixture = createFixture(false);

        // Simra fills in the whole form: Christine paid, and Simra is not in the split.
        openAddForm(fixture.names, fixture.groupId, fixture.simra.getUserId());
        type(withHint("Dinner, groceries, rent..."), "Groceries");
        enterAmount("45.50");
        chooseCategory("OTHER", "HOUSING");
        choosePayer("Simra", "Christine");
        onView(checkBox("Simra")).perform(scrollTo(), click());
        tick("Alex");
        type(withHint("Add a note"), "Weekly shop");
        assertPreview("Christine: $22.75\nAlex: $22.75");
        onView(withText("Save expense")).perform(scrollTo(), click());
        waitUntilScreenCloses();

        // Reopening the saved expense reads it back from the database.
        Expense saved = newestExpense(fixture);
        openDetail(fixture, fixture.simra.getUserId(), saved.getTransactionId());
        eventually(() -> onView(withText(containsString("Groceries\n$45.50"))).check(matches(isDisplayed())));
        for (String line : new String[]{"Category: HOUSING", "Paid by: Christine", "Created by: Simra",
                "Date: " + LocalDate.now(), "Split: EQUAL", "Note: Weekly shop",
                "Christine: $22.75", "Alex: $22.75"}) {
            onView(withText(containsString(line))).check(matches(isDisplayed()));
        }
        onView(withText(containsString("Groceries"))).check(matches(not(withText(containsString("Simra: $")))));
    }

    @Test
    public void creatorEdit_changesPayerParticipantsAndAmount_keepsDate_andOtherMemberSeesIt() {
        fixture = createFixture(true);
        LocalDate originalDate = fixture.dinner.getDate();

        // Simra edits her own expense: new amount, Christine pays, Alex replaces Simra.
        openEditForm(fixture, fixture.simra.getUserId(), fixture.dinner.getTransactionId());
        eventually(() -> onView(withText("Dinner")).check(matches(isDisplayed())));
        enterAmount("45.00");
        choosePayer("Simra", "Christine");
        onView(checkBox("Simra")).perform(scrollTo(), click());
        tick("Alex");
        assertPreview("Christine: $22.50\nAlex: $22.50");
        onView(withText("Save expense")).perform(scrollTo(), click());
        waitUntilScreenCloses();

        // Christine, signed in on her own, opens the expense and sees the latest values.
        fixture.app.getAccountService().login(fixture.christine.getEmail(), PASSWORD);
        openDetail(fixture, fixture.christine.getUserId(), fixture.dinner.getTransactionId());
        eventually(() -> onView(withText(containsString("Dinner\n$45.00"))).check(matches(isDisplayed())));
        for (String line : new String[]{"Paid by: Christine", "Christine: $22.50", "Alex: $22.50",
                "Created by: Simra", "Date: " + originalDate}) {
            onView(withText(containsString(line))).check(matches(isDisplayed()));
        }
        onView(withText(containsString("Dinner"))).check(matches(not(withText(containsString("Simra: $")))));
    }

    @Test
    public void permissions_otherMemberOnlyViews_creatorMustConfirmDelete() {
        fixture = createFixture(true);
        String expenseId = fixture.dinner.getTransactionId();

        // Christine can view everything but has no Edit or Delete.
        fixture.app.getAccountService().login(fixture.christine.getEmail(), PASSWORD);
        openDetail(fixture, fixture.christine.getUserId(), expenseId);
        eventually(() -> onView(withText(containsString("Dinner\n$30.00"))).check(matches(isDisplayed())));
        onView(withText("Edit expense")).check(matches(not(isDisplayed())));
        onView(withText("Delete expense")).check(matches(not(isDisplayed())));
        closeScreen();

        // Simra sees both buttons. Cancel keeps the expense; Delete removes it.
        fixture.app.getAccountService().login(fixture.simra.getEmail(), PASSWORD);
        openDetail(fixture, fixture.simra.getUserId(), expenseId);
        eventually(() -> onView(withText("Delete expense")).perform(scrollTo()).check(matches(isDisplayed())));
        onView(withText("Edit expense")).perform(scrollTo()).check(matches(isDisplayed()));

        onView(withText("Delete expense")).perform(scrollTo(), click());
        onView(withText("Delete expense?")).check(matches(isDisplayed()));
        onView(withText("Are you sure you want to delete this expense?")).check(matches(isDisplayed()));
        onView(withText("Cancel")).perform(click());
        onView(withText(containsString("Dinner\n$30.00"))).check(matches(isDisplayed()));

        onView(withText("Delete expense")).perform(scrollTo(), click());
        onView(withText("Delete")).perform(click());
        waitUntilScreenCloses();

        openDetail(fixture, fixture.simra.getUserId(), expenseId);
        eventually(() -> onView(withText("Expense does not exist.")).check(matches(isDisplayed())));
    }

    // ---- screens -------------------------------------------------------------------------

    private void openAddForm(LinkedHashMap<String, String> members, String groupId, String userId) {
        closeScreen();
        Context context = ApplicationProvider.getApplicationContext();
        scenario = ActivityScenario.launch(AddEditExpenseActivity.intent(context, groupId, userId, members, null));
    }

    private void openEditForm(Fixture f, String userId, String expenseId) {
        closeScreen();
        Context context = ApplicationProvider.getApplicationContext();
        scenario = ActivityScenario.launch(
                AddEditExpenseActivity.intent(context, f.groupId, userId, f.names, expenseId));
    }

    private void openDetail(Fixture f, String userId, String expenseId) {
        closeScreen();
        Context context = ApplicationProvider.getApplicationContext();
        scenario = ActivityScenario.launch(
                ExpenseDetailActivity.intent(context, f.groupId, userId, f.names, expenseId));
    }

    private void closeScreen() {
        if (scenario != null) {
            scenario.close();
            scenario = null;
        }
    }

    private void waitUntilScreenCloses() {
        long deadline = System.currentTimeMillis() + 15000;
        while (scenario.getState() != Lifecycle.State.DESTROYED) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("The screen did not close after the change was saved.");
            }
            sleep(100);
        }
    }

    // ---- test data -----------------------------------------------------------------------

    private static LinkedHashMap<String, String> offlineMembers() {
        LinkedHashMap<String, String> members = new LinkedHashMap<>();
        members.put("simra", "Simra");
        members.put("christine", "Christine");
        members.put("alex", "Alex");
        return members;
    }

    private static class Fixture {
        SplitSmartApp app;
        User simra;
        User christine;
        User alex;
        String groupId;
        LinkedHashMap<String, String> names = new LinkedHashMap<>();
        Expense dinner;
    }

    /** Three new accounts and a group; Simra owns it and is left signed in. */
    private static Fixture createFixture(boolean withDinner) {
        Fixture f = new Fixture();
        f.app = ApplicationProvider.getApplicationContext();
        long stamp = System.currentTimeMillis();
        f.christine = f.app.getAccountService().register(
                "Christine", "bbexpchristine" + stamp + "@usc.edu", randomUscId(), PASSWORD);
        f.alex = f.app.getAccountService().register(
                "Alex", "bbexpalex" + stamp + "@usc.edu", randomUscId(), PASSWORD);
        f.simra = f.app.getAccountService().register(
                "Simra", "bbexpsimra" + stamp + "@usc.edu", randomUscId(), PASSWORD);

        Group group = f.app.getGroupService().createGroup(f.simra.getUserId(), "BB Expenses", "");
        f.groupId = group.getGroupId();
        f.app.getGroupService().addMemberByEmail(f.simra.getUserId(), f.groupId, f.christine.getEmail());
        f.app.getGroupService().addMemberByEmail(f.simra.getUserId(), f.groupId, f.alex.getEmail());
        f.names.put(f.simra.getUserId(), "Simra");
        f.names.put(f.christine.getUserId(), "Christine");
        f.names.put(f.alex.getUserId(), "Alex");

        if (withDinner) {
            Expense dinner = new Expense("Dinner", Category.FOOD, f.simra.getUserId(), SplitMethod.EQUAL,
                    new ArrayList<>(Arrays.asList(
                            new Share(f.simra.getUserId(), 1500, 0),
                            new Share(f.christine.getUserId(), 1500, 0))));
            dinner.setGroupId(f.groupId);
            dinner.setAmountCents(3000);
            dinner.setNote("Birthday dinner");
            f.dinner = f.app.getExpenseService().createExpense(f.simra.getUserId(), dinner);
        }
        return f;
    }

    /** The most recent expense in the group, read the way the History tab reads it. */
    private static Expense newestExpense(Fixture f) {
        List<Transaction> history = f.app.getHistoryService().getHistory(f.groupId, null, SortOrder.NEWEST_FIRST);
        for (Transaction transaction : history) {
            if (transaction instanceof Expense) {
                return (Expense) transaction;
            }
        }
        throw new AssertionError("The saved expense was not found in the group.");
    }

    // ---- form helpers --------------------------------------------------------------------

    private static org.hamcrest.Matcher<android.view.View> checkBox(String member) {
        return allOf(withText(member), isAssignableFrom(CheckBox.class));
    }

    private void tick(String member) {
        onView(checkBox(member)).perform(scrollTo(), click());
    }

    private void chooseCategory(String current, String next) {
        onView(withSpinnerText(current)).perform(scrollTo(), click());
        onData(allOf(is(instanceOf(String.class)), is(next))).perform(click());
    }

    private void choosePayer(String current, String next) {
        onView(withSpinnerText(current)).perform(scrollTo(), click());
        onData(allOf(is(instanceOf(String.class)), is(next))).perform(click());
    }

    private void type(org.hamcrest.Matcher<android.view.View> field, String text) {
        onView(field).perform(scrollTo(), replaceText(text));
    }

    private void enterAmount(String dollars) {
        type(withHint("0.00"), dollars);
    }

    private void assertPreview(String expected) {
        onView(withText(expected)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    private void assertSaveEnabled(boolean enabled) {
        onView(withText("Save expense")).perform(scrollTo())
                .check(matches(enabled ? isEnabled() : not(isEnabled())));
    }

    /** Service calls run on a background thread, so poll for up to ten seconds. */
    private static void eventually(Runnable check) {
        long deadline = System.currentTimeMillis() + 10000;
        while (true) {
            try {
                check.run();
                return;
            } catch (RuntimeException | AssertionError notYet) {
                if (System.currentTimeMillis() > deadline) {
                    throw notYet;
                }
                sleep(150);
            }
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for the screen.", e);
        }
    }

    private static String randomUscId() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    }
}
