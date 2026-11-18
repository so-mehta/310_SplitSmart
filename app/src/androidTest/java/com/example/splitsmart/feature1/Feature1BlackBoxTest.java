package com.example.splitsmart.feature1;

import android.app.Activity;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.example.splitsmart.R;
import com.example.splitsmart.SplitSmartApp;
import com.example.splitsmart.model.Category;
import com.example.splitsmart.model.Expense;
import com.example.splitsmart.model.Group;
import com.example.splitsmart.model.Settlement;
import com.example.splitsmart.model.Share;
import com.example.splitsmart.model.SplitMethod;
import com.example.splitsmart.model.User;
import com.example.splitsmart.repository.firebase.FirebaseExpenseRepository;
import com.example.splitsmart.repository.firebase.FirebaseSettlementRepository;
import com.example.splitsmart.service.AccountService;
import com.example.splitsmart.service.GroupService;
import com.example.splitsmart.ui.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Random;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Feature 1 (User and Group Management) black-box system tests, written with Espresso.
 * Author: Rosycela Muniz.
 *
 * Each test drives the real app UI against the real Firebase backend and checks only what the
 * user can see on screen. The tests were derived from the Feature 1 requirements and the demo
 * script, not from the source code. Every run makes fresh accounts (unique email and USC ID), so
 * the tests do not depend on each other or on earlier runs.
 *
 * Needs an emulator or device with internet access. Run with:
 *   gradlew :app:connectedDebugAndroidTest
 *       -Pandroid.testInstrumentationRunnerArguments.class=com.example.splitsmart.feature1.Feature1BlackBoxTest
 */
@RunWith(AndroidJUnit4.class)
public class Feature1BlackBoxTest {

    private static final String PASSWORD = "fighton123";
    private static final long TIMEOUT_MS = 20_000;
    private static final Random RANDOM = new Random();

    private AccountService accountService;
    private GroupService groupService;
    private ActivityScenario<LoginActivity> scenario;
    private String run;

    @Before
    public void setUp() {
        SplitSmartApp app = ApplicationProvider.getApplicationContext();
        accountService = app.getAccountService();
        groupService = app.getGroupService();
        FirebaseAuth.getInstance().signOut();
        run = Long.toString(System.currentTimeMillis(), 36) + RANDOM.nextInt(1000);
    }

    @After
    public void tearDown() {
        closeApp();
    }

    /**
     * BB-F1-1: The sign-up screen rejects bad input and shows a message for each case.
     * Inputs (equivalence classes and boundaries from the spec): a non-@usc.edu email,
     * a 9-digit ID (one below the required length), an 11-digit ID (one above), and a
     * 10-character ID that contains a letter. The user must stay on the sign-up screen.
     */
    @Test
    public void signUp_invalidEmailOrUscId_showsErrorAndStaysOnSignUp() {
        openSignUp();

        fillSignUp("Bad Email", "bad" + run + "@gmail.com", newUscId(), PASSWORD);
        expectMessage("Enter a valid USC email address.");

        fillSignUp("Short Id", email("short"), "123456789", PASSWORD);
        expectMessage("USC ID must be exactly 10 digits.");

        fillSignUp("Long Id", email("long"), "12345678901", PASSWORD);
        expectMessage("USC ID must be exactly 10 digits.");

        fillSignUp("Letter Id", email("letter"), "12345678a0", PASSWORD);
        expectMessage("USC ID must be exactly 10 digits.");

        onView(withId(R.id.btn_sign_up)).check(matches(isDisplayed()));
    }

