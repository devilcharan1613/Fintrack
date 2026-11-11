# Master AI Engineering Prompt — Convert & Rebrand Repository to **Tracmio** (`com.charan.tracmio`)

Copy and paste the prompt below directly into your AI coding assistant (**Cursor**, **Claude Code**, **GitHub Copilot Workspace**, **Windsurf**, or **Codex**) inside your repository ([devilcharan1613/Fintrack](https://github.com/devilcharan1613/Fintrack/) / [PR #3](https://www.google.com/search?q=https://github.com/devilcharan1613/Fintrack/pull/3)) after placing the provided `index.html` at `app/src/main/assets/index.html` and the **Tracmio** logo (`76767.png`) in `app/src/main/res/mipmap-*/` and `app/src/main/assets/tracmio_logo.png`.

---

## 📋 COPY-PASTE MASTER PROMPT START

```markdown
You are a Principal Android & Full-Stack Hybrid Systems Engineer. Your task is to completely rebrand and upgrade this Android repository from **FinTrack** (`com.charan.fintrack`) to **Tracmio — Finance & Expenses Tracker** (`com.charan.tracmio`), rename all branches, packages, classes, resources, and notification channels from `FinTrack`/`fintrack` to `Tracmio`/`tracmio`, and wire the exact provided `app/src/main/assets/index.html` single-page application into a 100% working, offline-first Android application.

Execute every step below systematically and verify that `./gradlew clean testDebugUnitTest assembleDebug` passes with zero errors.

---

### STEP 1: Repository, Branch, and Gradle Project Rebranding (`FinTrack` → `Tracmio`)

1. **Git Branch & Repository Rename**:
   - Rename development branch `fintrack-2.5.0-development` to `tracmio-2.5.2-development`:
     ```bash
     git branch -m fintrack-2.5.0-development tracmio-2.5.2-development
     ```
   - Update `settings.gradle.kts` (or `settings.gradle`):
     ```kotlin
     rootProject.name = "Tracmio"
     include(":app")
     ```
2. **Application ID, Namespace & Version (`app/build.gradle.kts`)**:
   - Set `namespace = "com.charan.tracmio"` and `applicationId = "com.charan.tracmio"`.
   - Set `versionCode = 29` and `versionName = "2.5.2"`.
   - Rename APK output filenames from `FinTrack-*` to `Tracmio-2.5.2-debug.apk` / `Tracmio-2.5.2-release.apk`.
3. **Package Directory & Identifier Refactoring**:
   - Move `app/src/main/java/com/charan/fintrack/` to `app/src/main/java/com/charan/tracmio/` (and matching `src/test/` and `src/androidTest/` directories).
   - Rename all classes, files, preferences, and database identifiers from `FinTrack*` to `Tracmio*`:
     - `FinTrackDatabase` → `TracmioDatabase` (DB name `"tracmio_database"`, preserving migration read fallback from `"fintrack_database"` if an older database file exists on device)
     - `FinTrackBridge` → `TracmioBridge` (registering BOTH `"TracmioBridge"` and `"FinTrackBridge"` in `webView.addJavascriptInterface(...)` for backward compatibility)
     - `FinTrackReminderScheduler` → `TracmioReminderScheduler`
     - `NativeFinTrackActivity` → `NativeTracmioActivity`
     - `NativeFinTrackApp` → `NativeTracmioApp`
     - `NativeFinTrackViewModel` → `NativeTracmioViewModel`
     - `FinTrackNativeTheme` → `TracmioNativeTheme`
   - Update `app/src/main/res/values/strings.xml`:
     ```xml
     <resources>
         <string name="app_name">Tracmio</string>
         <string name="app_tagline">Finance &amp; Expenses Tracker</string>
     </resources>
     ```
   - Update app launcher icons (`ic_launcher.png`, `ic_launcher_round.png`, `ic_launcher_foreground.png`) using the 3D Liquid Glass **Tracmio** logo (`76767.png`).

---

### STEP 2: `AndroidManifest.xml` — Offline-First Permissions & Runtime Components

Update `app/src/main/AndroidManifest.xml` so that:
1. It declares **zero network permissions** (no `android.permission.INTERNET` and no `ACCESS_NETWORK_STATE`).
2. It declares **Notification** and **Media / Photos** permissions for runtime prompting across Android 6–15 (API 23–35):
   ```xml
   <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
   <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
   <uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" />
   <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
   <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
   ```
3. It removes `REQUEST_INSTALL_PACKAGES` and any APK self-update installer provider, as the in-app "Install Update from APK" option has been removed.
4. It registers `MainActivity` (`exported="true"`, `launchMode="singleTop"`, `windowSoftInputMode="adjustResize"`), `DailyReminderReceiver` (`exported="false"`), `ReminderNotificationActionReceiver` (`exported="false"`), and `ReminderBootReceiver` (`exported="true"` with `ACTION_BOOT_COMPLETED`).

---

### STEP 3: Exact `app/src/main/assets/index.html` Integration & Required UI/UX Specifications

Place the provided `index.html` into `app/src/main/assets/index.html` verbatim. Verify that all of the following features in `index.html` are preserved intact and never overwritten:
1. **Complete Tracmio Branding & 3D Animated Welcome Screen (`#tracmioWelcomeOverlay`)**:
   - Topbar and Welcome Screen display the 3D Liquid Glass **Tracmio** logo (`76767.png`) and the exact logo-matching italic wordmark (`Trac` in metallic white-silver gradient + `mio` in glowing emerald-green gradient with the custom SVG leaf over the dotless `ı`), plus subtitle `FINANCE & EXPENSES TRACKER`.
   - New users (when `localStorage.getItem('tracmio_welcomed')` is unset and no accounts exist) automatically see the animated `#tracmioWelcomeOverlay` with 3D logo levitation (`@keyframes tracmioLogoFloat`), pulsing sonar rings (`@keyframes tracmioSonarPulse`), specular shimmer sweep (`@keyframes tracmioShimmerSweep`), personalized `Your Name` input (`state.settings.userName`), and `Default Currency` selector.
   - Tapping the top-left Tracmio logo or `"✨ Preview Tracmio Welcome & Logo Animation"` in Settings replays the Welcome Screen anytime.
2. **5-Tab Floating Glass Bottom Navigation Dock**:
   - Contains **only 5 items**: `Home` (`dashboard`), `Accounts` (`accounts`), `+` (`openModal('txn')`), `Transactions` (`transactions`), and `More` (`more`). (`Reports` is removed from the bottom dock and accessed via `More` → `Reports`).
3. **Add Transaction Modal (`#overlay-txn`) & Custom UPI App Creator**:
   - `initFinTrackTheme()` is defined and called before `buildTypeGrid()` so the 5-card Transaction Type Grid (`Income`, `Expense`, `Transfer`, `Borrow`, `Lend` with 3D volumetric SVG icons) always renders at the top of `#overlay-txn`.
   - When `Payment Method = UPI` is selected, `#txnUpiField` shows a **`+ Add UPI App`** button (`openCreateUpiApp()`) and a **`＋ Create / Add New UPI App...`** option in `#txnUpiApp` that opens `#txnCustomUpiWrap` (`#txnCustomUpiApp` + **`+ Save App`** button `saveCustomUpiAppToList()`), saving user-created UPI apps persistently into `state.upiApps`.
4. **Transactions Page (`#page-transactions`) & Reports Page (`#page-reports`)**:
   - High-contrast visible date placeholders (`data-placeholder="Select start date"` / `"Select end date"`) on `#txnFromFilter`, `#txnToFilter`, `#reportFromDate`, and `#reportToDate` so empty `<input type="date">` fields never appear blank on Android WebView.
   - Reports page includes the live **4-Style Chart Switcher** (`📊 3D Neon Bars`, `🌊 Area Wave`, `💊 Capsules & Polar`, `⚡ Executive` via `setReportVisualStyle()`) and **4 Color Theme Palettes** (`Aurora`, `Tracmio Green`, `Sunset`, `Sapphire` via `setReportThemePalette()`), plus the `Period Transactions` list (`#repTransactionsList`).
5. **Settings Page (`#page-settings`)**:
   - **10 Built-In Liquid Glass Themes**: `Theme 1: Aurora Teal` (`dark`), `Theme 2: Tracmio Neon` (`tracmio`), `Theme 3: Obsidian Gold` (`obsidian`), `Theme 4: Sapphire Prism` (`sapphire`), `Theme 5: Arctic Pearl` (`light`), `Theme 6: Cyber Ruby` (`ruby`), `Theme 7: Matrix Emerald` (`emerald`), `Theme 8: Amethyst Nebula` (`amethyst`), `Theme 9: Titanium Amber` (`titanium`), `Theme 10: Deep Ocean Aqua` (`ocean`), plus the **Custom Neon Theme Studio** (`#customPrimaryColor`, `#customSecondaryColor`, `applyCustomFinTrackTheme()`).
   - **Working Custom Background Photo Layer (`#customBgLayer`) & Live Sliders**: `setFinTrackBackgroundFromNative(dataUrl)` calls `applyFinTrackBackground(dataUrl)` **before** `localStorage.setItem` so custom photos always render behind the translucent frosted glass UI (`html.has-custom-photo`). All 68 inline event handlers are exported to `window.*` and direct `input`/`change` listeners are wired on `#backgroundVisibility` (`0%–100%`) and `#appIconSize` (`App UI size (Increase / Decrease)` `80%–130%` + `−`/`+` buttons).
   - **Cleaned Settings Cards**:
     - Under `Daily Offline Reminder`: Ends cleanly at `Save Reminder Settings` (no test meme UI block inside Settings).
     - Under `Data Management`: Contains `Export Data` (JSON), `Import Data` (JSON), **`Export Transactions`** (`openExportDialog('settings')` for date-range `.xlsx` or `PDF`), and `Reset All Data` (`Export Excel Workbook`, `Export Date-Range Excel`, and `App Update / Install Update from APK` are removed).
     - Under `Privacy`: Contains only `Local Only` and `Internet Connection: Not required` (`Cloud Sync / Disabled` is removed).

---

### STEP 4: Native Android WebView Bridge (`MainActivity.kt`) & Runtime Permissions

In `MainActivity.kt`, configure the `WebView` and `@JavascriptInterface` bridge (`TracmioBridge`) so every function in `app/src/main/assets/index.html` works natively on Android:

1. **WebView Configuration & Safe-Area / IME Insets**:
   - Enable `javaScriptEnabled = true`, `domStorageEnabled = true`, `allowFileAccess = true`.
   - Register the bridge under **both** names so all JS calls succeed:
     ```kotlin
     val bridge = TracmioBridge()
     webView.addJavascriptInterface(bridge, "TracmioBridge")
     webView.addJavascriptInterface(bridge, "FinTrackBridge")
     ```
   - Forward `ViewCompat.setOnApplyWindowInsetsListener` system bar and IME insets to `window.finTrackApplyInsets(left, top, right, bottom, imeBottom)`.
   - Wire `OnBackPressedCallback` to evaluate `window.finTrackHandleBack ? window.finTrackHandleBack() : false` before finishing the activity.
2. **Runtime Notification & Media Permission Launchers**:
   - Register `ActivityResultContracts.RequestMultiplePermissions()` and expose `@JavascriptInterface`:
     - `requestNotificationPermission()`: Requests `Manifest.permission.POST_NOTIFICATIONS` on Android 13+ (API 33+).
     - `requestMediaPermission()`: Requests `Manifest.permission.READ_MEDIA_IMAGES` (API 33+) or `Manifest.permission.READ_EXTERNAL_STORAGE` (API <= 32).
     - `requestAllPermissions()`: Requests both Notification and Media permissions together and also prompts on first app startup.
3. **Custom Background Image Picker (`chooseBackgroundImage`)**:
   - Launch `ActivityResultContracts.GetContent()` (`"image/*"`), decode and compress the chosen image to a base64 `data:image/jpeg;base64,...` URI (max dimension `960px`, quality `60`), save a local copy in `filesDir`, and invoke:
     ```kotlin
     webView.evaluateJavascript("window.setFinTrackBackgroundFromNative(${JSONObject.quote(dataUrl)});", null)
     ```
4. **Backup & Statement Exports (`exportBackup`, `importBackup`, `exportWorkbook`, `printDocument`)**:
   - `exportBackup(json: String, fileName: String)`: Launches `CreateDocument("application/json")` with default name `tracmio_backup_<date>.json` and writes `json`.
   - `importBackup()`: Launches `OpenDocument(arrayOf("application/json", "text/plain", "*/*"))`, reads the JSON text, and invokes `window.__importBackupFromAndroid(jsonText)`.
   - `exportWorkbook(json: String, fileName: String)`: Generates the multi-sheet `.xlsx` workbook via `XlsxExporter.create(json)` and saves it via `CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")` with default name `Tracmio_Transactions_<range>.xlsx`.
   - `printDocument(html: String, jobName: String)`: Uses Android's `PrintManager` + a print `WebView` (`createPrintDocumentAdapter(jobName)`) to print or save the formatted `TRACMIO · OFFLINE FINANCE MANAGER` statement as PDF.

---

### STEP 5: Daily Offline Reminder Scheduler & Rotating Meme Notifications (`DailyReminderReceiver.kt`)

In `DailyReminderReceiver.kt` and `TracmioReminderScheduler.kt`:
1. Use notification channel ID `"tracmio_daily"` (name: `"Tracmio daily reminder"`).
2. When `memeMode == true` (`fintrack_reminders` SharedPreferences), rotate sequentially through `last_meme_index` using these 6 fun/meme notifications:
   - `"Your wallet called. It wants today's transactions logged 💸 Add your income and expenses!"`
   - `"Money moved? Tell Tracmio about it before you forget 👀 Add a transaction now."`
   - `"Your budget is waiting for its daily update 📊 Log today's income and expenses."`
   - `"UPI went 'Ting!' 5 times today 📱 Did Tracmio hear about that Swiggy order?"`
   - `"Salary came in like a lion and left like a lamb 🦁➡️🐑 Track where it went today!"`
   - `"Checking bank balance without logging expenses is horror movie behavior 👻 Log now!"`
3. When `memeMode == false`, rotate through the 3 standard check-in notifications:
   - `"Your daily money check-in is ready. Add today's income and expenses to keep your balance accurate."`
   - `"A one-minute update helps you stay on top of your money. Log today's transactions in Tracmio."`
   - `"Small daily updates make monthly reports useful. Add any income or expenses you made today."`
4. Attach the two interactive notification action buttons (`Add income` with `EXTRA_QUICK_ACTION = "income"` and `Add expense` with `EXTRA_QUICK_ACTION = "expense"`) which launch `MainActivity` and invoke `window.finTrackHandleNotificationAction(action)`.
```

## 📋 COPY-PASTE MASTER PROMPT END
