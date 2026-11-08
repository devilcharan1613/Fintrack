# FinTrack

**Version 2.5.0 · Offline edition**

FinTrack is a personal finance tracker designed to work without an internet connection. It does not require sign-in, OTP, cloud storage, a subscription, ads, or analytics.

## Finance features

- Dashboard with total balance, income, expenses, borrowed/lent amounts, net worth and recent activity.
- Multiple cash, bank, card and wallet accounts.
- Income, expenses, transfers, borrowing and lending.
- Categories, subcategories, people, notes, date/time and payment method details.
- Transaction search, filters, monthly history and income/expense summaries.
- Budgets with weekly, monthly and yearly periods.
- Savings goals with contributions.
- Loans, EMI values, repayment records and repayment history.
- Reports with period filters, category breakdowns and charts.

## v2.5.0 offline updates

- Refreshed luminous teal glass UI across the existing screens.
- Local JSON backup export through Android's Save dialog.
- JSON backup restore through Android's file picker, with validation before replacing existing data.
- Multi-sheet Excel workbook (.xlsx) export for accounts, transactions, people, budgets, goals, loans and categories.
- CSV and printable PDF transaction statements.
- Optional daily reminders scheduled on the device, with a fun-message setting and Add Expense / Add Income notification actions.
- Reminder schedule is restored after device restart. Android notification permission is required on Android 13 and newer.
- Manual install/update flow for an APK already stored on the phone. Android's package installer and user approval are required.

## Offline and privacy guarantees

- The Android manifest does not request android.permission.INTERNET.
- The app does not sign in, send financial records to a server, or synchronize data to a cloud account.
- Finance records and reminder preferences stay on the device.
- Backup and export files are created only after the user selects a destination in Android's file picker.
- Reminders use Android's local alarm and notification services. Delivery time may be adjusted by Android battery and background policies.
- APK updates are selected manually from local storage. FinTrack does not download updates.
- Clearing app storage or uninstalling the app can remove its local data. Keep a separate backup in a safe place.

## Build and tests

Use JDK 17 and Android SDK Platform 35.

Build command: ./gradlew --no-daemon testDebugUnitTest assembleDebug

APK output: app/build/outputs/apk/debug/app-debug.apk.

GitHub Actions uploads the debug APK as the FinTrack-debug-apk artifact. The CI workflow runs unit tests and assembles the APK. The XLSX package test is an instrumented test and must be run on an Android device or emulator.

## Application identity

- App name: FinTrack
- Application ID: com.charan.fintrack
- Version name: 2.5.0
- Version code: 27
- Minimum Android SDK: 23
- Target Android SDK: 35