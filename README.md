# Tracmio

**Version 2.5.2**

Tracmio — Finance & Expenses Tracker is an offline-first Android personal finance & ledger manager.

## 2.5.2 update

- Complete rebrand and upgrade to **Tracmio — Finance & Expenses Tracker** (`com.charan.tracmio`, version 2.5.2, versionCode 29).
- Dual WebView bridge registration (`TracmioBridge` & `FinTrackBridge`) for 100% compatibility.
- 3D Liquid Glass Tracmio brand identity, animated Welcome Overlay, 10 built-in Liquid Glass themes + Custom Neon Theme Studio, custom photo background & UI scale controls.
- Custom UPI App creator inside the Add Transaction modal (`Income`, `Expense`, `Transfer`, `Borrow`, `Lend`).
- 4-Style Chart Switcher (`3D Neon Bars`, `Area Wave`, `Capsules & Polar`, `Executive`) and 4 Color Theme Palettes on Reports.
- Native `.xlsx` workbook export, PDF statement printing (`TRACMIO · OFFLINE FINANCE MANAGER`), and JSON backup/restore.
- Offline daily reminder engine with rotating meme notifications and quick-action buttons (`Add income`, `Add expense`).

## Features

- Dashboard, accounts, wallets, and recent transactions.
- Income, expense, transfer, borrowing, and lending records with custom UPI apps.
- Categories and subcategories.
- People and lending/borrowing history.
- Budgets and savings goals with goal contributions.
- Loans, EMI, and repayments.
- Reports with period filters, breakdowns, 4 chart styles, and PDF/Excel statement exports.
- Settings, appearance customization, privacy information, JSON backup, and validated restore.

## Privacy and storage

- No mandatory sign-in, cloud synchronization, ads, or analytics.
- The Android manifest does not request the `INTERNET` or `ACCESS_NETWORK_STATE` permissions.
- Financial records are stored 100% locally on the device.
- Exported backup files are user-controlled. Store them safely.

## Build

Use JDK 17 and Android SDK Platform 35.

```bash
./gradlew --no-daemon testDebugUnitTest assembleDebug
```

APK output: `app/build/outputs/apk/debug/Tracmio-2.5.2-debug.apk`

GitHub Actions uploads the build as `Tracmio-debug-apk`.

## Application identity

- App name: Tracmio
- Tagline: Finance & Expenses Tracker
- Application ID: `com.charan.tracmio`
- Version name: `2.5.2`
- Version code: `29`
- Minimum Android SDK: 23
- Target Android SDK: 35
