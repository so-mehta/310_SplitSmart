package com.example.splitsmart;

import android.app.Activity;
import android.content.Intent;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.example.splitsmart.model.Category;
import com.example.splitsmart.model.Expense;
import com.example.splitsmart.model.Share;
import com.example.splitsmart.model.SplitMethod;
import com.example.splitsmart.service.AccountService;
import com.example.splitsmart.service.GroupService;
import com.example.splitsmart.ui.group.GroupListActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.NoMatchingViewException;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Random;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

/**
 * Feature 4 black-box tests (Christine).
 *
 * No manual setup is needed. Before the tests run, this class creates three fresh accounts named
 * Christine, Alex and Simra (random @usc.edu emails and USC IDs), signs in as Christine, and
 * creates the two QA groups with all three as members. "QA Balances Active" gets these four
 * equal-split expenses; "QA Balances Empty" gets none:
 *
 *   Dinner   $20.00  paid by Alex       Christine and Alex
 *   Parking  $10.00  paid by Alex       Christine and Alex
 *   Coffee    $8.00  paid by Christine  Christine and Alex
 *   Lunch     $8.00  paid by Christine  Christine and Simra
 *
 * After the tests, both groups (and their expenses) are deleted and the user is signed out.
 * Uses the real Firebase backend, so the emulator needs internet access.
 */
@RunWith(AndroidJUnit4.class)
public class ChristineBalanceUiTest {
    private static final String ACTIVE = "QA Balances Active";
    private static final String EMPTY = "QA Balances Empty";
    private static final String PASSWORD = "fighton123";
    private static final Random RANDOM = new Random();

    private static String christineEmail;
    private static String christineId;
    private static String activeGroupId;
    private static String emptyGroupId;

    private ActivityScenario<GroupListActivity> scenario;

    @BeforeClass
    public static void createAccountsGroupsAndExpenses() {
        SplitSmartApp app = ApplicationProvider.getApplicationContext();
        AccountService accounts = app.getAccountService();
        GroupService groups = app.getGroupService();
        String run = Long.toString(System.currentTimeMillis(), 36) + RANDOM.nextInt(1000);

        FirebaseAuth.getInstance().signOut();
        String alexEmail = "alex" + run + "@usc.edu";
        String simraEmail = "simra" + run + "@usc.edu";
        christineEmail = "christine" + run + "@usc.edu";
        String alexId = accounts.register("Alex", alexEmail, newUscId(), PASSWORD).getUserId();
        String simraId = accounts.register("Simra", simraEmail, newUscId(), PASSWORD).getUserId();
        // Registering signs the new user in, so Christine is signed in from here on.
        christineId = accounts.register("Christine", christineEmail, newUscId(), PASSWORD)
                .getUserId();

        emptyGroupId = groups.createGroup(christineId, EMPTY, "").getGroupId();
        activeGroupId = groups.createGroup(christineId, ACTIVE, "").getGroupId();
        for (String groupId : Arrays.asList(emptyGroupId, activeGroupId)) {
            groups.addMemberByEmail(christineId, groupId, alexEmail);
            groups.addMemberByEmail(christineId, groupId, simraEmail);
        }

        addEqualExpense(app, "Dinner", 2000, alexId, christineId, alexId);
        addEqualExpense(app, "Parking", 1000, alexId, christineId, alexId);
        addEqualExpense(app, "Coffee", 800, christineId, christineId, alexId);
        addEqualExpense(app, "Lunch", 800, christineId, christineId, simraId);
    }

    @AfterClass
    public static void deleteGroupsAndSignOut() {
        SplitSmartApp app = ApplicationProvider.getApplicationContext();
        GroupService groups = app.getGroupService();
        if (christineId != null) {
            signInAsChristine();
            if (activeGroupId != null) {
                groups.deleteGroup(christineId, activeGroupId);
            }
            if (emptyGroupId != null) {
                groups.deleteGroup(christineId, emptyGroupId);
            }
        }
        FirebaseAuth.getInstance().signOut();
    }

