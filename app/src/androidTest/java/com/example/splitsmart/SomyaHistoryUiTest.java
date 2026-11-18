package com.example.splitsmart;

import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.splitsmart.ui.group.GroupDetailActivity;
import org.hamcrest.Matcher;
import org.junit.*;
import org.junit.runner.RunWith;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/** Somya Mehta: eight requirements-based Espresso cases. Assertions inspect rendered UI only. */
@RunWith(AndroidJUnit4.class)
public class SomyaHistoryUiTest {
    private ActivityScenario<GroupDetailActivity> scenario;
    private YearMonth month;

    @Before public void openGroup() {
        assertTrue("Run with -PsomyaTestRunner=com.example.splitsmart.SomyaHistoryTestRunner",
                ApplicationProvider.getApplicationContext() instanceof SomyaHistoryTestApplication);
        SomyaHistoryTestApplication app = ApplicationProvider.getApplicationContext();
        app.resetFixtures(); month = app.baseMonth;
        Intent intent = new Intent(app, GroupDetailActivity.class)
                .putExtra(GroupDetailActivity.EXTRA_GROUP_ID, SomyaHistoryTestApplication.GROUP)
                .putExtra(GroupDetailActivity.EXTRA_USER_ID, SomyaHistoryTestApplication.USER);
        scenario = ActivityScenario.launch(intent);
        await(() -> onView(allOf(withText("Group history"), isDisplayed())).check(matches(isDisplayed())));
        assertRows(uber(), rent(), payment(), lunch(), pizza(), stay());
    }

    @After public void closeGroup() { if (scenario != null) scenario.close(); }

    @Test public void search_matchesExpenseTitlesAndSettlementNotesIgnoringCaseAndSpaces() {
        onView(withHint("Search titles and notes")).perform(replaceText("  pIzZa  "), closeSoftKeyboard());
        assertRows(payment(), pizza());
        onView(withHint("Search titles and notes")).perform(replaceText("no-such-transaction"), closeSoftKeyboard());
        awaitDisplayedText("No transactions found");
        assertRows();
    }

    @Test public void combinedFilters_intersectTypeCategoryAndMemberAndClearRestoresHistory() {
        filters();
        choose("All transactions", "Expenses");
        choose("All categories", "FOOD");
        choose("All members", "Alex");
        onView(withText("Hide filters")).perform(click());
        assertRows(pizza());
        filters();
        onView(withText("Clear search and filters")).perform(click());
        onView(withText("Hide filters")).perform(click());
        assertRows(uber(), rent(), payment(), lunch(), pizza(), stay());
    }

    @Test public void sort_reversesDatesAndOrdersSameDayTransactionsByCreationTime() {
        // The settlement was created after Lunch on the same date.
        filters(); choose("Newest first", "Oldest first");
        onView(withText("Hide filters")).perform(click());
        assertRows(stay(), pizza(), lunch(), payment(), rent(), uber());
        filters(); choose("Oldest first", "Newest first");
        onView(withText("Hide filters")).perform(click());
        assertRows(uber(), rent(), payment(), lunch(), pizza(), stay());
    }

    @Test public void summary_monthNavigationUsesExpenseDatesAndExcludesSettlements() {
        summaryTab();
        assertSummary("3 expenses\n$1,240.00", "FOOD: $40.00\nHOUSING: $1,200.00\nTRANSPORTATION: $0.00\nENTERTAINMENT: $0.00\nOTHER: $0.00\n");
        onView(withText("Previous month")).perform(scrollTo(), click());
        assertMonth(month.minusMonths(1));
        assertSummary("1 expenses\n$90.00", "FOOD: $0.00\nHOUSING: $90.00\nTRANSPORTATION: $0.00\nENTERTAINMENT: $0.00\nOTHER: $0.00\n");
        onView(withText("Next month")).perform(scrollTo(), click());
        assertMonth(month);
        assertSummary("3 expenses\n$1,240.00", "FOOD: $40.00\nHOUSING: $1,200.00\nTRANSPORTATION: $0.00\nENTERTAINMENT: $0.00\nOTHER: $0.00\n");
    }

