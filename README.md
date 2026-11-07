# FinTrack

**Version 2.4.1**

FinTrack is an offline-first Android personal finance tracker.

## 2.4.1 developer review update

- Hardened WebView navigation so only bundled app assets run inside the WebView; external links open outside the app.
- Added native Android JSON backup import/export using the system document picker.
- Preserved unreadable local data in a recovery copy before initializing a clean state.
- Fixed computed total balance across multiple active accounts in the Room DAO.
- Added automated reminder scheduling tests and Room instrumentation coverage.
- Improved notification action routing, status icon, permission feedback, recurring work handling and local-time rescheduling.
- Added CI checks for embedded JavaScript syntax, JVM unit tests, Android emulator instrumentation tests and debug APK assembly.

## 2.4.1 update

- Updated the Android version to 2.4.1 (version code 25).
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
- Version name: `2.4.1`
- Version code: `25`
- Minimum Android SDK: 23
- Target Android SDK: 35
