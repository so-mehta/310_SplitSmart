# 310_SplitSmart

SplitSmart is an Android app for USC students and alumni to share and split group expenses.
It uses Firebase Authentication and Cloud Firestore as its backend.

## Requirements

- Android Studio (recent stable release) with the Android SDK for API 37 installed
- JDK 11 or newer (the one bundled with Android Studio works)
- An emulator or device running Android 13 (API 33) or newer, with internet access
  (the app talks to the live Firebase project configured in `app/google-services.json`)

## Running the app

1. Unzip the project and open the `SplitSmart` folder in Android Studio (File > Open).
2. Wait for the Gradle sync to finish.
3. Start an emulator (Device Manager > create/start a Pixel device with API 33+), or plug in a
   device with USB debugging on.
4. Select the `app` run configuration and press Run. The login screen opens.
5. Tap "New here? Create an account" and register with a name, an `@usc.edu` email, a 10-digit
   USC ID and a password of at least 6 characters.

From the command line instead: `gradlew installDebug` (Windows: `gradlew.bat installDebug`) with
an emulator running, then open SplitSmart on the device.

## Running the tests

| What | Command (use `gradlew.bat` on Windows) | Needs a device? |
|---|---|---|
| All local unit tests (JUnit + Mockito, white-box) | `gradlew :app:testDebugUnitTest` | No |
| All instrumented tests (Espresso, black-box) | `gradlew :app:connectedDebugAndroidTest` | Yes, with internet |
| Unit-test coverage report (JaCoCo) | `gradlew :app:testDebugUnitTest :app:createDebugUnitTestCoverageReport` | No |

To run one test class, add a filter:

- Unit: `gradlew :app:testDebugUnitTest --tests "com.example.splitsmart.service.Feature1WhiteBoxTest"`
- Espresso: `gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.splitsmart.feature1.Feature1BlackBoxTest`

In Android Studio you can also right-click a test file or test method and choose Run.

Reports:

- Unit test results: `app/build/reports/tests/testDebugUnitTest/index.html`
- Espresso results: `app/build/reports/androidTests/connected/debug/index.html`
- Coverage: `app/build/reports/coverage/test/debug/index.html`

Test locations:

- White-box (JUnit): `app/src/test/java/com/example/splitsmart/...`
- Black-box (Espresso): `app/src/androidTest/java/com/example/splitsmart/...`

The Espresso tests create fresh accounts with random emails and USC IDs on every run, so they
can be run repeatedly against the same Firebase project.