    /**
     * BB-F1-2: A USC email or USC ID that already has an account cannot be used again.
     * First a valid account is made through the UI. Then the same email is tried with a new ID,
     * then the same ID with a new email. Both attempts must show the duplicate message.
     */
    @Test
    public void signUp_duplicateEmailOrUscId_isRejected() {
        String takenEmail = email("tommy");
        String takenId = newUscId();
        openSignUp();
        fillSignUp("Tommy " + run, takenEmail, takenId, PASSWORD);
        waitForView(withId(R.id.fab_create));
        closeApp();

        FirebaseAuth.getInstance().signOut();
        openSignUp();
        fillSignUp("Imposter", takenEmail, newUscId(), PASSWORD);
        expectMessage("An account with this email already exists.");

        fillSignUp("Imposter", email("other"), takenId, PASSWORD);
        expectMessage("An account with this USC ID already exists.");
    }

    /**
     * BB-F1-3: Account management. Log in, open Profile, check that the email and USC ID are
     * shown but cannot be edited, change the name, log out, then log back in and check that the
     * new name was kept. A wrong password must be rejected.
     */
    @Test
    public void profile_loginUpdateNameLogout_emailAndIdAreReadOnly() {
        String userEmail = email("profile");
        String userId = newUscId();
        accountService.register("Old Name " + run, userEmail, userId, PASSWORD);
        FirebaseAuth.getInstance().signOut();

        launchLogin();
        login(userEmail, "wrongpass");
        waitForText("Incorrect email or password.");

        login(userEmail, PASSWORD);
        openProfile();
        waitForView(allOf(withId(R.id.et_email), withText(userEmail)));
        onView(withId(R.id.et_usc_id)).check(matches(withText(userId)));
        onView(withId(R.id.et_email)).check(matches(not(isEnabled())));
        onView(withId(R.id.et_usc_id)).check(matches(not(isEnabled())));
        onView(withId(R.id.et_name)).check(matches(withText("Old Name " + run)));

        onView(withId(R.id.et_name)).perform(replaceText("New Name " + run), closeSoftKeyboard());
        waitForView(allOf(withId(R.id.btn_save_name), isEnabled())).perform(click());
        waitForText("Name updated.");

        onView(withId(R.id.btn_logout)).perform(click());
        waitForView(withId(R.id.btn_login));

        login(userEmail, PASSWORD);
        openProfile();
        waitForView(allOf(withId(R.id.et_name), withText("New Name " + run)));
        onView(withId(R.id.et_email)).check(matches(withText(userEmail)));
        onView(withId(R.id.et_usc_id)).check(matches(withText(userId)));
    }

    /**
     * BB-F1-4: Create a group and manage its members. The owner creates a group with a name and
     * description and sees "You are the owner". The group is still listed after the app is
     * reopened. The owner adds a registered user by email, and gets a clear message for an
     * unregistered email and for someone who is already a member.
     */
    @Test
    public void group_createReopenAndAddMembers_withExplanatoryMessages() {
        String friendEmail = email("friend");
        accountService.register("Friend " + run, friendEmail, newUscId(), PASSWORD);
        String ownerEmail = email("owner");
        accountService.register("Owner " + run, ownerEmail, newUscId(), PASSWORD);
        FirebaseAuth.getInstance().signOut();

        launchLogin();
        login(ownerEmail, PASSWORD);
        waitForView(withId(R.id.fab_create)).perform(click());
        String groupName = "Trip " + run;
        onView(withId(R.id.et_name)).perform(replaceText(groupName));
        onView(withId(R.id.et_desc)).perform(replaceText("Spring break"), closeSoftKeyboard());
        onView(withId(R.id.btn_save)).perform(click());
        waitForText(groupName);
        SplitSmartApp app = ApplicationProvider.getApplicationContext();
        String ownerMeta = app.getString(R.string.group_meta_owner,
                app.getResources().getQuantityString(R.plurals.member_count, 1, 1));
        onView(withText(ownerMeta)).check(matches(isDisplayed()));

        // Reopen the app: the user is still logged in and the saved group is still there.
        closeScreens();
        launchLogin();
        waitForText(groupName).perform(click());

        openMembersTab();
        waitForText("Owner " + run);
        addMember(friendEmail);
        waitForText("Friend " + run);

        addMember("nobody" + run + "@usc.edu");
        waitForText("No account was found with this email.");
        onView(withText("Cancel")).perform(click());

        addMember(friendEmail);
        waitForText("This user is already a member of the group.");
    }