    @Before
    public void openGroupList() {
        signInAsChristine();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), GroupListActivity.class);
        intent.putExtra(GroupListActivity.EXTRA_USER_ID, christineId);
        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void closeGroupList() {
        if (scenario != null) {
            scenario.close();
        }
        // Group screens opened from the list are not tracked by the scenario, so close them too.
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (Stage stage : EnumSet.range(Stage.PRE_ON_CREATE, Stage.STOPPED)) {
                for (Activity activity
                        : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage)) {
                    activity.finish();
                }
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @Test
    public void showsAlexNetPayable() {
        openBalances(ACTIVE);
        awaitText("You owe Alex $11.00");
    }

    @Test
    public void showsSimraReceivable() {
        openBalances(ACTIVE);
        awaitText("Simra owes you $4.00");
    }

    @Test
    public void keepsPayableAndReceivableCardsSeparate() {
        openBalances(ACTIVE);
        awaitText("Simra owes you $4.00");
        assertTotals("$11.00", "$4.00");
    }

    @Test
    public void sameMembersInAnotherGroupHaveZeroBalances() {
        openBalances(EMPTY);
        awaitText("Settled with Alex");
        awaitText("Settled with Simra");
        assertTotals("$0.00", "$0.00");
    }

    @Test
    public void switchingGroupsRestoresTheActiveGroupsBalances() {
        openBalances(EMPTY);
        awaitText("Settled with Alex");
        pressBack();
        openBalances(ACTIVE);
        awaitText("You owe Alex $11.00");
        assertTotals("$11.00", "$4.00");
    }

    private void assertTotals(String payable, String receivable) {
        onView(withId(R.id.tv_total_owe)).check(matches(withText(payable)));
        onView(withId(R.id.tv_total_owed)).check(matches(withText(receivable)));
    }

    private void openBalances(String group) {
        awaitText(group);
        onView(allOf(withText(group), isDisplayed())).perform(click());
        awaitText("Balances");
        // A tap on the tab can be lost while the tabs are still being set up, which leaves the
        // History tab selected. Tap again until the Balances page is actually showing.
        long deadline = SystemClock.elapsedRealtime() + 30_000;
        while (true) {
            onView(allOf(withText("Balances"), isDisplayed())).perform(click());
            try {
                onView(allOf(withId(R.id.tv_total_owe), isDisplayed())).check(matches(isDisplayed()));
                return;
            } catch (NoMatchingViewException notShowingYet) {
                if (SystemClock.elapsedRealtime() >= deadline) {
                    throw notShowingYet;
                }
                SystemClock.sleep(500);
            }
        }
    }

    private void awaitText(String expected) {
        // Balances load from Firebase in the background; allow for a slow emulator or network.
        long deadline = SystemClock.elapsedRealtime() + 30_000;
        while (true) {
            try {
                onView(allOf(withText(expected), isDisplayed())).check(matches(isDisplayed()));
                return;
            } catch (NoMatchingViewException missing) {
                if (SystemClock.elapsedRealtime() >= deadline) {
                    throw missing;
                }
                SystemClock.sleep(150);
            }
        }
    }

    // ------------------------------------------------------------------ setup helpers

    private static void signInAsChristine() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !user.getUid().equals(christineId)) {
            SplitSmartApp app = ApplicationProvider.getApplicationContext();
            app.getAccountService().login(christineEmail, PASSWORD);
        }
    }

    /** Christine records an expense in the active group, split equally between two people. */
    private static void addEqualExpense(SplitSmartApp app, String title, long amountCents,
                                        String payerId, String firstId, String secondId) {
        long half = amountCents / 2;
        app.getExpenseService().createExpense(christineId, new Expense(null, activeGroupId, null,
                amountCents, null, "", title, Category.OTHER, payerId, SplitMethod.EQUAL,
                Arrays.asList(new Share(firstId, half, 50), new Share(secondId, half, 50))));
    }

    private static String newUscId() {
        StringBuilder id = new StringBuilder("9");
        for (int i = 0; i < 9; i++) {
            id.append(RANDOM.nextInt(10));
        }
        return id.toString();
    }
}
