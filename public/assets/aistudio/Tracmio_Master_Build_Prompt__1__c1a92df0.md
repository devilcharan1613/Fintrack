# Tracmio Master Build Prompt — Goal, Instructions & Step-by-Step Process

Use this master prompt with your AI coding agent (**Cursor**, **Claude Code**, **GitHub Copilot**, **Windsurf**, or **Codex**) inside your [devilcharan1613/Fintrack](https://github.com/devilcharan1613/Fintrack/) repository ([PR #3](https://www.google.com/search?q=https://github.com/devilcharan1613/Fintrack/pull/3), [CI Run 42745161349](https://www.google.com/search?q=https://github.com/devilcharan1613/Fintrack/actions/runs/42745161349)) to convert the entire project from **FinTrack** to **Tracmio** (`com.charan.tracmio`).

---

## 📋 COPY-PASTE MASTER PROMPT START

```markdown
# MASTER ENGINEERING PROMPT: Rebrand & Convert FinTrack Repository to Tracmio (v2.5.2)

## 1. GOAL
Your goal is to completely rebrand and upgrade this Android repository from **FinTrack** (`com.charan.fintrack`) to **Tracmio — Finance & Expenses Tracker** (`com.charan.tracmio`, v2.5.2, versionCode 29), rename all git branches, Gradle configurations, package directories, Kotlin classes, database names, shared preferences, notification channels, and CI workflows from `FinTrack`/`fintrack` to `Tracmio`/`tracmio`, and wire the exact provided `index.html` into `app/src/main/assets/index.html` to produce a 100% working, offline-first Android application and APK.

---

## 2. CORE INSTRUCTIONS & NON-NEGOTIABLE CONSTRAINTS
1. **Exact HTML Preservation (`app/src/main/assets/index.html`)**:
   - Use the provided `index.html` verbatim as `app/src/main/assets/index.html`. Do NOT strip, minify, or rewrite its CSS, SVG icons, 20 screens, or JavaScript functions.
   - Verify that `"Cloud Sync / Disabled"`, `"Install Update from APK"`, `"🎭 Meme & Smart Notification Center"`, `"😂 Test Meme Alert"`, `"Export Excel Workbook"`, and `"Export Date-Range Excel"` remain completely removed from the HTML.
2. **Complete Identifier Rebranding (`FinTrack` → `Tracmio`)**:
   - Every user-facing string, app title, PDF print header (`TRACMIO · OFFLINE FINANCE MANAGER`), notification title, package name (`com.charan.tracmio`), and APK filename (`Tracmio-2.5.2-debug.apk` / `Tracmio-2.5.2-release.apk`) must use **Tracmio**.
3. **Dual WebView Bridge Registration for Zero-Breakage Compatibility**:
   - Register the Android `@JavascriptInterface` bridge under **both** `"TracmioBridge"` and `"FinTrackBridge"` on the `WebView` instance so every JavaScript call in `index.html` works seamlessly.
4. **Strict Offline-First Privacy**:
   - Do NOT add `android.permission.INTERNET` or `android.permission.ACCESS_NETWORK_STATE`. All data must remain 100% local on the device.
5. **Mandatory Build & Test Verification**:
   - Do not finish until `./gradlew clean testDebugUnitTest assembleDebug` succeeds with zero compilation or unit test errors.

---

## 3. STEP-BY-STEP EXECUTION PROCESS

### PHASE 1: Git Branch, Repository Config & CI Workflow Rename
1. **Rename Git Branches**:
   - Rename the active development branch from `fintrack-2.5.0-development` to `tracmio-2.5.2-development`:
     ```bash
     git branch -m fintrack-2.5.0-development tracmio-2.5.2-development
     ```
   - If pushing to a renamed GitHub repository (`devilcharan1613/Tracmio`), update the remote URL:
     ```bash
     git remote set-url origin https://github.com/devilcharan1613/Tracmio.git
     ```
2. **Update Root Project Name (`settings.gradle.kts` / `settings.gradle`)**:
   ```kotlin
   rootProject.name = "Tracmio"
   include(":app")
   ```
3. **Update GitHub Actions Workflows (`.github/workflows/*.yml`)**:
   - Update branch triggers to include `main`, `master`, and `tracmio-2.5.2-development`.
   - Rename artifact upload names from `FinTrack-*` to `Tracmio-debug-apk` and `Tracmio-release-apk`.

---

### PHASE 2: Gradle Module & Kotlin Package Refactoring (`com.charan.tracmio`)
1. **Update `app/build.gradle.kts`**:
   - Set `namespace = "com.charan.tracmio"` and `applicationId = "com.charan.tracmio"`.
   - Set `versionCode = 29` and `versionName = "2.5.2"`.
   - Update APK output naming so debug/release builds output `Tracmio-2.5.2-debug.apk` and `Tracmio-2.5.2-release.apk`.
2. **Move Source & Test Package Directories**:
   - Move `app/src/main/java/com/charan/fintrack/` → `app/src/main/java/com/charan/tracmio/`
   - Move `app/src/test/java/com/charan/fintrack/` → `app/src/test/java/com/charan/tracmio/`
   - Move `app/src/androidTest/java/com/charan/fintrack/` → `app/src/androidTest/java/com/charan/tracmio/`
   - Update all `package com.charan.tracmio...` and `import com.charan.tracmio...` statements across every `.kt` file.
3. **Rename Kotlin Classes & Identifiers**:
   - `FinTrackDatabase.kt` → `TracmioDatabase.kt` (class `TracmioDatabase`, database file `"tracmio_database"`, migrating existing `"fintrack_database"` if present)
   - `FinTrackReminderScheduler.kt` → `TracmioReminderScheduler.kt` (object `TracmioReminderScheduler`)
   - `NativeFinTrackActivity.kt` → `NativeTracmioActivity.kt` (class `NativeTracmioActivity`)
   - `NativeFinTrackApp.kt` → `NativeTracmioApp.kt` (composable `NativeTracmioApp`)
   - `NativeFinTrackViewModel.kt` → `NativeTracmioViewModel.kt` (class `NativeTracmioViewModel`)
   - `Theme.kt`: rename `FinTrackNativeTheme` → `TracmioNativeTheme`
   - `MainActivity.kt`: rename inner bridge class `FinTrackBridge` → `TracmioBridge`

---

### PHASE 3: AndroidManifest.xml, Strings & App Launcher Icon Setup
1. **Update `app/src/main/res/values/strings.xml`**:
   ```xml
   <resources>
       <string name="app_name">Tracmio</string>
       <string name="app_tagline">Finance &amp; Expenses Tracker</string>
   </resources>
   ```
2. **Replace App Icons with the 3D Liquid Glass Tracmio Logo (`76767.png`)**:
   - Copy the Tracmio logo into `app/src/main/assets/tracmio_logo.png` and generate `ic_launcher.png`, `ic_launcher_round.png`, and `ic_launcher_foreground.png` across `mipmap-mdpi`, `mipmap-hdpi`, `mipmap-xhdpi`, `mipmap-xxhdpi`, and `mipmap-xxxhdpi`.
3. **Configure `app/src/main/AndroidManifest.xml`**:
   - Declare **only** offline-compatible permissions (Notification, Media/Photos, and Boot Completed — no `INTERNET` or `REQUEST_INSTALL_PACKAGES`):
     ```xml
     <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
     <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
     <uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" />
     <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
     <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
     ```
   - Register `MainActivity`, `DailyReminderReceiver`, `ReminderNotificationActionReceiver`, and `ReminderBootReceiver` under `com.charan.tracmio`.

---

### PHASE 4: Install & Verify `app/src/main/assets/index.html`
Place the updated `index.html` into `app/src/main/assets/index.html` and verify these exact UI and functional behaviours:
1. **Brand Header & Welcome Screen (`#tracmioWelcomeOverlay`)**:
   - Topbar and Welcome Overlay display the 3D Liquid Glass Tracmio icon and exact logo-matching wordmark (`Trac` in metallic white-silver + `mio` in glowing emerald-green with the custom SVG leaf over the dotless `ı`), with subtitle `FINANCE & EXPENSES TRACKER`.
   - First-time users see the animated Welcome Overlay (`@keyframes tracmioLogoFloat`, `@keyframes tracmioSonarPulse`, `@keyframes tracmioShimmerSweep`) with name input and default currency picker.
2. **5-Tab Bottom Navigation Dock**:
   - Shows only `Home`, `Accounts`, `+`, `Transactions`, and `More` (`Reports` is accessed via `More` → `Reports`).
3. **Add Transaction Modal (`#overlay-txn`) & Custom UPI App Creator**:
   - `initFinTrackTheme()` executes before `buildTypeGrid()` so the 5-card Transaction Type Grid (`Income`, `Expense`, `Transfer`, `Borrow`, `Lend`) always renders at the top of the modal.
   - Selecting `Payment Method = UPI` reveals `#txnUpiField` with the **`+ Add UPI App`** button (`openCreateUpiApp()`) and **`＋ Create / Add New UPI App...`** option in `#txnUpiApp` so users can create and save custom UPI apps into `state.upiApps`.
4. **Transactions Page (`#page-transactions`) & Reports Page (`#page-reports`)**:
   - Date filter inputs (`#txnFromFilter`, `#txnToFilter`, `#reportFromDate`, `#reportToDate`) display high-contrast visible placeholders (`Select start date` / `Select end date`).
   - Reports page features the **4-Style Chart Switcher** (`📊 3D Neon Bars`, `🌊 Area Wave`, `💊 Capsules & Polar`, `⚡ Executive`), **4 Color Theme Palettes** (`Aurora`, `Tracmio Green`, `Sunset`, `Sapphire`), and the `Period Transactions` list.
5. **Settings Page (`#page-settings`)**:
   - **Appearance**: 10 built-in Liquid Glass themes (`Theme 1: Aurora Teal` through `Theme 10: Deep Ocean Aqua`) + **Custom Neon Theme Studio** (`#customPrimaryColor`, `#customSecondaryColor`), custom background photo (`#customBgLayer` rendered via `applyFinTrackBackground(dataUrl)` before `localStorage.setItem`), live `Photo visibility` slider (`0%–100%`), and live `App UI size (Increase / Decrease)` slider (`80%–130%` + `−`/`+` buttons).
   - **Data Management**: `Export Data` (JSON), `Import Data` (JSON), unified **`Export Transactions`** (`openExportDialog('settings')` for date-range `.xlsx` or `PDF`), and `Reset All Data`.
   - **Privacy**: Only `Local Only` (`All financial records stay on this device`) and `Internet Connection` (`Not required`). `"Cloud Sync / Disabled"` is completely removed.

---

### PHASE 5: Wire `MainActivity.kt` WebView & Native Bridge (`TracmioBridge`)
Update `MainActivity.kt` so the `WebView` and `TracmioBridge` implement all native bindings called by `index.html`:
1. **WebView Setup & Dual Bridge Binding**:
   ```kotlin
   val bridge = TracmioBridge()
   webView.addJavascriptInterface(bridge, "TracmioBridge")
   webView.addJavascriptInterface(bridge, "FinTrackBridge")
   webView.loadUrl("file:///android_asset/index.html")
   ```
2. **Window Insets & Hardware Back Navigation**:
   - Pass status bar, navigation bar, and keyboard (IME) insets to `window.finTrackApplyInsets(left, top, right, bottom, imeBottom)`.
   - Route Android back presses to `window.finTrackHandleBack()` so open modals, the welcome screen, or subpages close cleanly before exiting the app.
3. **Runtime Permission Methods (`@JavascriptInterface`)**:
   - `requestNotificationPermission()`: Requests `POST_NOTIFICATIONS` on Android 13+ (API 33+).
   - `requestMediaPermission()`: Requests `READ_MEDIA_IMAGES` (API 33+) or `READ_EXTERNAL_STORAGE` (API <= 32).
   - `requestAllPermissions()`: Requests both Notification and Media permissions on first launch and when triggered from JS.
4. **Custom Background Photo Picker (`@JavascriptInterface fun chooseBackgroundImage()`)**:
   - Opens the system image picker (`GetContent("image/*")`), downscales the selected photo to max `960px` JPEG (`quality = 60`), encodes it as `data:image/jpeg;base64,...`, stores a backup copy in `filesDir`, and calls:
     ```kotlin
     webView.evaluateJavascript("window.setFinTrackBackgroundFromNative(${JSONObject.quote(dataUrl)});", null)
     ```
5. **Backup, Excel & PDF Print Bridges (`@JavascriptInterface`)**:
   - `exportBackup(json: String, fileName: String)`: Saves JSON via `CreateDocument("application/json")` (default `tracmio_backup_<date>.json`).
   - `importBackup()`: Reads JSON via `OpenDocument` and calls `window.__importBackupFromAndroid(jsonText)`.
   - `exportWorkbook(json: String, fileName: String)`: Builds the `.xlsx` file via `XlsxExporter.create(json)` and saves via `CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")`.
   - `printDocument(html: String, jobName: String)`: Renders `html` into a print `WebView` and invokes Android's `PrintManager` so the user can save or print the `TRACMIO · OFFLINE FINANCE MANAGER` PDF report.

---

### PHASE 6: Daily Offline Reminder & Rotating Meme Notification Engine
Update `DailyReminderReceiver.kt`, `TracmioReminderScheduler.kt`, `ReminderNotificationActionReceiver.kt`, and `ReminderBootReceiver.kt`:
1. **Notification Channel**:
   - Channel ID: `"tracmio_daily"`, Channel Name: `"Tracmio daily reminder"`.
2. **Rotating Meme Notifications (`memeMode = true`)**:
   - Rotate sequentially through `last_meme_index` in `SharedPreferences("fintrack_reminders", MODE_PRIVATE)`:
     1. `"Your wallet called. It wants today's transactions logged 💸 Add your income and expenses!"`
     2. `"Money moved? Tell Tracmio about it before you forget 👀 Add a transaction now."`
     3. `"Your budget is waiting for its daily update 📊 Log today's income and expenses."`
     4. `"UPI went 'Ting!' 5 times today 📱 Did Tracmio hear about that Swiggy order?"`
     5. `"Salary came in like a lion and left like a lamb 🦁➡️🐑 Track where it went today!"`
     6. `"Checking bank balance without logging expenses is horror movie behavior 👻 Log now!"`
3. **Standard Daily Notifications (`memeMode = false`)**:
   - Rotate sequentially through:
     1. `"Your daily money check-in is ready. Add today's income and expenses to keep your balance accurate."`
     2. `"A one-minute update helps you stay on top of your money. Log today's transactions in Tracmio."`
     3. `"Small daily updates make monthly reports useful. Add any income or expenses you made today."`
4. **Notification Quick Actions**:
   - Add two action buttons (`Add income` with `EXTRA_QUICK_ACTION = "income"` and `Add expense` with `EXTRA_QUICK_ACTION = "expense"`) that open `MainActivity` and invoke `window.finTrackHandleNotificationAction(action)`.

---

### PHASE 7: Final Verification Checklist
Run and verify all of the following before completing the task:
1. `grep -rn "Cloud Sync" app/src/main/assets/index.html` returns **0 matches**.
2. `grep -rn "Install Update from APK" app/src/main/assets/index.html` returns **0 matches**.
3. `./gradlew clean testDebugUnitTest assembleDebug` completes with `BUILD SUCCESSFUL`.
4. Confirm the generated APK is named `Tracmio-2.5.2-debug.apk` and launches directly into the Tracmio Welcome Screen and Dashboard.
```

## 📋 COPY-PASTE MASTER PROMPT END
