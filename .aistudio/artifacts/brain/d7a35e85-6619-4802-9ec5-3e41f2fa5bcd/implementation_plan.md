# Tracmio — Full Native Kotlin & Jetpack Compose Migration (v1.00)

A complete migration of **Tracmio — Finance & Expenses Tracker** from a hybrid WebView architecture into a 100% pure native Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, and an offline-first **Room Database**, preserving version `1.00` (`versionCode = 1`) and adding the 100-item rotating offline notification catalog.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and product choices have been confirmed from your answers and earlier requirements:

- **Confirmed Decision 1 — 100% Pure Native Jetpack Compose UI & Room Persistence**: `MainActivity` launches directly into the native Jetpack Compose `TracmioApp` (`setContent { TracmioApp() }`) backed by the local `TracmioDatabase` (Room) and preferences storage, removing WebView runtime dependencies while maintaining 120Hz native animations and strict zero-network privacy.
- **Confirmed Decision 2 — Version `1.00` (`versionCode = 1`) Alignment**: All build configurations, APK outputs (`Tracmio-1.00-debug.apk`), Settings/About version badges, JSON backups, and Excel/PDF export headers remain unified at version `1.00` (`versionCode = 1`).
- **Confirmed Decision 3 — 100-Alert Scheduled Background Notification Engine**: The 100 unique smart notifications (25 Witty & Meme reminders, 15 Payday & Cash Flow checks, 15 Smart Budget alerts, 15 Subscription & Bill reminders, 15 Mindful Money nudges, and 15 Milestone & Streak celebrations) are embedded in `assets/notifications.json` and used exclusively by the background `DailyReminderReceiver` and `TracmioReminderScheduler` to cycle daily with `+ Add Income` and `+ Add Expense` quick-action buttons.
- **Confirmed Decision 4 — 10 Background Themes + 10 Liquid Glass UI Styles & 3D Vector Badges**: A dedicated **Themes & Appearance** screen accessible from the **More** menu provides 10 atmospheric background themes paired with 10 customizable Liquid Glass card/surface styles, custom photo background support with live opacity, UI font scaling (`80%–130%`), an auto-hiding floating bottom navigation dock with full safe-area clearance, and 3D glass vector icon badges replacing plain emojis.

---

### 1. Overview & Core Concept

