# 💰 FinTrack

**📱 Version 2.5.1 · Offline Edition · Android**

FinTrack is a native Android personal finance tracker built with Kotlin, Jetpack Compose and Room. The development branch migrates existing local WebView records into the native Room database on first launch, and keeps the original local data untouched during migration. This README distinguishes source implementation from runtime verification. A feature being present in source code does not mean it has passed every device test.

## 🧭 Feature status legend

- **Implemented (source):** corresponding UI or code is present in this branch.
- **Partial:** a basic implementation exists, but completeness or all expected cases are not verified.
- **Needs testing:** implementation exists, but device/emulator or regression testing is still required.
- **Not claimed:** no complete implementation or successful test has been confirmed.

## 📊 1. Dashboard

- **Implemented (source):** dashboard summaries for balances, income, expenses and recent activity.
- **Implemented (source):** borrowed/lent and net-worth summary areas.
- **Needs testing:** verify every dashboard figure against saved accounts, transactions and outstanding balances.

## 👛 2. Accounts and wallets

- **Implemented (source):** multiple account records, opening balances, credit limits, institution/reference metadata and account notes.
- **Implemented (source):** account-linked transaction views and ledger-derived current balances.
- **Implemented (source):** archive accounts without deleting their linked transaction history.
- **Needs testing:** opening/current balance calculations, transfers, edits and deletion edge cases.

## 💸 3. Transactions

- **Implemented (source):** income and expense records.
- **Implemented (source):** transfer, borrowing and lending transaction types.
- **Implemented (source):** date/time, amount, category, account, payment method, UPI app and notes fields where applicable.
- **Implemented (source):** transaction search and type/account/date filtering controls.
- **Implemented (source):** custom From/To date controls in transaction and report views.
- **Needs testing:** end-to-end add/edit/delete flows, transfer pairing, filter boundaries and totals after edits.

## 📈 4. Reports and analytics

- **Implemented (source):** income-versus-expense analytics, report date presets and a custom date-range filter.
- **Implemented (source):** account balances, expense category breakdown and person-wise borrowing/lending summaries.
- **Implemented (source):** transfers excluded from income and expense totals.
- **Implemented (source):** local statement export with one logical row per transfer.
- **Needs testing:** chart aggregation, date inclusivity, empty periods and reconciliation with the transaction list.

## 🎯 5. Budgets

- **Implemented (source):** budget records and budget display/progress UI.
- **Partial:** budget periods and spending calculations require broader validation.
- **Needs testing:** create/edit/delete, period rollover, category matching and over-budget cases.

## 🏦 6. Savings goals

- **Implemented (source):** savings goal records, contribution history, target/remaining calculations, editing and deletion.
- **Implemented (source):** contributions are tracking-only and do not change account balances.

## 🤝 7. Borrowing, lending and loans

- **Implemented (source):** person-linked borrowing/lending records.
- **Implemented (source):** loan records, repayment history, outstanding-principal calculations, edits and deletion.
- **Implemented (source):** repayments are tracking-only and do not change account balances; overpayment is rejected.

## 👥 8. People

- **Implemented (source):** people records and person summary views.
- **Needs testing:** contact validation, linked transaction history and settlement reconciliation.

## 📤 9. Exports and backup

- **Implemented (source):** JSON backup export and restore through Android file selection.
- **Implemented (source):** restore validation and merge by stable record IDs without clearing existing data; duplicate records are skipped.
- **Implemented (source):** CSV transaction export.
- **Implemented (source):** multi-sheet XLSX workbook export, including a summary sheet and finance-data sheets.
- **Implemented (source):** custom date-range XLSX export with strict inclusive local-date validation.
- **Implemented (source):** local A4 PDF statement generation using Android PdfDocument.
- **Implemented (source):** paired transfer rows are represented once in CSV, XLSX and PDF transaction listings.
- **Implemented (source):** CSV fields are quoted and formula-like user text is neutralized for spreadsheet safety.
- **Needs testing:** open generated XLSX files in spreadsheet applications; verify custom-range contents, CSV encoding, PDF page layout and backup compatibility with older app data.

### 🗂️ Export file layouts

**Excel (.xlsx)**

The workbook is designed with separate worksheets for Summary, Accounts, Transactions, People, Budgets, Savings Goals, Loans, Loan Payments and Categories. The Summary sheet contains export metadata and income/expense totals. Column availability depends on the fields stored in the corresponding records.

**Custom-range Excel**

The selected date range is intended to limit the Transactions worksheet and identify the period in the export summary. Other reference sheets provide context for the exported transactions. Verify the output before relying on it as a formal statement.

**CSV (.csv)**

