# Tracmio_v16.13_Master_PRD.md
**Refined Product Requirements Document (PRD) & Master Engineering Specification**  
**App Name**: Tracmio — Finance & Expenses Tracker  
**Exact Package / Namespace**: `com.tracemio.app` (`applicationId = "com.tracemio.app"`, `namespace = "com.tracemio.app"`)  
**Version**: `versionName = "16.13"` (`versionCode = 30`)  
**SDK Targets**: `minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`  
**Design System**: v2.5.0 Aurora Teal Liquid Glass (Strict 1:1 Lock with `index.html`)  
**Offline & Privacy Posture**: 100% Offline, Zero Network Permissions (`INTERNET` and `ACCESS_NETWORK_STATE` strictly forbidden), SQLCipher + Android Keystore Encrypted Storage

---

## 1. Exact Visual Theme & Design System Specification (Why the UI Changed & How to Lock It)

### 1.1 Root-Cause Analysis & Mandatory Fixes
1. **Deep Teal Aurora Base (No Flat Dark-Navy)**:
   - Lock background to `#031923` → `#052230` → `#04131A` with radial cyan (`#00D6A3`, `alpha = 0.42f`), neon aqua (`#18F2D0`, `alpha = 0.22f`), and violet (`#695FDC`, `alpha = 0.34f`) aurora orbs.
2. **Override All Material 3 Surface Tokens**:
   - Explicitly map `surface`, `surfaceVariant`, `surfaceContainer`, `surfaceContainerHigh`, `surfaceContainerHighest`, and `outline` in `MaterialTheme.colorScheme` to the translucent dark-teal glass palette so `ModalBottomSheet`, `AlertDialog`, `FilterChip`, and `OutlinedTextField` never inject default gray/purple Material surfaces.
3. **Top Specular Highlight + Cyan Glass Border**:
   - Every `GlassCard` must use a top-lit vertical gradient (`Color(0xC70F3A4E)` → `Color(0xB8092B3C)`), `20.dp` corner radius, and a `1.4.dp` vertical border brush (`Color(0x29FFFFFF)` top specular highlight → `Color(0x3D7DF5FF)` cyan glass border).
4. **High-Contrast Ice-Cyan Typography**:
   - Lock `textPrimary = Color(0xFFFFFFFF)` and `textSecondary = Color(0xFFC0D4DF)`.
5. **Uniform 2-Column Grids (Zero Zigzag)**:
   - Render 2-column rows using `items.chunked(2)` inside `Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max))` with `Modifier.weight(1f).fillMaxHeight()` on both child `GlassCard`s.

### 1.2 Locked Color Token Table (v2.5.0 `index.html` ↔ Compose)

| Token Role | Original v2.5.0 CSS (`index.html`) | Jetpack Compose `Color` Literal | Usage Rules |
| :--- | :--- | :--- | :--- |
| **Shell Base Top / Mid / Bottom** | `#031923` / `#052230` / `#04131A` | `0xFF031923` / `0xFF052230` / `0xFF04131A` | Vertical background gradient stops |
| **Aurora Orbs (Cyan / Violet / Aqua)** | `rgba(0,214,163,.42)` / `rgba(105,95,220,.34)` / `rgba(24,242,208,.22)` | `0xFF00D6A3` / `0xFF695FDC` / `0xFF18F2D0` | GPU-composited parallax radial aurora glow (no per-frame `.blur()` on animated Canvas) |
| **Glass Card Surface (Top → Bottom)** | `rgba(15, 58, 78, 0.78)` → `rgba(9, 43, 60, 0.72)` | `0xC70F3A4E` → `0xB8092B3C` | All cards, modals, inputs, and bottom sheets |
| **Glass Border & Top Specular** | `rgba(255,255,255,.16)` → `rgba(125,245,255,.24)` | `0x29FFFFFF` → `0x3D7DF5FF` | `1.4.dp` outer glass border stroke |
| **Floating Bottom Nav & Topbar** | `rgba(9, 43, 60, 0.84)` | `0xD6092B3C` | Fixed topbar & `28.dp` floating pill bottom nav |
| **Primary Neon Teal / Aqua** | `#00D6A3` / `#18F2D0` / `#00F5D4` | `0xFF00D6A3` / `0xFF18F2D0` / `0xFF00F5D4` | Primary buttons, active tabs, income, streaks |
| **Expense Rose / Transfer Blue / Alert Gold** | `#FB7185` / `#38BDF8` / `#FBBF24` | `0xFFFB7185` / `0xFF38BDF8` / `0xFFFBBF24` | Expenses & top-left receipt delete / transfers / `>30%` CC & Khata alerts |
| **Primary / Secondary Text** | `#FFFFFF` / `#C0D4DF` | `0xFFFFFFFF` / `0xFFC0D4DF` | All headings & balances / subtitles & helper labels |

