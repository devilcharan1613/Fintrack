# Tracmio

**Version 16.13 (`versionCode 30`)**

**Tracmio — Finance & Expenses Tracker** is a 100% offline-first native Kotlin + Jetpack Compose personal finance, ledger, and planning application.

## 16.13 Native Kotlin Release

- **Pure Native Kotlin & Jetpack Compose** (zero `WebView` or browser engines) styled with the **Aurora Teal Liquid Glass (v2.5.0)** UI system, 11 theme presets, 10 mesh wallpapers, UI scaling, and `SensorManager` liquid tilt parallax.
- **Encrypted Local Storage**: All accounts, allocations, transactions, budgets, goals, loans, persons, custom trackers, and reminders live inside **Room + SQLCipher (AES-256)** and **EncryptedSharedPreferences** backed by **Android Keystore**.
- **App Lock & Mandatory Recovery**: 4 or 6-digit PIN + Biometric (`BiometricPrompt`) unlock backed by salted SHA-256 + Keystore, verified PIN confirmation on disable, and offline Security Question / Recovery Key reset.
- **100% Feature Suite**:
  - **Dashboard & Onboarding**: Multi-currency display (`₹ / $ / € / £`), Monthly Expense Pace Meter, Quick Stats 2×2 Grid, Opt-In Paste SMS / UPI Quick-Log, Safe-to-Spend daily limit, Triple-Tap Stealth Mode, Quick Spend chips (`×` delete), and Warranty/Deadline cards.
  - **Accounts & Wallets**: Account Allocations + Auto-Split income %, Balance Reconcile, 3-Step Cash Counter (Notes `₹500–₹10` & Coins `₹20–₹1`), Credit Card billing cycle & `>30%` utilization alert, Account Edit/Archive, and direct `+ Deposit` / `− Withdraw` / `⇄ Transfer` / `💵 Count`.
  - **Transactions & Receipts**: 5-type modal (`EXPENSE`, `INCOME`, `BORROW`, `LEND`, `TRANSFER`), Allocation pocket sync, Tracker linking, Camera/Photo Picker receipt attachments, custom `+ UPI App` chips, Top-Left Filters, Date-Grouped ledger, `#Hashtag` totals, `↻ Repeat`, top-left red delete icon, and `.xlsx`/CSV/PDF export.
  - **Planning, People & Trackers**: Budgets/Goals/Loans edit & top-ups, Wishlist `Buy Now`, Snowball vs. Avalanche debt payoff planner, Person Detail ledger + `1-Tap Settle Up`, Smart Split & WhatsApp Summary, Subscriptions (`Pay & Log`), and full Custom Tracker Folder workspaces.
  - **Intelligence & Reporting**: Interactive Reports (Period filters, 4 visual styles, 4 color palettes, 6-month Cashflow Canvas, UPI & Weekend Radar), Financial X-Ray 0–100 score ring + 10 vitals, Money Story (`50/30/20`), Spending DNA (`Mon–Sun`), 3-Tab Time Machine (Past / Future Forecast / Compare Dates), and 7-Column Interactive Calendar + Streak milestones.

## Privacy & Zero-Network Security

- Zero mandatory sign-in, cloud synchronization, ads, or analytics.
- `AndroidManifest.xml` declares **zero `INTERNET` or `ACCESS_NETWORK_STATE` permissions** and **zero broad storage permissions** (`Photo Picker` + `SAF` only).
- Full JSON and PBKDF2-HMAC-SHA256 + AES-256-GCM encrypted backups with strict schema sanitization.

## Build

Use JDK 17 and Android SDK Platform 35:

```bash
gradle :app:testDebugUnitTest assembleDebug
```

Single APK output: `app/build/outputs/apk/debug/Tracmio-16.13.apk`

## Application Identity

- App name: **Tracmio**
- Tagline: Finance & Expenses Tracker
- Application ID / Namespace: `com.tracemeo.app`
- Version name: `16.13`
- Version code: `30`
- Minimum Android SDK: `26`
- Target Android SDK: `35`