A transaction-row export with fields such as date, type, description/notes, category, account, payment method and amount (INR).

**PDF (.pdf)**

The native export uses Android PdfDocument to create a local A4 transaction statement. Paired transfers are rendered once with source and destination accounts.

**JSON backup (.json)**

A local backup intended to preserve the app's finance data for later restore. Keep the exported file somewhere safe outside the app's private storage.

## 🔔 10. Reminders and notifications

- **Implemented (source):** optional daily reminder scheduling using Android local alarm/notification APIs.
- **Implemented (source):** reminder time and fun-message preference controls.
- **Implemented (source):** notification actions for adding income or expenses.
- **Implemented (source):** boot receiver intended to restore reminder scheduling after restart.
- **Needs testing:** Android 13+ notification permission flow, denied/revoked permission, reboot, Doze/battery restrictions, notification action routing and delivery on supported Android versions.
- **Limitation:** Android may delay or restrict background notifications depending on device settings and battery policies. Reminders do not require internet.

## 🎨 11. Themes and appearance

- **Implemented (source):** dark/light theme preference and shared Liquid Glass styling layer.
- **Implemented (source):** custom background photo selection, photo removal and 0–100% visibility control.
- **Implemented (source):** account metadata and category subcategory editing in native Compose.
- **Needs testing:** consistency of every screen in both themes, photo persistence after restart, contrast/readability and performance on lower-memory devices.

## 🧭 12. Settings, navigation and app update flow

- **Implemented (source):** native Settings exposes local reminders, theme, background photo/visibility, JSON export/restore, CSV, full and date-range XLSX, PDF and manual APK update.
- **Implemented (source):** a confirmation dialog protects the destructive reset-all-finance-data action.
- **Implemented (source):** native screen destinations use Navigation Compose, with bottom navigation and More-menu entry points.
- **Implemented (source):** legacy-launcher migration gate; after a successful first-run merge, MainActivity opens the native Compose app on subsequent launches.
- **Implemented (source):** manual selection and hand-off of a local APK to Android's package installer.
- **Needs testing:** all navigation/back-stack paths, fresh-install flow and APK update installation/data preservation on real devices.
- **Security note:** Android controls package installation and displays its own confirmation UI. FinTrack does not silently install updates.

## 🔒 13. Offline operation and privacy

- **Source configuration:** the Android manifest does not declare `android.permission.INTERNET`; cleartext traffic is disabled and Android automatic app backup is disabled.
- CI checks the offline manifest configuration, bundled HTML references and use of network APIs.
- No sign-in, OTP, cloud sync, online account, subscription or cloud backup is part of this offline edition.
- Finance data and reminder preferences are stored locally by the app. This release does not claim SQLCipher database encryption or an app lock.
- Export and backup files are written only after the user chooses a destination.
- Reminders use Android's local scheduling and notification facilities.
- Clearing app storage or uninstalling the app may permanently remove local records. Export a backup regularly and keep it in a safe location.

## 🛠️ 14. Developer review and release status

This is a development-branch README, not a release certification.

- **Branch:** `fintrack-2.5.0-development`
- **Native release line:** version `2.5.1` (version code `28`) on this development branch.
- **Main branch:** unchanged by this implementation work.
- **Build verification:** check the latest GitHub Actions run for the exact commit before distributing an APK.
- **Test coverage:** unit tests cover paise arithmetic, ledger calculations and reminder time; instrumentation tests cover Room migrations, backup merge/restore, XLSX structure, native navigation, settings and repository validation.
- **Not yet certified by this README:** physical-device testing, exhaustive regression testing, accessibility review, battery/background behaviour, and verification of every exported file in third-party applications.

Do not treat a successful APK compilation as proof that every feature works correctly. Report bugs with the device model, Android version and steps to reproduce.

## ⚙️ Build

Prerequisites: JDK 17 and Android SDK Platform 35.

```bash
./gradlew --no-daemon testDebugUnitTest assembleDebugAndroidTest assembleDebug
```

Debug APK output:

`app/build/outputs/apk/debug/app-debug.apk`

The GitHub Actions workflow is configured to build the debug APK and publish the `FinTrack-debug-apk` artifact when the workflow succeeds. Confirm the artifact is present for the latest development-branch commit before downloading.

## 📦 Application identity

| Property | Value |
|---|---|
| App name | FinTrack |
| Application ID | `com.charan.fintrack` |
| Version name | `2.5.1` |
| Version code | `28` |
| Minimum Android | API 23 (Android 6.0) |
| Target Android | API 35 (Android 15) |
| Edition | Offline |

---

**Made for simple, private, offline-first money management. 💚**

*FinTrack v2.5.1 Offline Edition · Development branch documentation*
