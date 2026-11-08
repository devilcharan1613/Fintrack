# FinTrack

**Version 2.5.0**

FinTrack is an offline-first Android personal finance tracker.

## 2.5.0 update

- Updated the Android version to 2.5.0 (version code 27).
- Tightened the top safe-area spacing so the toolbar clears the display notch without a large empty band.
- Corrected bottom navigation inset ownership to prevent the Android navigation inset being counted twice.
- Reduced excessive bottom scroll padding while keeping screen content clear of the app navigation bar.
- Improved bottom-sheet height and safe-area handling.
- Reset sheet scroll and stale keyboard inset state when a form is cancelled or reopened.
- Blur a previously focused field during modal open/close to prevent the keyboard from restoring an old sheet position.
- Kept the FinTrack identity, logo, application ID, offline storage, and finance modules.

## Features

- Dashboard, accounts, wallets and recent transactions.
- Income, expense, transfer, borrowing and lending records.
- Categories and subcategories.
- People and lending/borrowing history.
- Budgets and savings goals.
- Loans, EMI and repayments.
- Reports with period filters, breakdowns and charts.
- Settings, privacy information, JSON backup and validated restore.

## Privacy and storage

- No mandatory sign-in, cloud synchronization, ads or analytics.
- The Android manifest does not request the INTERNET permission.
- Financial records are stored locally on the device.
- Exported backup files are user-controlled. Store them safely.
- Clearing app storage or uninstalling can remove local data.

## Build

Use JDK 17 and Android SDK Platform 35.

```bash
./gradlew --no-daemon assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions uploads the build as `FinTrack-debug-apk`.

## Application identity

- App name: FinTrack
- Application ID: `com.charan.fintrack`
- Version name: `2.5.0`
- Version code: `27`
- Minimum Android SDK: 23
- Target Android SDK: 35
