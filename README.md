# FinTrack

**Version 2.5.0 · Offline edition · Android**

FinTrack is an offline-first personal finance tracker. This README documents features present in the development-branch source and distinguishes implementation from runtime verification. A feature being present in source code does not mean it has passed device testing.

## Feature status legend

- **Implemented (source):** corresponding UI or code is present in this branch.
- **Partial:** a basic implementation exists, but completeness or all expected cases are not verified.
- **Needs testing:** implementation exists, but device/emulator or regression testing is still required.
- **Not claimed:** no complete implementation or successful test has been confirmed.

## 1. Dashboard

- **Implemented (source):** dashboard summaries for balances, income, expenses and recent activity.
- **Implemented (source):** borrowed/lent and net-worth summary areas.
- **Needs testing:** verify every dashboard figure against saved accounts, transactions and outstanding balances.

## 2. Accounts and wallets

- **Implemented (source):** multiple account records and account balance display.
- **Implemented (source):** account-linked transaction views.
- **Needs testing:** opening/current balance calculations, transfers, edits and deletion edge cases.

## 3. Transactions

- **Implemented (source):** income and expense records.
- **Implemented (source):** transfer, borrowing and lending transaction types.
- **Implemented (source):** date/time, amount, category, account, payment method, UPI app and notes fields where applicable.
- **Implemented (source):** transaction search and type/account/date filtering controls.
- **Implemented (source):** custom From/To date controls in transaction and report views.
- **Needs testing:** end-to-end add/edit/delete flows, transfer pairing, filter boundaries and totals after edits.

## 4. Reports and analytics

- **Implemented (source):** income-versus-expense and category chart areas.
- **Implemented (source):** account balances and person summaries.
- **Implemented (source):** date-range controls and transaction statement export entry points.
- **Needs testing:** chart aggregation, date inclusivity, empty periods and reconciliation with the transaction list.

## 5. Budgets

- **Implemented (source):** budget records and budget display/progress UI.
- **Partial:** budget periods and spending calculations require broader validation.
- **Needs testing:** create/edit/delete, period rollover, category matching and over-budget cases.

## 6. Savings goals

- **Implemented (source):** savings goal records and contribution flow.
- **Needs testing:** contribution history, target/remaining calculations, editing and deletion.

## 7. Borrowing, lending and loans

- **Implemented (source):** person-linked borrowing/lending records.
- **Implemented (source):** loan records and repayment-history data structures.
- **Needs testing:** repayment balance calculations, partial/full settlement, overpayment prevention and person-wise totals.

## 8. People

- **Implemented (source):** people records and person summary views.
- **Needs testing:** contact validation, linked transaction history and settlement reconciliation.

## 9. Exports and backup

- **Implemented (source):** JSON backup export and restore through Android file selection.
- **Implemented (source):** restore validation before replacing current local data.
- **Implemented (source):** CSV transaction export.
- **Implemented (source):** multi-sheet XLSX workbook export, including a summary sheet and finance-data sheets.
- **Implemented (source):** custom date-range XLSX export entry point.
- **Implemented (source):** PDF transaction statement through Android's print flow (Print to PDF is available in Android's print UI).
- **Needs testing:** open generated XLSX files in spreadsheet applications; verify custom-range contents, CSV encoding, PDF page layout and backup compatibility with older app data.
- **Important:** Android Print to PDF depends on the print service available on the device. FinTrack does not claim a separate direct PDF-file generator.

### Export file layouts

**Excel (.xlsx)**

The workbook is designed with separate worksheets for Summary, Accounts, Transactions, People, Budgets, Savings Goals, Loans, Loan Payments and Categories. The Summary sheet contains export metadata and income/expense totals. Column availability depends on the fields stored in the corresponding records.

**Custom-range Excel**

The selected date range is intended to limit the Transactions worksheet and identify the period in the export summary. Other reference sheets provide context for the exported transactions. Verify the output before relying on it as a formal statement.

**CSV (.csv)**

A transaction-row export with fields such as date, type, description/notes, category, account, payment method and amount (INR).

**PDF via Print**

A print-friendly transaction statement with selected period, generated date, income/expense/net summary and a transaction table. Choose **Save as PDF** in the Android print dialog when that destination is offered.

**JSON backup (.json)**

A local backup intended to preserve the app's finance data for later restore. Keep the exported file somewhere safe outside the app's private storage.

## 10. Reminders and notifications

- **Implemented (source):** optional daily reminder scheduling using Android local alarm/notification APIs.
- **Implemented (source):** reminder time and fun-message preference controls.
- **Implemented (source):** notification actions for adding income or expenses.
- **Implemented (source):** boot receiver intended to restore reminder scheduling after restart.
- **Needs testing:** Android 13+ notification permission flow, denied/revoked permission, reboot, Doze/battery restrictions, notification action routing and delivery on supported Android versions.
- **Limitation:** Android may delay or restrict background notifications depending on device settings and battery policies. Reminders do not require internet.

## 11. Themes and appearance

- **Implemented (source):** dark/light theme preference and shared Liquid Glass styling layer.
- **Implemented (source):** custom background photo selection and saved visibility preference controls.
- **Needs testing:** consistency of every screen in both themes, photo persistence after restart, slider behaviour from 0–100%, contrast/readability and performance on lower-memory devices.

## 12. Navigation and app update flow

- **Implemented (source):** bottom navigation and More-menu navigation entry points.
- **Implemented (source):** manual selection and hand-off of a local APK to Android's package installer.
- **Needs testing:** all navigation/back-stack paths and APK update installation/data preservation on real devices.
- **Security note:** Android controls package installation and displays its own confirmation UI. FinTrack does not silently install updates.

## 13. Offline operation and privacy

- **Source configuration:** the Android manifest does not declare `android.permission.INTERNET`.
- No sign-in, OTP, cloud sync, online account, subscription or cloud backup is part of this offline edition.
- Finance data and reminder preferences are stored locally by the app.
- Export and backup files are written only after the user chooses a destination.
- Reminders use Android's local scheduling and notification facilities.
- Clearing app storage or uninstalling the app may permanently remove local records. Export a backup regularly and keep it in a safe location.

## 14. Developer review and release status

This is a development-branch README, not a release certification.

- **Branch:** `fintrack-2.5.0-development`
- **Pull request:** PR #3 remains separate from `main`.
- **Main branch:** not modified by this documentation update.
- **Build verification:** check the latest GitHub Actions run for the exact commit before distributing an APK.
- **Test coverage:** unit tests and XLSX instrumented tests are present in the project; instrumented tests require a configured Android emulator/device.
- **Not yet certified by this README:** physical-device testing, exhaustive regression testing, accessibility review, battery/background behaviour, and verification of every exported file in third-party applications.

Do not treat a successful APK compilation as proof that every feature works correctly. Report bugs with the device model, Android version and steps to reproduce.

## Build

Prerequisites: JDK 17 and Android SDK Platform 35.

```bash
./gradlew --no-daemon testDebugUnitTest assembleDebug
```

Debug APK output:

`app/build/outputs/apk/debug/app-debug.apk`

The GitHub Actions workflow is configured to build the debug APK and publish the `FinTrack-debug-apk` artifact when the workflow succeeds. Confirm the artifact is present for the latest development-branch commit before downloading.

## Application identity

| Property | Value |
|---|---|
| App name | FinTrack |
| Application ID | `com.charan.fintrack` |
| Version name | `2.5.0` |
| Version code | `27` |
| Minimum Android | API 23 (Android 6.0) |
| Target Android | API 35 (Android 15) |
| Edition | Offline |

---

**FinTrack v2.5.0 Offline Edition · Development branch documentation**
