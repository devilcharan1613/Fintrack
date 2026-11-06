# FinTrack

## Included
- Kotlin + Jetpack Compose + Material 3 foundation
- Dark navy and emerald theme
- Three-step onboarding with animated progress
- Animated navigation transitions
- Offline SQLite persistence for accounts and financial records
- Dashboard with balance, income/expense summary, quick actions and empty states
- Account, transaction, budget, savings goal and loan entry forms
- Income, expense, transfer, borrow and lend transaction types with subcategories
- Reports for income, expenses, net cash flow and current balance
- Bottom navigation: Home, Accounts, Transactions, Reports, More
- Local categories and people management
- Atomic transfer records and Room schema migrations through version 3
- Validated account references and atomic transfer writes
- No login, cloud sync, INTERNET permission, or demo finance records

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
- Adaptive edge-to-edge safe areas for status bars, cutouts, navigation modes, and IME
- Liquid Glass surfaces with blur fallbacks, responsive spacing, and reduced-motion support

## Privacy and storage

- No `INTERNET` permission, login, cloud sync, ads or analytics.
- The web interface stores data in the app's local WebView storage.
- On first launch, existing records from the previous `fintrack.db` SQLite database are read and migrated into the new local data store. The old database is not modified or deleted.
- A clean installation starts with empty accounts and transactions. There are no pre-filled sample balances.
- Export backups regularly. Clearing app storage or uninstalling the app can remove local records.

## Build

The repository includes a GitHub Actions workflow at `.github/workflows/android-apk.yml`.

The Room database preserves the original accounts table through migrations and adds
transactions, budgets, goals, loans, categories, people, and loan payments without
resetting existing records. JSON backups include account, transaction, planning,
category, and people data; CSV export contains transaction history.

## Verification
The debug APK builds with `./gradlew assembleDebug` and is written to
`app/build/outputs/apk/debug/app-debug.apk`.

## UI system

The Android shell uses `WindowInsetsCompat` with edge-to-edge rendering and passes
system-bar, display-cutout, and IME insets to the offline WebView as CSS variables.
The web UI consumes those values alongside `env(safe-area-inset-*)`, avoiding
double-applied padding across gesture and three-button navigation. Glass surfaces
use CSS backdrop blur where available and opaque translucent fallbacks otherwise.
