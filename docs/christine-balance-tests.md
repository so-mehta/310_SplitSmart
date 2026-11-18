# Feature 4 — Balance Calculation and Display

Contributor: Christine Jeong

## Running the tests

- JUnit: In Android Studio, run `BalanceServiceTest` under `app/src/test`, or run `bash gradlew :app:testDebugUnitTest --tests '*BalanceServiceTest'` from the project root. The suite uses mocked repositories and runs without Firebase.
- Espresso: Start an Android emulator, run SplitSmart, and sign in as Christine. Prepare the two QA groups below. Run `ChristineBalanceUiTest` under `app/src/androidTest` with the emulator selected, or run `bash gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.splitsmart.ChristineBalanceUiTest`. Keep the QA group names unique and visible in the group list. The tests read the prepared Firebase records.

## Espresso test data

Create **QA Balances Active** and **QA Balances Empty** with Christine, Alex, and Simra in each. Leave History empty in **QA Balances Empty**. Add the following to **QA Balances Active** and use an Equal split with only the listed participants. Leave settlement history empty.

| Expense | Total | Paid by | Participants |
|---|---:|---|---|
| Dinner | $20.00 | Alex | Christine and Alex |
| Parking | $10.00 | Alex | Christine and Alex |
| Coffee | $8.00 | Christine | Christine and Alex |
| Lunch | $8.00 | Christine | Christine and Simra |

## White-box cases

File: `app/src/test/java/com/example/splitsmart/service/BalanceServiceTest.java`

| Method | Setup and expected result | Rationale | Actual result |
|---|---|---|---|
| `christine_samePairExpensesAccumulateAndOffset` | Apply $10 and $5 shares in one direction and $4 in reverse. Expect opposite signed balances of $11. | Exercises accumulation and offsetting for the same pair. | Pass |
| `christine_chainOfDebtsDoesNotCreateThirdDebt` | Seed two $10 debts through the middle member. Expect $10 payable, $10 receivable, and zero between the endpoints. | Detects netting across unrelated pairs. | Pass |
| `christine_settlementEditAndDeletionRecomputeBalance` | Start at $11 owed, then seed a $5 settlement, replace it with $8, and remove it. Expect $6, $3, then $11 owed. | Checks recalculation against each version of the repository records. | Pass |
| `christine_editAfterFullSettlementReopensDebt` | Fully pay a $10 share, then increase it to $12. Expect $2 owed. | Checks the transition from zero after an expense edit. | Pass |
| `christine_deletingExpenseAfterPaymentReversesBalance` | Fully pay a $10 share, then remove the expense while the payment remains. Expect $10 receivable. | Checks the sign change and classification in the summary. | Pass |

Direct JUnit execution passed all 16 tests in the suite, including these five. JaCoCo measured **49/51 lines (96.1%)** and **32/44 branches (72.7%)** in `BalanceService` for the full suite. No defects surfaced in this run.

## Black-box cases

File: `app/src/androidTest/java/com/example/splitsmart/ChristineBalanceUiTest.java`

| Method | Action and expected UI | Rationale | Actual result |
|---|---|---|---|
| `showsAlexNetPayable` | Open Active → Balances and see “You owe Alex $11.00.” | Verifies that the displayed amount and direction reflect the offset. | Pass |
| `showsSimraReceivable` | Open Active → Balances and see “Simra owes you $4.00.” | Checks the receivable label, member name, and amount. | Pass |
| `keepsPayableAndReceivableCardsSeparate` | Open Active → Balances and see $11 owed and $4 receivable in separate cards. | Detects swapped or netted totals. | Pass |
| `sameMembersInAnotherGroupHaveZeroBalances` | Open Empty → Balances and see both members settled and both totals at zero. | Checks group isolation with identical members. | Pass |
| `switchingGroupsRestoresTheActiveGroupsBalances` | Open Empty → Balances, go back, then open Active → Balances. Expect the $11 payable and $4 receivable again. | Detects stale balances after a group switch. | Pass |

All five Espresso tests passed on Medium Phone API 36.0 on November 16, 2026. The Android test runner recorded zero failures and zero ignored tests.