- **What It Does**: Tracmio is a privacy-first, 100% offline personal finance and expense manager. Users can track multi-currency accounts (Bank, Cash, UPI, Credit Card, Investment), log 5 types of transactions (Income, Expense, Transfer, Borrow, Lend) with custom UPI apps and receipt photos, manage category budgets and savings goals, track borrowed/lent person ledgers and loans, analyze spending across 4 interactive Canvas chart styles with tap tooltips, customize 10 backgrounds and 10 Liquid Glass UI styles, and export date-range Excel (`.xlsx`), PDF statements, and JSON backups.
- **Target Audience / Persona**: Privacy-conscious individuals who want complete control over their daily cash flow, UPI spends, budgets, and net worth on their device without cloud accounts or network permissions.
- **Key Value**: Instant native 120Hz responsiveness, unclipped dark-themed cold start with an animated 3D Tracmio logo and mandatory first-time user onboarding, tactile 3D glass vector iconography, and zero-permission offline reliability.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Cold Launch & Mandatory New-User Welcome Flow**: Opening the app displays a dark obsidian-emerald native window (`#030D14`) that transitions smoothly into a 3D animated Tracmio logo reveal (floating bob, dual sonar pulse rings, and shimmer sweep). Every first-time user (or after a data reset) is greeted by the mandatory **Welcome Screen** to enter their display name, choose a default currency (`INR ₹`, `USD $`, `EUR €`, `GBP £`, etc.), and initialize their starter accounts.
  2. **Dashboard & 6-Month Trend Inspection**: The **Home** screen displays the Net Worth & Total Balance glass hero card, Income/Expense summary pills, Quick Stats (Borrowed, Lent, Net Worth, Total Transactions), horizontal account cards, an interactive 6-month trend chart where tapping any data point reveals a floating value tooltip, and recent transactions.
  3. **Add Transaction & Custom UPI App Flow**: Tapping the center `+` button in the floating dock opens an elevated modal bottom sheet with safe-area bottom clearance. Users select from the 5-card volumetric type grid (`Income`, `Expense`, `Transfer`, `Borrow`, `Lend`), enter the amount, pick a 3D vector category badge, choose an account and payment method, create or select a custom UPI app when `UPI` is chosen, optionally attach a receipt photo, and save directly to Room.
  4. **Dedicated Themes & Appearance Studio**: From **More → Themes & Appearance**, users can independently mix and match **10 Atmospheric Background Themes** with **10 Liquid Glass UI Styles**, pick custom primary/secondary neon colors, set a personal background photo with a live `0%–100%` visibility slider, and scale the app UI (`80%–130%`).
  5. **Reports, Export & Scheduled Notifications**: Users switch between 4 native Compose `Canvas` chart renderers (`3D Neon Bars`, `Area Wave`, `Capsules & Polar`, and `Executive Summary`) and 4 color palettes, export filtered transactions to multi-sheet `.xlsx` or printable PDF statements, and configure daily reminder times and notification categories (`All Alerts`, `Meme & Humor Only`, `Milestones & Streaks`, `Strict Financial Hygiene`) powered by the 100-alert catalog.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: **3D Volumetric Liquid Glass** — dark obsidian-teal depth layered with translucent frosted surfaces, specular 1dp gradient borders, and glowing neon accents.
  - *Color Palette & Mood*:
    - Primary Canvas: `#030D14` to `#071A26` radial atmospheric gradients (customizable across 10 Background Themes: *Aurora Teal*, *Tracmio Neon Emerald*, *Obsidian Royal Gold*, *Sapphire Prism*, *Arctic Crystal Pearl*, *Cyber Ruby Nebula*, *Matrix Bio-Forest*, *Amethyst Cosmic Haze*, *Titanium Solar Amber*, and *Deep Ocean Bioluminescence*).
    - Semantic Accents: Emerald Primary (`#22F59B`), Cyan Glow (`#18E0D0`), Income Positive (`#2CE59B`), Expense Coral (`#FF5C7C`), Warning Amber (`#FFB84D`).
  - *Typography & Hierarchy*: Bold geometric display numerals for balances and KPIs, clean sans-serif labels with high-contrast legibility, and dynamic font scaling (`0.8x` to `1.3x`) via `CompositionLocalProvider(LocalDensity)`.
  - *Component Styling & Layout*:
    - **10 Liquid Glass UI Styles**: *Classic Liquid Glass*, *Frosted Diamond Crystal*, *Neon Cyber Edge*, *3D Floating Prism*, *Velvet Matte Smoke*, *Liquid Mercury Chrome*, *Emerald Bio-Glass*, *Royal Gold Luxe*, *Tactical HUD Grid*, and *Ultra-Round Bubble Glass*.
    - **3D Glass Vector Icon Badges**: Every category, account type, quick action, and menu item renders a crisp vector glyph inside a 3D specular glass badge instead of flat system emojis.
    - **Floating Safe-Area Bottom Bar**: Capsule bottom dock (`Home`, `Accounts`, `+`, `Transactions`, `More`) with `windowInsetsPadding(WindowInsets.navigationBars)` and `12.dp` floating margin that smoothly slides down when scrolling down lists and reappears on upward scroll.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Pure Jetpack Compose UI Over Hybrid WebView**
  - *Chosen Approach*: Render all screens, dialogs, bottom sheets, and interactive charts natively in Jetpack Compose (`setContent { TracmioApp() }`) and remove `WebView` from `MainActivity`.
  - *Why*: Eliminates WebView startup overhead, HTML/JS bridge serialization lag, and inset/clipping quirks while unlocking smooth 120Hz Compose animations and direct reactive `StateFlow` updates from Room.
  - *Alternatives Considered*: Keeping a hybrid WebView wrapper was rejected per your preference for a 100% pure native Compose architecture.
- **Decision 2: Exact Paise-Precision Room Database + Preferences Store**
  - *Chosen Approach*: Store all financial entities in `TracmioDatabase` (Room with `Long` paise precision columns alongside `Double` amounts, plus UPI apps and recurring templates) and persist theme, background photo, UI scale, onboarding state, biometric lock, and reminder preferences locally.
  - *Why*: Guarantees zero floating-point drift in ledger calculations, preserves existing Room migrations (`MIGRATION_1_2` through `MIGRATION_6_7`), and keeps 100% of user data offline.
- **Decision 3: Background-Only 100-Alert Smart Notification Rotation**
  - *Chosen Approach*: Bundle all 100 categorized alerts in `app/src/main/assets/notifications.json` and read them inside `DailyReminderReceiver` using persistent index tracking (`last_notification_index` in `SharedPreferences("tracmio_reminders")`) and category filtering.
  - *Why*: Matches your exact preference to use the 100-alert catalog strictly for scheduled background daily notifications without cluttering the main navigation with a separate notification feed screen.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        MainActivity (ComponentActivity)                     │
│  • Dark Launch Theme (No White Flash)  • SAF JSON / XLSX / PDF Launchers    │
│  • Photo Picker & Permission Launchers • Quick-Action Intent Dispatcher     │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ setContent { TracmioApp() }
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                   TracmioRootScaffold & Liquid Glass Engine                 │
│  • 10 Background Themes + Personal Photo Scrim Layer + UI Scale Provider    │
│  • 10 Liquid Glass Surface Styles + 3D Vector Icon Badge Renderer           │
│  • Animated App-Open 3D Logo Splash & Mandatory New-User Welcome Overlay    │
│  • Auto-Hiding Floating Safe-Border Bottom Navigation Dock (5 Tabs)         │
└───────┬───────────────┬──────────────┬───────────────┬──────────────┬───────┘
        │               │              │               │              │
        ▼               ▼              ▼               ▼              ▼