    /**
     * BB-F1-5: Group permissions and deletion. A non-owner member can see the group and its
     * member list, and a user can be in more than one group, but the member gets no Add Member,
     * Edit or Delete controls. The owner then renames the group and deletes it, and the group's
     * expense and settlement are deleted with it.
     */
    @Test
    public void group_onlyOwnerCanEditOrDelete_deleteRemovesExpensesAndSettlements() {
        String memberEmail = email("member");
        User member = accountService.register("Member " + run, memberEmail, newUscId(), PASSWORD);
        String ownerEmail = email("boss");
        User owner = accountService.register("Boss " + run, ownerEmail, newUscId(), PASSWORD);
        Group disposable = groupService.createGroup(owner.getUserId(), "Disposable " + run, "");
        Group second = groupService.createGroup(owner.getUserId(), "Second " + run, "");
        groupService.addMemberByEmail(owner.getUserId(), disposable.getGroupId(), memberEmail);
        groupService.addMemberByEmail(owner.getUserId(), second.getGroupId(), memberEmail);
        SplitSmartApp app = ApplicationProvider.getApplicationContext();
        Expense expense = app.getExpenseService().createExpense(owner.getUserId(), new Expense(
                null, disposable.getGroupId(), null, 2000, null, "", "Dinner", Category.FOOD,
                owner.getUserId(), SplitMethod.EQUAL, Arrays.asList(
                        new Share(owner.getUserId(), 1000, 50), new Share(member.getUserId(), 1000, 50))));
        accountService.login(memberEmail, PASSWORD);
        Settlement settlement = app.getSettlementService().createSettlement(member.getUserId(),
                new Settlement(null, disposable.getGroupId(), null, 500, null, "",
                        member.getUserId(), owner.getUserId()));
        FirebaseAuth.getInstance().signOut();

        // Member: sees both groups and the members, but has no owner controls.
        launchLogin();
        login(memberEmail, PASSWORD);
        waitForText("Second " + run);
        waitForText("Disposable " + run).perform(click());
        openMembersTab();
        waitForText("Boss " + run);
        onView(withText("Member " + run)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_add_member)).check(matches(not(isDisplayed())));
        onView(withContentDescription("More options")).check(doesNotExist());
        closeApp();

        // Owner: rename, then delete.
        launchLogin();
        login(ownerEmail, PASSWORD);
        waitForText("Disposable " + run).perform(click());
        chooseMenuItem("Edit");
        waitForView(withId(R.id.et_name)).perform(replaceText("Renamed " + run), closeSoftKeyboard());
        onView(withId(R.id.btn_save)).perform(click());
        waitForText("Renamed " + run);

        chooseMenuItem("Delete");
        waitForText("Delete this group?");
        onView(withId(android.R.id.button1)).perform(click());
        waitForText("Second " + run);
        waitForGone("Renamed " + run);