    @Test public void summary_emptyMonthAndSelectedMonthSurviveActivityRecreation() {
        summaryTab();
        onView(withText("Next month")).perform(scrollTo(), click());
        assertSummary("1 expenses\n$25.00", "FOOD: $0.00\nHOUSING: $0.00\nTRANSPORTATION: $25.00\nENTERTAINMENT: $0.00\nOTHER: $0.00\n");
        onView(withText("Next month")).perform(scrollTo(), click());
        assertMonth(month.plusMonths(2));
        assertEmptySummary();
        scenario.recreate();
        // Check the selected month before switching tabs: the Summary tab must also restore.
        assertMonth(month.plusMonths(2));
        assertEmptySummary();
    }

    @Test public void detailsAndRemainingFilters_showCompleteTransactionsAndRestrictOtherCreators() {
        // This keyword exists only in Rent's note, never its title.
        search("  LEASE renewal  ");
        assertRows(rent());
        onView(withText(rent())).perform(click());
        awaitDisplayedText("Rent\n$1,200.00\nCategory: HOUSING\nPaid by: Somya\nCreated by: Somya\nDate: "
                + month.atEndOfMonth() + "\nSplit: EQUAL\nNote: lease renewal\n\nShares\nSomya: $600.00\nAlex: $600.00\n");
        onView(withText("Edit expense")).check(matches(isDisplayed()));
        onView(withText("Delete expense")).check(matches(isDisplayed()));
        androidx.test.espresso.Espresso.pressBack();
        assertRows(rent());
        search("");
        assertRows(uber(), rent(), payment(), lunch(), pizza(), stay());
        filters(); choose("All categories", "HOUSING"); hideFilters();
        assertRows(rent(), stay());
        filters(); choose("HOUSING", "TRANSPORTATION"); hideFilters();
        assertRows(uber());
        clearFilters();
        filters(); choose("All members", "Christine"); hideFilters();
        assertRows(lunch());
        clearFilters();
        filters(); choose("All transactions", "Settlements"); hideFilters();
        assertRows(payment());
        onView(withText(payment())).perform(click());
        onView(withText("Paid by: Alex\nReceived by: Somya\nAmount: $7.00\nDate: " + month.atDay(15)
                + "\nCreated by: Alex\nNote: pizza refund")).check(matches(isDisplayed()));
        // Somya can inspect Alex's settlement but cannot change it through the UI.
        onView(withText("Edit")).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist());
        onView(withText("Delete")).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist());
        onView(withText("Close")).perform(click());
        assertRows(payment());
    }

    @Test public void expenseLifecycle_refreshesHistoryBalancesAndMonthlyCategoryTotals() {
        onView(withText("Add expense")).perform(click());
        await(() -> onView(withHint("Dinner, groceries, rent...")).check(matches(isEnabled())));
        onView(withHint("Dinner, groceries, rent...")).perform(replaceText("Museum"), closeSoftKeyboard());
        onView(withHint("0.00")).perform(replaceText("20.00"), closeSoftKeyboard());
        chooseInForm("OTHER", "ENTERTAINMENT");
        onView(allOf(withText("Alex"), isAssignableFrom(android.widget.CheckBox.class))).perform(scrollTo(), click());
        onView(withHint("Add a note")).perform(scrollTo(), replaceText("gallery tickets"), closeSoftKeyboard());
        await(() -> onView(withText("Save expense")).perform(scrollTo()).check(matches(isEnabled())));
        onView(withText("Save expense")).perform(click());
        awaitDisplayedText("Group history");
        search("gallery tickets");
        String created = "Expense: Museum\nSomya paid $20.00\nInvolved: Somya, Alex\n" + java.time.LocalDate.now();
        assertRows(created);
        assertCurrentSummary("4 expenses\n$1,260.00", "20.00", "0.00");
        assertBalanceWithAlex("Alex owes you $560.50");
        tab("History"); assertRows(created);
        onView(withText(created)).perform(click());
        awaitDisplayedText("Museum\n$20.00\nCategory: ENTERTAINMENT\nPaid by: Somya\nCreated by: Somya\nDate: "
                + java.time.LocalDate.now() + "\nSplit: EQUAL\nNote: gallery tickets\n\nShares\nSomya: $10.00\nAlex: $10.00\n");
        onView(withText("Edit expense")).perform(scrollTo(), click());
        await(() -> onView(withHint("0.00")).check(matches(withText("20.00"))));
        onView(withHint("0.00")).perform(replaceText("24.00"), closeSoftKeyboard());
        chooseInForm("ENTERTAINMENT", "OTHER");
        onView(withHint("Add a note")).perform(scrollTo(), replaceText("gallery tickets updated"), closeSoftKeyboard());
        await(() -> onView(withText("Save expense")).perform(scrollTo()).check(matches(isEnabled())));
        onView(withText("Save expense")).perform(click());
        awaitDisplayedText("Museum\n$24.00\nCategory: OTHER\nPaid by: Somya\nCreated by: Somya\nDate: "
                + java.time.LocalDate.now() + "\nSplit: EQUAL\nNote: gallery tickets updated\n\nShares\nSomya: $12.00\nAlex: $12.00\n");
        androidx.test.espresso.Espresso.pressBack();
        String edited = "Expense: Museum\nSomya paid $24.00\nInvolved: Somya, Alex\n" + java.time.LocalDate.now();
        assertRows(edited);
        assertCurrentSummary("4 expenses\n$1,264.00", "0.00", "24.00");
        assertBalanceWithAlex("Alex owes you $562.50");
        tab("History"); assertRows(edited);
        onView(withText(edited)).perform(click());
        await(() -> onView(withText("Delete expense")).check(matches(isEnabled())));
        onView(withText("Delete expense")).perform(scrollTo(), click());
        onView(withText("Are you sure you want to delete this expense?")).check(matches(isDisplayed()));
        onView(withText("Cancel")).perform(click());
        androidx.test.espresso.Espresso.pressBack();
        assertRows(edited); // Cancel must leave the transaction intact.
        onView(withText(edited)).perform(click());
        await(() -> onView(withText("Delete expense")).check(matches(isEnabled())));
        onView(withText("Delete expense")).perform(scrollTo(), click());
        onView(withText("Delete")).perform(click());
        awaitDisplayedText("No transactions found"); assertRows();
        assertCurrentSummary("3 expenses\n$1,240.00", "0.00", "0.00");
        assertBalanceWithAlex("Alex owes you $550.50");
        tab("History"); search("");
        assertRows(uber(), rent(), payment(), lunch(), pizza(), stay());
    }

    @Test public void settlementLifecycle_validatesDebtLimitAndRefreshesHistoryAndBalances() {
        tab("Balances");
        awaitDisplayedText("You owe Christine $5.00");
        openSettlementForm();
        await(() -> onView(withId(R.id.spinner_receiver)).check(matches(withSpinnerText("Christine (you owe $5.00)"))));
        settlementAmount("0.00");
        awaitDisplayedText("Amount must be greater than zero.");
        settlementAmount("-1.00");
        awaitDisplayedText("Enter a non-negative amount with at most two decimal places.");
        settlementAmount("5.01");
        awaitDisplayedText("Settlement amount cannot be more than the amount owed.");
        // Rejected submissions must not change history or balances.
        androidx.test.espresso.Espresso.pressBack();
        awaitDisplayedText("You owe Christine $5.00");
        tab("History"); assertRows(uber(), rent(), payment(), lunch(), pizza(), stay());
        tab("Balances"); openSettlementForm();
        await(() -> onView(withId(R.id.et_amount)).check(matches(isDisplayed())));
        onView(withId(R.id.et_note)).perform(scrollTo(), replaceText("book repayment"), closeSoftKeyboard());
        settlementAmount("5.00"); // Exactly the current debt is allowed.
        awaitDisplayedText("Settled with Christine");
        tab("History"); search("book repayment");
        String created = ownPayment("5.00"); assertRows(created);
        assertCurrentSummary("3 expenses\n$1,240.00", "0.00", "0.00");
        tab("History"); assertRows(created);
        openHistoryRow(created);
        assertOwnSettlementDetail("5.00", "book repayment");
        onView(withText("Edit")).perform(click());
        onView(withHint("Amount (USD)")).perform(replaceText("3.25"), closeSoftKeyboard());
        onView(withHint("Note (optional)")).perform(replaceText("book repayment updated"), closeSoftKeyboard());
        onView(withText("Save")).perform(click());
        String edited = ownPayment("3.25"); assertRows(edited);
        tab("Balances"); awaitDisplayedText("You owe Christine $1.75");
        assertCurrentSummary("3 expenses\n$1,240.00", "0.00", "0.00");
        tab("History"); assertRows(edited);
        openHistoryRow(edited);
        assertOwnSettlementDetail("3.25", "book repayment updated");
        onView(withText("Delete")).perform(click());
        onView(withText("Delete settlement?")).check(matches(isDisplayed()));
        onView(withText("Cancel")).perform(click());
        assertRows(edited);
        tab("Balances"); awaitDisplayedText("You owe Christine $1.75");
        tab("History"); assertRows(edited);
        openHistoryRow(edited);
        onView(withText("Delete")).perform(click());
        onView(withText("Delete settlement?")).check(matches(isDisplayed()));
        onView(withText("Delete")).perform(click());
        awaitDisplayedText("No transactions found"); assertRows();
        tab("Balances"); awaitDisplayedText("You owe Christine $5.00");
        assertCurrentSummary("3 expenses\n$1,240.00", "0.00", "0.00");
        tab("History"); search("");
        assertRows(uber(), rent(), payment(), lunch(), pizza(), stay());
    }

    private void search(String keyword) {
        onView(withHint("Search titles and notes")).perform(replaceText(keyword), closeSoftKeyboard());
    }
    private void hideFilters() { onView(withText("Hide filters")).perform(click()); }
    private void clearFilters() {
        filters(); onView(withText("Clear search and filters")).perform(click()); hideFilters();
        assertRows(uber(), rent(), payment(), lunch(), pizza(), stay());
    }
    private void chooseInForm(String current, String value) {
        onView(withSpinnerText(current)).perform(scrollTo(), click());
        onData(allOf(is(instanceOf(String.class)), is(value))).perform(click());
    }
    private void assertCurrentSummary(String totals, String entertainment, String other) {
        summaryTab();
        assertSummary(totals, "FOOD: $40.00\nHOUSING: $1,200.00\nTRANSPORTATION: $0.00\nENTERTAINMENT: $"
                + entertainment + "\nOTHER: $" + other + "\n");
    }
    private void assertBalanceWithAlex(String expected) {
        tab("Balances"); awaitDisplayedText(expected);
    }
    private void openHistoryRow(String text) {
        // A reload can rebind this row between touch-down and touch-up. Use the row's
        // public accessibility click, as with the tabs, after its loading label clears.
        await(() -> onView(allOf(withText("Loading..."), isDisplayed()))
                .check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist()));
        onView(withText(text)).perform(new ViewAction() {
            public Matcher<View> getConstraints() { return allOf(isDisplayed(), isClickable()); }
            public String getDescription() { return "Open the rendered transaction through its accessible click action"; }
            public void perform(UiController ui, View view) {
                assertTrue("Transaction row must accept its public accessibility click action",
                        view.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK, null));
                ui.loopMainThreadUntilIdle();
            }
        });
    }
    private void openSettlementForm() {
        await(() -> onView(withId(R.id.btn_record_settlement)).check(matches(isCompletelyDisplayed())));
        onView(withId(R.id.btn_record_settlement)).perform(click());
    }
    private void settlementAmount(String amount) {
        onView(withId(R.id.et_amount)).perform(scrollTo(), replaceText(amount), closeSoftKeyboard());
        await(() -> onView(withId(R.id.btn_save)).perform(scrollTo()).check(matches(isEnabled())));
        onView(withId(R.id.btn_save)).perform(click());
    }
    private String ownPayment(String amount) {
        return "Settlement: Somya paid Christine $" + amount + "\n" + java.time.LocalDate.now();
    }
    private void assertOwnSettlementDetail(String amount, String note) {
        awaitDisplayedText("Paid by: Somya\nReceived by: Christine\nAmount: $" + amount + "\nDate: "
                + java.time.LocalDate.now() + "\nCreated by: Somya\nNote: " + note);
    }

    private void filters() { onView(withText("Show filters")).perform(click()); }
    private void choose(String current, String value) {
        onView(allOf(withSpinnerText(current), isDisplayed())).perform(click());
        onData(allOf(is(instanceOf(String.class)), is(value))).perform(click());
    }
    private void summaryTab() {
        tab("Summary");
        awaitDisplayedText("Monthly summary");
        assertMonth(month);
    }
    private void tab(String label) {
        onView(allOf(withContentDescription(label), isDisplayed())).perform(new ViewAction() {
            public Matcher<View> getConstraints() { return allOf(isDisplayed(), isClickable()); }
            public String getDescription() { return "Activate the " + label + " tab using its accessible click action"; }
            public void perform(UiController ui, View view) {
                assertTrue("Tab must accept its public accessibility click action",
                        view.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK, null));
                ui.loopMainThreadUntilIdle();
            }
        });
        await(() -> onView(withId(R.id.pager)).check((view, missing) -> {
            if (missing != null) throw missing;
            assertEquals("Finish the visible tab transition before interacting with its contents",
                    androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE,
                    ((androidx.viewpager2.widget.ViewPager2) view).getScrollState());
        }));
    }
    private void assertMonth(YearMonth expected) {
        String label = expected.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US));
        await(() -> onView(withText(label)).perform(scrollTo()).check(matches(isDisplayed())));
    }
    private void assertEmptySummary() {
        assertSummary("0 expenses\n$0.00", "FOOD: $0.00\nHOUSING: $0.00\nTRANSPORTATION: $0.00\nENTERTAINMENT: $0.00\nOTHER: $0.00\n");
    }
    private void assertSummary(String totals, String categories) {
        await(() -> onView(withText(totals)).perform(scrollTo()).check(matches(isDisplayed())));
        await(() -> onView(withText(categories)).perform(scrollTo()).check(matches(isDisplayed())));
    }
    private Matcher<View> historyList() {
        // Exclude ViewPager2's internal RecyclerView and the offscreen Balances list.
        return allOf(isAssignableFrom(RecyclerView.class),
                withParent(isAssignableFrom(android.widget.LinearLayout.class)), isDisplayed());
    }
    private void assertRows(String... expected) {
        // Read only widget count and rendered TextViews, never ViewModel/repository contents.
        await(() -> onView(historyList()).check((view, missing) -> {
            if (missing != null) throw missing;
            assertEquals(expected.length, ((RecyclerView) view).getAdapter().getItemCount());
        }));
        for (int i = 0; i < expected.length; i++) {
            final int position = i;
            onView(historyList()).perform(new ViewAction() {
                public Matcher<View> getConstraints() { return isAssignableFrom(RecyclerView.class); }
                public String getDescription() { return "Scroll to rendered history row " + position; }
                public void perform(UiController ui, View view) {
                    ((RecyclerView) view).scrollToPosition(position); ui.loopMainThreadUntilIdle();
                }
            });
            await(() -> onView(historyList()).check((view, missing) -> {
                if (missing != null) throw missing;
                RecyclerView.ViewHolder holder = ((RecyclerView) view).findViewHolderForAdapterPosition(position);
                assertNotNull("History row " + position + " must be rendered", holder);
                assertEquals(expected[position], ((TextView) holder.itemView).getText().toString());
            }));
        }
    }
    private static void await(Runnable assertion) {
        long deadline = SystemClock.elapsedRealtime() + 10000;
        while (true) {
            try { assertion.run(); return; }
            catch (AssertionError | NoMatchingViewException retry) {
                if (SystemClock.elapsedRealtime() >= deadline) throw retry;
                SystemClock.sleep(50);
            }
        }
    }
    private static void awaitDisplayedText(String text) {
        onView(isRoot()).perform(new ViewAction() {
            public Matcher<View> getConstraints() { return isRoot(); }
            public String getDescription() { return "Wait for visible text: " + text; }
            public void perform(UiController ui, View root) {
                Matcher<View> target = allOf(withText(text), isDisplayed());
                long deadline = SystemClock.elapsedRealtime() + 10000;
                do {
                    for (View child : androidx.test.espresso.util.TreeIterables.breadthFirstViewTraversal(root)) {
                        if (target.matches(child)) return;
                    }
                    ui.loopMainThreadForAtLeast(50);
                } while (SystemClock.elapsedRealtime() < deadline);
                throw new AssertionError("Expected visible text: " + text);
            }
        });
    }
    private String pizza() { return "Expense: Pizza\nSomya paid $30.00\nInvolved: Somya, Alex\n" + month.atDay(1); }
    private String lunch() { return "Expense: Lunch\nChristine paid $10.00\nInvolved: Christine, Somya\n" + month.atDay(15); }
    private String rent() { return "Expense: Rent\nSomya paid $1,200.00\nInvolved: Somya, Alex\n" + month.atEndOfMonth(); }
    private String stay() { return "Expense: Stay\nAlex paid $90.00\nInvolved: Alex, Somya\n" + month.minusMonths(1).atEndOfMonth(); }
    private String uber() { return "Expense: Uber\nAlex paid $25.00\nInvolved: Alex, Somya\n" + month.plusMonths(1).atDay(1); }
    private String payment() { return "Settlement: Alex paid Somya $7.00\n" + month.atDay(15); }
}