┌──────────────┐ ┌─────────────┐ ┌───────────┐ ┌──────────────┐ ┌─────────────┐
│  HomeScreen  │ │AccountsPage │ │  + Modal  │ │ Transactions │ │  More Hub   │
│ • Net Worth  │ │ • Wallets   │ │   Sheet   │ │ • Date/Type  │ │ • Reports   │
│ • 6M Trend   │ │ • Transfer  │ │ • 5 Types │ │   Filters    │ │ • Themes UI │
│   + Tooltips │ │ • Account   │ │ • Custom  │ │ • Search     │ │ • Budgets   │
│ • QuickStats │ │   Detail    │ │   UPI App │ │ • Edit/Delete│ │ • Goals/Loan│
└───────┬──────┘ └──────┬──────┘ └─────┬─────┘ └──────┬───────┘ └──────┬──────┘
        └───────────────┴──────────────┼──────────────┴────────────────┘
                                       │ StateFlow<NativeTracmioUiState>
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      NativeTracmioViewModel (MVVM/MVI)                      │
│  • Reactive combine() of Room Flows + Theme/Appearance/Settings State       │
│  • Ledger & Analytics Calculators (Net Worth, Category Burn, 6M Series)     │
└───────────────────┬─────────────────────────────────────┬───────────────────┘
                    │                                     │
                    ▼                                     ▼
┌───────────────────────────────────────┐ ┌───────────────────────────────────┐
│   FinanceRepository & Room Database   │ │  Native System Services & Assets  │
│  • Accounts, Transactions, Categories │ │  • DailyReminderReceiver +        │
│  • Budgets, Goals, GoalContributions  │ │    100 Alerts (notifications.json)│
│  • Loans, LoanPayments, Persons       │ │  • XlsxExporter & Native PDF Print│
│  • Custom UPI Apps & Backup Service   │ │  • TracmioPreferencesStore        │
└───────────────────────────────────────┘ └───────────────────────────────────┘
```

- **Data Model & State**:
  - **Core Entities**: `AccountEntity`, `TransactionEntity` (supports `INCOME`, `EXPENSE`, `TRANSFER`, `BORROW`, `LEND`, `upiApp`, `paymentMethod`, `subcategory`, `personId`, `transferAccountId`), `BudgetEntity`, `GoalEntity`, `GoalContributionEntity`, `LoanEntity`, `LoanPaymentEntity`, `CategoryEntity`, `PersonEntity`, plus custom UPI apps and recurring rules.
  - **Appearance & App Preferences**: Active Background Theme (`bg1`–`bg10`), Active Liquid Glass UI Style (`ui1`–`ui10`), Custom Primary/Secondary Neon hex colors, Background Photo path & opacity (`0f–1f`), UI Font/Density Scale (`0.8f–1.3f`), `welcomed` flag, User Name, Default Currency, Daily Reminder schedule (`enabled`, `hour`, `minute`, `alertCategory`), and `last_notification_index` (`0..99`).

- **Interactive Component & State Mapping**:
  - **Cold Start & Mandatory Onboarding**: On launch, `TracmioApp` plays the 3D animated logo splash and checks `preferences.isWelcomed`. If `false` (every new user or post-reset), `WelcomeOverlay` is displayed with name input and currency selector; confirming saves the profile, seeds starter accounts/categories, and transitions to `HomeScreen`.
  - **6-Month Trend & 4-Style Reports Charts**: Tapping any bar or vertex on the 6-month trend chart or the 4 native `Canvas` charts (`NeonBarChart`, `AreaWaveChart`, `CapsulePolarChart`, `ExecutiveSummary`) updates `selectedChartPoint` state and renders an interactive glass tooltip card showing exact month/category, inflow, outflow, and net delta.
  - **Add/Edit Transaction & Custom UPI Creator**: Selecting `UPI` reveals the UPI App chip row with a `+ Add UPI App` button that opens a dialog to persist new UPI apps immediately into the list and select them for the current transaction.
  - **100-Alert Scheduled Notifications**: `DailyReminderReceiver` loads `app/src/main/assets/notifications.json`, filters by the user's chosen alert mode (or all 100 alerts), selects `items[last_notification_index % items.size]`, increments `last_notification_index`, and posts a high-priority notification on channel `"tracmio_daily"` with `+ Add Income` and `+ Add Expense` `PendingIntent` actions that open `MainActivity` directly into `AddTransactionSheet` pre-selected to Income or Expense.
