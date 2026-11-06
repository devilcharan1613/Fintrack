# FinTrack

FinTrack is an offline-first personal finance tracker for Android. The Android package embeds the supplied FinTrack HTML application as a local asset, so the interface and finance data work without a server or internet connection.

## Included screens and tools

- Dashboard with total balance, income, expenses, quick stats, accounts and recent transactions
- Accounts and wallets with account management and balance actions
- Transactions for income, expenses, transfers, borrowing and lending
- Budgets with category limits and spending progress
- Savings goals with target and saved amounts
- Loans with principal, repayments and EMI tracking
- Reports with time filters, category breakdowns and charts
- More menu for people, settings, backup and privacy
- Local JSON backup export and restore through Android's document picker
- Animated screen and card transitions with a dark navy and emerald interface

## Privacy and storage

- No `INTERNET` permission, login, cloud sync, ads or analytics.
- The web interface stores data in the app's local WebView storage.
- On first launch, existing records from the previous `fintrack.db` SQLite database are read and migrated into the new local data store. The old database is not modified or deleted.
- A clean installation starts with empty accounts and transactions. There are no pre-filled sample balances.
- Export backups regularly. Clearing app storage or uninstalling the app can remove local records.

## Build

The repository includes a GitHub Actions workflow at `.github/workflows/android-apk.yml`.

1. Open the **Actions** tab in this repository.
2. Select **Build FinTrack APK**.
3. Open the latest successful run.
4. Download the `FinTrack-debug-apk` artifact.

The APK inside the artifact is named `app-debug.apk`.

## Application identity

- Application ID: `com.charan.fintrack`
- App label: `FinTrack`
- Minimum Android SDK: 23
- Target Android SDK: 35
- Offline local application, no sign-in required.