---

### 1.3 Locked Topbar, Bottom Nav, Stealth Mode & Gesture Rules
1. **Fixed Left-Aligned Topbar (`TracmioTopBar`)**:
   - **Left**: `36.dp` 3D Tracmio logo badge + bold `Tracmio` title fixed on the left across **all** screens; current screen name or greeting appears in the subtitle line (`#C0D4DF`).
   - **Right**: Privacy Eye button (`36.dp` circle) + neon `+ Add` pill button. **No topbar search icon.**
   - **Privacy Eye Behavior**:
     - **Single-Tap**: Masks all amounts as `₹••••••`.
     - **Triple-Tap (within 750ms)**: Toggles **Decoy Stealth Mode** — replaces real balances and amounts across the entire UI with realistic decoy minimal numbers (`₹25`–`₹180`) for public privacy.
2. **5-Tab Floating Pill Bottom Navigation (`FloatingGlassBottomNav`)**:
   - Match `index.html`'s 5 bottom tabs (`Dashboard`, `Accounts`, `Transactions`, `Reports`, `More`) inside the `28.dp` floating Liquid Glass pill bar.
3. **3D Glass Badges, Capsule Sliders & Sheet Gestures**:
   - `GlassBadge3D` (`38.dp`–`44.dp` radial neon-teal glass icon container) on all More, Tools Hub, and Settings tiles.
   - Apple-inspired `30.dp`–`38.dp` tall Liquid Glass brightness-style capsule sliders with **`+1 / -1` exact 1% step buttons** (`80%`–`130%` UI scale).
   - Smooth pull-down-to-dismiss on all bottom sheets (ignoring touches that start inside `input`/`textarea`/`select` so scrolling or typing in **Notes** never jumps the sheet to the top).

---

## 2. Screen-by-Screen UI, Button & CRUD Specification

### 2.1 Dashboard (`route = "dashboard"`)
1. **Hero Balance Card**: Total Balance, Income (`#00D6A3`), Expense (`#FB7185`).
2. **Monthly Pace Meter**: Tracks month-to-date expense pace vs. calendar progress (`On Track` / `Caution` / `Overpace`).
3. **🛡️ Safe-to-Spend Daily Limit Card**:
   - **Exact Formula**:
     $$\text{Daily Safe-to-Spend} = \frac{\sum \max(0, \text{Non-Credit Account Balance} - \text{Allocated Buckets})}{\text{Remaining Days in Current Month (inclusive of today)}}$$
   - Shows today's spend vs. Daily Safe-to-Spend cap, progress bar, remaining days, and total unallocated balance.
4. **⚡ 1-Tap Quick Spend Chips**:
   - Horizontal scroll of chips (`☕ Tea ₹20`, `🚌 Auto ₹50`, `🍽️ Lunch ₹120`, `📱 Recharge ₹199`) + **`+ Chip`** modal button.
   - **Tap Chip**: Logs expense immediately with a **2-second `UNDO` Snackbar**.
   - **Tap `×` on Chip**: Deletes chip immediately (with `UNDO` Snackbar) and respects an empty list if all chips are removed.
5. **Quick Stats 2×2 Uniform Grid**: `Borrowed`, `Lent`, `Net Worth`, `Transactions`.
6. **Wallets Horizontal Strip (`112.dp` uniform height)**: Account cards + `+ Add` card.
7. **🌙 End-of-Day "Anything Spent Today?" Card**: Shown when `todayExpenseCount == 0` with a `+ Add ₹` button.
8. **📦 Waiting for Refund / Return Card**: Inline `+ Add` (`Title`, `Amount ₹`), `Received ✓` (credits account & logs refund income), and `✕` delete.
9. **Quick-Filter Pills & Recent Activity**: `All`, `Today`, `Yesterday`, `Cash`, `UPI` pills above Recent Activity; tapping any transaction row opens the **Expense Receipt Modal**.

---

### 2.2 Accounts (`route = "accounts"`)
1. **🏧 1-Tap ATM Withdrawal (`Bank → Cash`) Card**:
   - Instant `₹500`, `₹1,000`, `₹2,000` buttons transferring from the primary Bank account to the Cash/Wallet account in one atomic transaction.