        assertNull(new FirebaseExpenseRepository().findById(expense.getTransactionId()));
        assertNull(new FirebaseSettlementRepository().findById(settlement.getTransactionId()));
        assertEquals(1, groupService.getGroupsForUser(owner.getUserId()).size());
    }

    // ------------------------------------------------------------------ helpers

    private String email(String prefix) {
        return prefix + run + "@usc.edu";
    }

    private static String newUscId() {
        StringBuilder id = new StringBuilder("9");
        for (int i = 0; i < 9; i++) {
            id.append(RANDOM.nextInt(10));
        }
        return id.toString();
    }

    /**
     * Finishes every open screen and signs out. Screens opened from inside the app (group list,
     * profile, ...) are not tracked by ActivityScenario, so close them by hand; otherwise they
     * linger into the next step and Espresso may look at the wrong window.
     */
    private void closeApp() {
        closeScreens();
        FirebaseAuth.getInstance().signOut();
    }

    /** Finishes every open screen but stays logged in, like swiping the app away. */
    private void closeScreens() {
        if (scenario != null) {
            scenario.close();
            scenario = null;
        }
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

    private void launchLogin() {
        scenario = ActivityScenario.launch(LoginActivity.class);
    }

    private void openSignUp() {
        launchLogin();
        waitForView(withId(R.id.btn_go_to_sign_up)).perform(scrollTo(), click());
        waitForView(withId(R.id.btn_sign_up));
    }

    private static void fillSignUp(String name, String email, String uscId, String password) {
        onView(withId(R.id.et_name)).perform(replaceText(name));
        onView(withId(R.id.et_email)).perform(replaceText(email));
        onView(withId(R.id.et_usc_id)).perform(replaceText(uscId));
        onView(withId(R.id.et_password)).perform(replaceText(password), closeSoftKeyboard());
        onView(withId(R.id.btn_sign_up)).perform(scrollTo(), click());
    }

    private static void login(String email, String password) {
        waitForView(withId(R.id.btn_login));
        onView(withId(R.id.et_email)).perform(replaceText(email));
        onView(withId(R.id.et_password)).perform(replaceText(password), closeSoftKeyboard());
        onView(withId(R.id.btn_login)).perform(click());
    }

    private static void openProfile() {
        chooseMenuItem("Profile");
    }

    /**
     * Opens the toolbar overflow menu and picks an item. Owner-only items appear only after the
     * group loads, so keep reopening the menu until the item shows up.
     */
    private static void chooseMenuItem(String title) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (!isShown(withText(title)) && System.currentTimeMillis() < deadline) {
            if (isShown(withContentDescription("More options"))) {
                onView(withContentDescription("More options")).perform(click());
            }
            pause();
        }
        onView(withText(title)).perform(click());
    }

    private static void openMembersTab() {
        waitForView(allOf(withText("Members"), isDescendantOfA(withId(R.id.tab_layout))))
                .perform(click());
    }

    /** Opens Add Member (retrying while the tab pager settles) and submits the email. */
    private static void addMember(String email) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (!isShown(withId(R.id.til_email)) && System.currentTimeMillis() < deadline) {
            if (isShown(withId(R.id.btn_add_member))) {
                onView(withId(R.id.btn_add_member)).perform(click());
            }
            pause();
        }
        onView(withId(R.id.et_email)).perform(replaceText(email), closeSoftKeyboard());
        onView(withText("Add")).perform(click());
    }

    private static ViewInteraction waitForText(String text) {
        return waitForView(withText(text));
    }

    /**
     * Firebase calls run on background threads Espresso cannot see, so poll for the view.
     * Polls use a silent failure handler: Espresso's default handler dumps the view hierarchy
     * and takes a screenshot on every miss (about 6 s), which is long enough to miss a Snackbar.
     */
    private static ViewInteraction waitForView(Matcher<View> matcher) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (!isShown(matcher) && System.currentTimeMillis() < deadline) {
            pause();
        }
        return onView(matcher).check(matches(isDisplayed()));
    }

    /** Waits for a Snackbar, checks its text (so a wrong message is reported), then waits for it to go. */
    private static void expectMessage(String text) {
        waitForView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(withText(text)));
        waitForGone(text);
    }

    private static void waitForGone(String text) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            boolean[] gone = {true};
            onView(withText(text)).withFailureHandler((error, m) -> gone[0] = false)
                    .check(doesNotExist());
            if (gone[0]) {
                return;
            }
            pause();
        }
        onView(withText(text)).check(doesNotExist());
    }

    /** True if a view matching the matcher is on screen now. Never throws. */
    private static boolean isShown(Matcher<View> matcher) {
        boolean[] shown = {true};
        onView(matcher).withFailureHandler((error, m) -> shown[0] = false)
                .check(matches(isDisplayed()));
        return shown[0];
    }

    private static void pause() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
