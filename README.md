# FinTrack

## Included
- Kotlin + Jetpack Compose + Material 3 foundation
- Dark navy and emerald theme
- Three-step onboarding with animated progress
- Animated navigation transitions
- Offline SQLite persistence for accounts and financial records
- Dashboard with balance, income/expense summary, quick actions and empty states
- Account, transaction, budget, savings goal and loan entry forms
- Reports for income, expenses, net cash flow and current balance
- Bottom navigation: Home, Accounts, Transactions, Reports, More
- Placeholder destinations for later milestones
- No login, cloud sync, INTERNET permission, or demo finance records

## Open
1. Extract this ZIP.
2. Open the `FinTrackFree` folder in Android Studio.
3. Use JDK 17 and install Android SDK Platform 35.
4. Allow Gradle sync to download dependencies.
5. Run on an API 23+ emulator or Android device.

## Build
Run `gradle :app:assembleDebug` from the project root, or use Android Studio's Build APK action.
Expected output: `app/build/outputs/apk/debug/app-debug.apk`.

## Scope
FinTrack is an offline personal finance tracker. It does not require login, cloud sync,
or the `INTERNET` permission. Records are stored locally in the app's SQLite database.

## Verification
The debug APK builds with `./gradlew assembleDebug` and is written to
`app/build/outputs/apk/debug/app-debug.apk`.