2. **Account Cards & Action Bar**:
   - Shows account name, type badge, balance, allocation pills (`Personal · ₹500`, `Bills · ₹200`, `Unallocated · ₹100`), Credit Card cycle (`Stmt Day`, `Due Day`, and **`⚠️ >30% Utilization`** alert), and wrapped action buttons:
     - **`+ Deposit`**, **`− Withdraw`**, **`⇄ Transfer`**
     - **`📂 Allocate`**: Opens **Account Allocations Modal** (`+ Set Allocation`, inline **Auto-Split Income `%`** rule per bucket, `✕ Remove`, enforcing $\sum \text{Allocations} \le \text{Account Balance}$).
     - **`⚖️ Reconcile`**: Opens **Reconcile Balance Modal** (enter actual balance → auto-logs `Balance Adjustment` for $\Delta$).
     - **`💵 Cash Count`** (on Cash/Wallet accounts): Opens **3-Step Cash Denomination Counter**:
       - **Step 1**: Pick Note (`₹500`, `₹200`, `₹100`, `₹50`, `₹20`, `₹10`) or Coin (`₹20`, `₹10`, `₹5`, `₹2`, `₹1`).
       - **Step 2**: Set quantity with `−` / `+` stepper and tap **`+ Add`**.
       - **Step 3**: Review added rows (with `✕` remove), live **Counted Total**, optional **Note** field, and tap **`Apply & Reconcile Cash`**.

---

### 2.3 Add/Edit Transaction Modal, Expense Receipt & Transactions (`route = "transactions"`)
1. **Add/Edit Transaction Modal (`#overlay-txn`)**:
   - **5-Card Type Grid**: `Income`, `Expense`, `Transfer`, `Borrow`, `Lend` (**no** SMS paste auto-fill box).
   - **Amount with Inline Math**: Accepts expressions like `120+45+30` or `(240+60)/2` with a `= Calc` button and auto-evaluation on blur/save (safe recursive parser, zero `eval()`).
   - **Income Source Toggle** (shown when type is `Income`): `💼 Own Earnings / Freelance` vs. `🏠 Parent / Family Pocket-Money`.
   - **Account / Wallet Field**: Shows **both** **`+ Add Wallet`** and **`+ Add Account`** quick buttons above the dropdown, and includes both `➕ + Add New Wallet...` and `🏦 + Add New Bank / Account...` inside the dropdown (even when 0 accounts exist).
   - **Account Allocation Selector**: Automatically appears right below Account whenever the selected account has allocations (e.g., `Personal Money (₹500)`), deducting/crediting that bucket on save.
   - **Category & Subcategory**: Defaults to neutral **`Select Category`** (never `Food`) with all 17 expanded daily categories (`Food & Chai`, `Khata & Kirana`, `Transport & Travel`, `SIM, WiFi & Bills`, `College & Exams`, `Family & Income`, `Shopping & Personal`, `Health & Medical`, `Entertainment`, `Investment`, `Fuel & Commute`, `Rent & Housing`, `Recharge & WiFi`, `Gifts & Celebrations`, `Business & Studio`, `Pets & Care`, `Other`) + `+ Add` buttons.
   - **Payment Method & Half-Cash + Half-UPI Split**: Includes `💵+📱 Half-Cash + Half-UPI Split` (revealing `Cash Portion ₹`, `UPI Portion ₹`, and `Cash Wallet Account` selector to log both legs atomically).
   - **Notes (`#Hashtags`), Custom Tracker Link, Receipt Attachment & Warranty/Deadline Date**: Stable scroll position when focusing Notes; no automatic receipt popup after saving.
2. **Expense Receipt Modal (`#overlay-txnreceipt`)**:
   - **Top-Left Red Delete Icon (`#FB7185`)**: Appears **only** in the top-left of the Expense Receipt modal (never on transaction list rows); reverses account & allocation balances on delete.
   - **Top-Right Edit Pencil (`✏️`)**: Opens transaction in edit mode.
   - **Bottom `↻ Repeat This Transaction` Button**: Clones the transaction with today's date/time in 1 tap.
3. **Transactions Screen (`#page-transactions`)**:
   - **Top-Left `Filter` Button**: Expands/collapses the filter panel containing **Account**, **Quick date range** (`All dates`, `Today`, `Yesterday`, `This week`, `Last 7 days`, `This month`, `Last month`, `Custom date range`), **From date**, **To date**, and **Clear filters**.
   - **Clickable `#Hashtag` Pills**: Any `#tag` in transaction notes opens `#overlay-hashtag` showing instant **Total Spent**, **Total Income**, and matching transactions across all accounts.

---

