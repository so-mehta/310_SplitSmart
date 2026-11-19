# Feature 1: User and Group Management Tests

**Written by:** Rosycela Muniz
**Tests:** 5 black-box (Espresso) and 5 white-box (JUnit 4 + Mockito)

This section is meant to be merged into the team's Project 2.4 testing document.

---

## Instructions: how to execute these tests

Use `gradlew.bat` instead of `gradlew` on Windows. Run the commands from the project root.

**Black-box (Espresso, instrumented):** start an emulator or connect a device first. It needs
Android 13 / API 33 or newer and internet access, because the tests use the real Firebase
backend. Then run:

```
gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.splitsmart.feature1.Feature1BlackBoxTest
```

To run a single test, add `#methodName` after the class name. In Android Studio, you can also
open the file and click the green arrow next to the class or a method. Results are written to
`app/build/reports/androidTests/connected/debug/index.html`.

**White-box (JUnit, local, no device needed):**

```
gradlew :app:testDebugUnitTest --tests "com.example.splitsmart.service.Feature1WhiteBoxTest"
```

Results are written to `app/build/reports/tests/testDebugUnitTest/index.html`.

**Coverage (JaCoCo):**

```
gradlew :app:testDebugUnitTest :app:createDebugUnitTestCoverageReport
```

The report is written to `app/build/reports/coverage/test/debug/index.html`. Coverage is turned
on with `enableUnitTestCoverage = true` in the `debug` build type in `app/build.gradle.kts`.

---

## Black-box testing (Espresso)

**File:** `app/src/androidTest/java/com/example/splitsmart/feature1/Feature1BlackBoxTest.java`
**Tool:** Espresso 3.7 with AndroidX Test (`AndroidJUnit4` runner, `ActivityScenario`)

**How these tests were designed.** They come from the Feature 1 requirements and the demo
script only, not from the source code. Each one drives the real UI (typing, tapping, menus,
dialogs) against the real Firebase backend. It checks only what a user can see: screens,
Snackbar messages, dialog errors and list contents. Inputs were chosen with equivalence
partitioning and boundary-value analysis on the rules in the spec: `@usc.edu`, exactly 10
digits, one account per email and per ID, and owner-only actions.

**Isolation.** Each test makes new accounts with a random email (`<prefix><timestamp>@usc.edu`)
and a random 10-digit USC ID. That means tests never collide with each other or with earlier
runs. Before and after each test, every open screen is closed and the user is signed out.
Some tests prepare state through the app's own services in a few lines, for example by
registering a second user who will be added to a group. Every behavior being checked is
still exercised through the UI.

**Result:** all 5 pass. They were run twice in a row on a Pixel 4 API 33 emulator.

### BB-F1-1: Sign-up rejects invalid email and USC ID formats

- **Location:** `app/src/androidTest/java/com/example/splitsmart/feature1/Feature1BlackBoxTest.java`, test `signUp_invalidEmailOrUscId_showsErrorAndStaysOnSignUp`
- **Description:** Opens Sign Up from the login screen and submits four invalid forms in turn.
  Each one must show the correct Snackbar message and leave the user on the sign-up screen:
  1. a `@gmail.com` email → "Enter a valid USC email address."
  2. a 9-digit ID → "USC ID must be exactly 10 digits."
  3. an 11-digit ID → same message
  4. the 10-character ID `12345678a0` → same message
- **Execution:** run the class as described above, or append `#signUp_invalidEmailOrUscId_showsErrorAndStaysOnSignUp`.
- **Rationale:** The spec has two format rules, so there is one invalid email class (wrong
  domain) and three invalid ID classes. 9 and 11 digits are the boundaries on either side of
  the valid length of 10. `12345678a0` has the correct length but includes a non-digit, so
  only the "numerical" rule is being tested. Every other field is valid, so each attempt
  breaks exactly one rule.
