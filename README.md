# FinTrack

**Version 2.3.0**

FinTrack is an offline-first Android personal finance tracker.

## 2.3.0 update

- Set the Android application version to 2.3.0 (version code 23).
- Retained the existing FinTrack app identity, application ID and launcher icon.
- Retained the current dark liquid-glass visual system and responsive layouts.
- Retained edge-to-edge safe-area handling for system bars, display cutouts, navigation modes and the keyboard.
- Retained offline local storage, backup export and validated restore.
- Retained accounts, transactions, people, budgets, savings goals, loans, reports and settings.

## Features

- Dashboard with balance, income/expense summary, quick stats, accounts and recent transactions.
- Accounts and wallets with account management and balance actions.
- Income, expense, transfer, borrowing and lending transactions.
- Categories and subcategories.
- People and lending/borrowing history.
- Budgets and category spending progress.
- Savings goals and contribution tracking.
- Loans, EMI and repayment tracking.
- Reports with period filters, category breakdowns and charts.
- Settings, privacy information and local data management.
- JSON backup export and validated restore using Android document-picker integration.

## Privacy and storage

- No mandatory sign-in, cloud synchronization, ads or analytics.
- The Android manifest does not request the INTERNET permission.
- Primary financial records are stored locally on the device.
- Backup files are user-controlled. Store exported backups safely.
- Clearing app storage or uninstalling can remove local app data.

## Build

Use JDK 17 and Android SDK Platform 35.

```bash
./gradlew --no-daemon assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions builds the debug APK on pushes to `main` and uploads an artifact named `FinTrack-debug-apk`.

## Application identity

- App name: FinTrack
- Application ID: `com.charan.fintrack`
- Version name: `2.3.0`
- Version code: `23`
- Minimum Android SDK: 23
- Target Android SDK: 35