### 2.4 People, Khata & Planning (`route = "people"`, `route = "budgets"`, `route = "goals"`, `route = "loans"`)
1. **People (`#page-persons`)**:
   - Top bar with **`⚡ Smart Split Bill`** (`Equal / % / Exact` + `Include my own share`), **`📋 Summary`** (copies WhatsApp settlement summary), and **`+ Person`**.
   - Person cards show **`📋 <upiId>`** (1-tap copy UPI ID) and **`✓ Settle`** button.
   - **📓 Friend / Shopkeeper Khata (Pay Later) Card**: Inline `+ Add Khata` (`Shop/Friend`, `Amount ₹`, `Item`), **`Clear ✓`** (settles & logs expense), and **`✕`** delete.
2. **Budgets (`#page-budgets`)**: Monthly category budgets + **🛟 Emergency Fund Runway Meter** (`(Bank + Savings) ÷ Monthly Spend`).
3. **Savings Goals (`#page-goals`)**:
   - Goals list + **🪙 Round-Up Spare Change Jar** (`Nearest ₹10 / ₹100` toggle + target goal), **🧊 24h Cooling Wishlist** (`+ Start 24h Cooldown`, `✓ Skip & Save`), **🎁 Cashback & Rewards** (`+ Log Cashback`), and **🧮 Price-Per-Use Value Calculator** (`+ Track Item Value`, `+1 Use`).
4. **Loans & EMI (`#page-loans`)**:
   - Loans list (`+ Record repayment`, `History`) + **📈 Multi-Account Net Worth & Snowball vs. Avalanche Debt Payoff Planner** (`Assets`, `Debt`, `Net Worth`, `Extra Monthly Payoff ₹` input, and side-by-side Snowball vs. Avalanche months).

---

### 2.5 More Menu, Dedicated Tools Hub Folder, Reports, Calendar & Reminders
1. **Dedicated Tools Hub Folder (`#page-more`)**:
   - In **More**, **Tools Hub** is a single tile box in the 2-column grid.
   - Tapping **Tools Hub** opens the folder view showing **ONLY** the tools inside **Tools Hub** (with `⇄ Organize` and `✕ Close / ← Back`)—never repeating the rest of the Menu tiles underneath unless `⇄ Organize` mode is active.
2. **Reports (`#page-reports`)**:
   - Period tabs, custom date range, 4 visual styles, Compose/Canvas Category Charts, **💳 Merchant & UPI App Breakdown** (with month-over-month `▲/▼ %` trends), **📅 Weekend vs. Weekday Spend Radar**, **⚖️ Side-by-Side Month Comparison**, and **🏠 Family Pocket-Money vs. 💼 Own Earnings**.
3. **Calendar (`#page-calendar`)**:
   - Monthly calendar grid, **🔥 No-Spend Streak & Milestones** (`3d Saver`, `7d Streak`, `14d Pro`, `₹1k+ Saved`), and **📶 SIM / WiFi Recharge Countdown (`28 / 56 / 84d`)** with `Renew` and `✕`.
4. **Reminders (`#page-reminders`)**:
   - Monthly Backup Reminder, Daily Check-In Reminder, Custom Reminders, **🧾 Receipt Warranty & Deadline Vault**, and **🕘 9 PM Daily Spend Summary** (`Preview Alert`).
5. **🛡️ App Lock (`#page-applock` & `#tracmioLockScreen`)**:
   - Optional Apple-inspired Liquid Glass 4- or 6-digit PIN + `androidx.biometric` Fingerprint/Face unlock.
   - Enforces **Mandatory Recovery Setup** (`Security Question + Answer` + `Backup Recovery Key TRAC-XXXX-XXXX` + **`📦 Full JSON Backup`** including all entities and `appPreferences`).
6. **📖 Kid-Simple In-App Guide & Manual (`#page-guide`)**:
   - Includes the **🚀 Quick-Jump Feature Directory** at the top explaining every feature in 5-year-old-friendly language with a real-life example and a working **`Open →`** button.

---

## 3. Architecture, Security & Build Verification (`com.tracemio.app`, v16.13)
- **Package & Build**: Standardize strictly on `com.tracemio.app` (`versionCode = 30`, `versionName = "16.13"`, `minSdk = 26`, `targetSdk = 35`).
- **Database Migration Safety**: Use explicit Room migrations (plus automatic JSON backup export before schema upgrades—do **not** silently wipe user data with unguarded destructive migrations in production).
- **Single APK & Clean Repo**: Keep only **one** `Tracmio-16.13.apk` (and signed `Tracmio-16.13.aab` for Play Store), remove all old APKs/duplicate HTML files, and verify `./gradlew clean :app:testDebugUnitTest assembleDebug bundleRelease` passes with zero errors.