- **Bug found: yes (Bug F1-1, fixed).** See [Bugs found and fixed](#bugs-found-and-fixed).
  The first run failed at the 11-digit case: the app *created the account* and opened the
  group list instead of showing an error.

### BB-F1-2: Duplicate USC email or USC ID is rejected

- **Location:** same file, test `signUp_duplicateEmailOrUscId_isRejected`
- **Description:** Creates an account through the UI and signs out. Then it tries to sign up
  (a) with the same email and a new ID, which must show "An account with this email already
  exists.", and (b) with a new email and the same ID, which must show "An account with this
  USC ID already exists."
- **Rationale:** The spec says each USC email and each USC ID can belong to only one account.
  These are two separate rules, so each is tested on its own by changing only the other
  field. The original account is created through the UI so the whole sign-up path is used.
- **Bug found:** no.

### BB-F1-3: Log in, view and update the name, log out; email and USC ID are read-only

- **Location:** same file, test `profile_loginUpdateNameLogout_emailAndIdAreReadOnly`
- **Description:** Logs in with a wrong password ("Incorrect email or password." is shown),
  then with the correct one. It opens Profile and checks the email and USC ID values. It also
  checks that those two fields are **disabled**, so they cannot be edited. Then it changes the
  name, sees "Name updated.", and logs out, which returns to the login screen. Finally it logs
  back in and checks that the new name was kept and the email and ID are unchanged.
- **Rationale:** This covers every account action in the spec: log in, log out, view the name,
  update the name, and "email and USC ID must not be edited." The wrong-password attempt is
  the invalid class for login. Logging out and back in shows the name change was saved to the
  backend, not just shown on screen.
- **Bug found:** no.

### BB-F1-4: Create a group, reopen the app, add members, see explanatory messages

- **Location:** same file, test `group_createReopenAndAddMembers_withExplanatoryMessages`
- **Description:** The owner creates a group with a name and description. The group list
  shows it with "1 member · You are the owner". The test closes all screens without logging
  out, then relaunches the app, and the group is still listed. In the Members tab the owner
  adds a registered user by email, and that user appears. Adding an unregistered email shows
  "No account was found with this email." Adding the same user again shows "This user is
  already a member of the group."
- **Rationale:** This covers "creator becomes the owner", "saved group remains available after
  reopening" and "add registered users by USC email". The add-member input space has three
  partitions: a registered non-member (valid), an unregistered email (invalid), and an
  existing member (invalid). One test is used per partition.
- **Bug found:** no.

### BB-F1-5: Only the owner can edit or delete; deleting removes expenses and settlements

- **Location:** same file, test `group_onlyOwnerCanEditOrDelete_deleteRemovesExpensesAndSettlements`
- **Description:** Setup: the owner creates two groups and adds a member to both. The owner
  records a $20 expense and the member records a $5 settlement in the "Disposable" group.

  The test then logs in as the member, who sees **both** groups (a user can be in several
  groups). The member opens Disposable and can see the member list, but there is no Add Member
  button and no Edit/Delete menu.

  Next it logs in as the owner, who renames the group through Edit and then deletes it
  (confirming the dialog). The group disappears from the list while the other group stays.
  Finally the test checks that the expense and the settlement documents no longer exist.
- **Rationale:** This covers the permission rules from the spec (only the owner edits or
  deletes, and members can view) and the cascade rule (deleting a group removes its expenses
  and settlements). The second group is the control: deleting one group must not touch the
  other. Both an expense and a settlement are created so that both parts of the cascade are
  checked.
- **Bug found:** no.

---

## White-box testing (JUnit)

**File:** `app/src/test/java/com/example/splitsmart/service/Feature1WhiteBoxTest.java`
**Tool:** JUnit 4.13 for local unit tests, Mockito 5 for the repositories, JaCoCo for coverage

**How these tests were designed.** They were written by reading the source of
`AccountService`, `GroupService`, `InputValidator` and `Group`. Each test targets specific
branches or paths: the order of the checks, early exits, short-circuit conditions and null
checks. The repositories are Mockito mocks, so each test can choose which branch a lookup
takes and confirm which repository calls each path makes.

**Execution:** `gradlew :app:testDebugUnitTest --tests "com.example.splitsmart.service.Feature1WhiteBoxTest"`,
or run the file from Android Studio.

### WB-F1-1: The checks in `register()` run in order, with the exact messages

- **Location:** `app/src/test/java/com/example/splitsmart/service/Feature1WhiteBoxTest.java`, test `wb1_register_guardChainFiresInOrderWithExactMessages`
- **Description:** `register()` checks name → email → USC ID → password, in that order. The test
  starts with all four fields invalid and fixes one per call. Each call therefore takes the
  "true" branch of the next check. It asserts the exact message each time and that the
  repository is never touched (`verifyNoInteractions`).
- **Coverage target:** path coverage of the validation sequence, and the "true" branch of every check.
- **Result:** pass.
- **Bug found:** no.

### WB-F1-2: The duplicate-email check exits before the USC ID lookup

- **Location:** same file, test `wb2_register_duplicateEmailShortCircuitsBeforeUscIdLookup`
- **Description:** With both the email and the ID already taken, `register()` must report the
  duplicate email and return before `findByUscId()` is called. With only the ID taken, it must
  call `findByEmail()` and then `findByUscId()`, in that order (`InOrder`), and report the
  duplicate ID. In neither case is an auth account created.
- **Coverage target:** both outcomes of each duplicate check, plus the early-return path.
- **Result:** pass.
- **Bug found:** no.

### WB-F1-3: `updateName()` changes only the name

- **Location:** same file, test `wb3_updateName_onlyMutatesNameAndPersistsOriginalEmailAndUscId`
- **Description:** Captures the `User` passed to `update()`. It checks that the name is trimmed
  and changed while the ID, email and USC ID keep their stored values. It also checks that only
  `findById` and `update` are called (`verifyNoMoreInteractions`).
- **Coverage target:** statement coverage of the success path of `updateName()`. This is how the
  code enforces the rule that the email and USC ID can't be edited after registration.
- **Result:** pass.
- **Bug found:** no.

### WB-F1-4: Both outcomes of each check in `addMemberByEmail()`, and `Group` null checks

- **Location:** same file, test `wb4_addMemberByEmail_coversUnknownEmailAlreadyMemberAndSuccessBranches`
- **Description:** Takes each outcome of `user == null` and `group.isMember(...)`:
  - unknown email → "No account was found with this email."
  - existing member → "This user is already a member of the group."
  - new user → `update()` is called exactly once, with the new member appended

  It also covers the null branches of the `Group` constructor (`memberIds == null` → empty
  list) and of `isMember()` (`memberIds == null` → false).
- **Coverage target:** branch coverage of `addMemberByEmail()` and of `Group.isMember()` and the `Group` constructor.
- **Result:** pass.
- **Bug found:** no.

### WB-F1-5: The ownership check controls the delete cascade

- **Location:** same file, test `wb5_deleteGroup_ownershipConditionBranchesGateTheCascade`
- **Description:** `Group.isOwner()` is `ownerId != null && ownerId.equals(userId)`. The test
  covers three cases:
  - a group with a null owner (the first condition is false, even with a null caller)
  - a member who isn't the owner (the second condition is false)
  - the real owner (both conditions true)

  Only the last case may reach the cascade. It must delete expenses → settlements → group, in
  that order (`InOrder`). The first two cases must throw `PermissionException`, with no
  interaction with the expense or settlement repositories.
- **Coverage target:** condition coverage of both sides of the `&&` in `isOwner()`, plus the
  permission path in `deleteGroup()`.
- **Result:** pass.
- **Bug found:** no.

### Coverage criteria and level achieved

**Criteria.** Branch (decision) coverage is the main criterion, with line and instruction
coverage reported alongside it. Tests WB-F1-1 and WB-F1-5 also aim for path coverage of the
validation sequence and condition coverage of `isOwner()`.

**Measured with JaCoCo**, using
`gradlew :app:testDebugUnitTest :app:createDebugUnitTestCoverageReport`. The table covers the
Feature 1 logic classes.

| Class | Branch | Line | Instruction |
|---|---|---|---|
| `service/AccountService` | 34/34 (100%) | 55/55 (100%) | 211/211 (100%) |
| `service/GroupService` | 18/18 (100%) | 56/56 (100%) | 216/216 (100%) |
| `service/InputValidator` | 10/10 (100%) | 7/7 (100%) | 41/41 (100%) |
| `model/Group` | 10/10 (100%) | 22/27 (81.5%) | 80/91 (87.9%) |
| **Total** | **72/72 (100%)** | **140/145 (96.6%)** | **548/559 (98.0%)** |

These numbers are for all unit tests in the project that exercise Feature 1:
`Feature1WhiteBoxTest` plus the existing `AccountServiceTest`, `GroupServiceTest` and
`InputValidatorTest`. The five `Feature1WhiteBoxTest` tests alone reach 38/72 branches (52.8%).
They were chosen to target specific paths and exact messages that the existing tests did not
assert, not to repeat what was already covered.

The only lines not covered are 5 getters and setters in `Group` that the services never call.
They contain no branches.

The Android UI classes (Activities and ViewModels) are not counted in this local unit-test
coverage. They are tested by the Espresso black-box tests above.

---

## Bugs found and fixed

### Bug F1-1: The sign-up USC ID field silently changed what the user entered

- **Found by:** BB-F1-1 (`signUp_invalidEmailOrUscId_showsErrorAndStaysOnSignUp`). On the first
  run, the 11-digit case did not show an error. The app created an account and opened the
  group list.
- **What the bug was:** In `app/src/main/res/layout/activity_sign_up.xml`, the USC ID field had
  `android:maxLength="10"` and `android:inputType="number"`. Both change the text *before* the
  app's validation sees it. Typing looked fine, because the field simply stopped accepting an
  11th digit or a letter. Pasting was the problem:
  - `maxLength` cut a pasted 11-digit ID down to its first 10 digits. The account was created
    with a USC ID the user never entered.
  - `inputType="number"` installs a digits-only filter, so letters in a pasted ID were
    silently dropped.

  Either way, the user never saw "USC ID must be exactly 10 digits." for what they actually
  entered.
- **Fixed:** yes.
  - In `activity_sign_up.xml`: removed `android:maxLength="10"` and changed the field's
    `inputType` to `textNoSuggestions`, so nothing filters the input.
  - In `app/src/main/java/com/example/splitsmart/ui/auth/SignUpActivity.java`: added
    `uscIdInput.setRawInputType(InputType.TYPE_CLASS_NUMBER)` in `onCreate`. This keeps the
    numeric keypad without the digits filter, so whatever is typed or pasted reaches
    `InputValidator.isValidUscId()` and gets the proper error message.
- **Verified:** BB-F1-1 now passes for all four invalid inputs, and the valid sign-up paths in
  BB-F1-2 still pass.
