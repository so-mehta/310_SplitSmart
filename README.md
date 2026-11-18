# SplitSmart

SplitSmart is an Android app that helps USC students keep track of shared expenses. Roommates, trip groups and clubs log what they spend, split each cost fairly, and see exactly who owes whom. No money moves through the app; it is a shared record.

Built by Team 6: The Piggly Wigglys, for CSCI 310.



## Features

| Feature | What you can do |
|---|---|
| Accounts and groups | Sign up with a USC email and USC ID, create groups, add members by email |
| Expenses | Add, view, edit and delete expenses. Only the creator can edit or delete |
| Splitting | Split equally, by exact amounts, or by percentages, with a live preview of each share |
| Balances | See what you owe, what you are owed, and your balance with each member |
| History and summary | Search, filter and sort every transaction, record settlements, and view monthly totals by category |

## Running the app in Android Studio

**You need** a recent Android Studio that supports Android Gradle Plugin 9.3, and an emulator or phone running Android 13 (API 33) or later. Gradle downloads the JDK it needs on the first build.

1. **Open the project.** In Android Studio choose *File > Open* and select the
   `310_SplitSmart` folder.
2. **Let Gradle sync.** The first sync downloads dependencies and needs an internet
   connection. Wait for it to finish.
3. **Choose a device.** In *Device Manager*, start an emulator with API 33 or later, or
   connect a phone with USB debugging turned on.
4. **Run.** Select the `app` configuration and press *Run*.

The app opens on the login screen. Tap *New here? Create an account* to sign up. The email must end in `@usc.edu`, the USC ID must be 10 digits, and the password needs at least 6 characters. Sign-up sends no email, so a test address such as `test1@usc.edu` works.

To try splitting and balances, create a second account, add it to a group by email, and switch between the two with *Profile > Log out*.

## How it works

The app follows a four-layer architecture. Each layer calls only the layer directly below it.

```
Views (Activities, Fragments)      draw the screen and pass on user actions
  ViewModels                       run service calls off the main thread
Services                           hold every business rule
Repositories                       hide Firebase behind Java interfaces
Firebase Auth and Cloud Firestore  store accounts and data
```

Code lives under `app/src/main/java/com/example/splitsmart/`:

| Package | Contents |
|---|---|
| `model` | Plain data classes: `User`, `Group`, `Expense`, `Settlement`, `Share` |
| `service` | Business rules, such as `ExpenseService` and `SplitCalculator` |
| `repository` | Repository interfaces the services depend on |
| `repository.firebase` | Firestore and Firebase Auth implementations |
| `ui` | One package per area: `auth`, `group`, `expense`, `balance`, `history`, `common` |
| `SplitSmartApp` | Creates every repository and service once, at startup |

Three conventions hold everywhere:

- **Money is stored in whole cents** (`long`), so shares always add up exactly. Only the
  UI formats amounts as `$12.50`.
- **Services check before they write.** A failed check throws `ValidationException` or
  `PermissionException`, nothing is saved, and the screen shows the message.
- **Security is enforced twice**: once in the services, and again on the server by the
  Firestore rules in [`firestore.rules`](firestore.rules).

## Firebase

The app connects to the team's Firebase project (`splitsmart-a42e9`) through `app/google-services.json`, so no setup is needed to run it. It uses email and password sign-in and Cloud Firestore. Everything you create while testing is saved to that shared project.

## Piggly Wiggly Team <img src="docs/piggly.svg" height="28" alt="(՞•Ꙫ•՞)">


| Name | Email |
|---|---|
| Simra Zargar | szargar@usc.edu |
| Alex Reyes Aranda | reyesara@usc.edu |
| Rosycela Muniz | rosycela@usc.edu |
| Christine Jeong | jeongc@usc.edu |
| Somya Mehta | somyameh@usc.edu |
