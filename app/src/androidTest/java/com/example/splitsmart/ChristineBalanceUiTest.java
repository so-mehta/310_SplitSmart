package com.example.splitsmart;

import android.content.Intent;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.splitsmart.ui.group.GroupListActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.NoMatchingViewException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class ChristineBalanceUiTest {
    private static final String ACTIVE = "QA Balances Active";
    private static final String EMPTY = "QA Balances Empty";

    private ActivityScenario<GroupListActivity> scenario;

    @Before
    public void openGroupList() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assertNotNull("Sign in as Christine and prepare the two QA groups before running.", user);
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), GroupListActivity.class);
        intent.putExtra(GroupListActivity.EXTRA_USER_ID, user.getUid());
        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void closeGroupList() {
        if (scenario != null) {
            scenario.close();
        }
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
        onView(allOf(withText("Balances"), isDisplayed())).perform(click());
    }

    private void awaitText(String expected) {
        long deadline = SystemClock.elapsedRealtime() + 10_000;
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
}
