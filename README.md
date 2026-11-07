# FinTrack

**Version 2.4.2**

FinTrack is an offline-first Android personal finance tracker.

## 2.4.2 transaction export update

- Added custom inclusive start/end date filtering to Reports.
- Added local PDF statement export using Android's Save As document picker.
- Added editable DOCX statement export using a minimal Office Open XML document package.
- Added financial summary, current account balance snapshot, monthly income/expense chart, category chart, account flow breakdown, category totals and complete transaction ledger.
- Exported data is a read-only snapshot of the selected date range; report generation does not alter financial records.
- Added Android instrumentation tests for PDF signature and DOCX package contents.
- Corrected local calendar date generation to avoid UTC date rollover in India and other positive-offset time zones.

## 2.4.1 developer review update

- Hardened WebView navigation so only bundled app assets run inside the WebView; external links open outside the app.
- Added native Android JSON backup import/export using the system document picker.
- Preserved unreadable local data in a recovery copy before initializing a clean state.
- Fixed computed total balance across multiple active accounts in the Room DAO.
- Added automated reminder scheduling tests and Room instrumentation coverage.
- Improved notification action routing, status icon, permission feedback, recurring work handling and local-time rescheduling.
- Added CI checks for embedded JavaScript syntax, JVM unit tests, Android emulator instrumentation tests and debug APK assembly.
- Tightened top and bottom safe-area handling and improved bottom-sheet keyboard behaviour.

## Features

- Dashboard, accounts, wallets and recent transactions.
- Income, expense, transfer, borrowing and lending records.
- Categories and subcategories.
- People and lending/borrowing history.
- Budgets and savings goals.
- Loans, EMI and repayments.
- Reports with period filters, breakdowns and charts.
- PDF and editable DOCX transaction statements with custom date ranges.
- Smart reminders and Meme Mode.
- Settings, privacy information, JSON backup and validated restore.

## Privacy and storage

- No mandatory sign-in, cloud synchronization, ads or analytics.
- The Android manifest does not request the INTERNET permission.
- Financial records are stored locally on the device.
- Transaction exports are generated locally and saved only to a user-selected destination.
- Exported backup and report files are user-controlled. Store them safely.
- Clearing app storage or uninstalling can remove local data.

## Build

Use JDK 17 and Android SDK Platform 35.

```bash
./gradlew --no-daemon testDebugUnitTest assembleDebug
./gradlew --no-daemon connectedDebugAndroidTest
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions uploads the build as `FinTrack-debug-apk`.

## Application identity

- App name: FinTrack
- Application ID: `com.charan.fintrack`
- Version name: `2.4.2`
- Version code: `26`
- Minimum Android SDK: 23
- Target Android SDK: 35
