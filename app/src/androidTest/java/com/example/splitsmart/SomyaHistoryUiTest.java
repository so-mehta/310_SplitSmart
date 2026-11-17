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

/** Somya Mehta: five requirements-based Espresso cases. Assertions inspect rendered UI only. */
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

    private void filters() { onView(withText("Show filters")).perform(click()); }
    private void choose(String current, String value) {
        onView(allOf(withSpinnerText(current), isDisplayed())).perform(click());
        onData(allOf(is(instanceOf(String.class)), is(value))).perform(click());
    }
    private void summaryTab() {
        onView(allOf(withContentDescription("Summary"), isDisplayed())).perform(new ViewAction() {
            public Matcher<View> getConstraints() { return allOf(isDisplayed(), isClickable()); }
            public String getDescription() { return "Activate the Summary tab using its accessible click action"; }
            public void perform(UiController ui, View view) {
                assertTrue("Summary must accept its public accessibility click action",
                        view.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK, null));
                ui.loopMainThreadUntilIdle();
            }
        });
        awaitDisplayedText("Monthly summary");
        assertMonth(month);
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
