# FinTrack v2.5.0 Native Migration Decisions

Baseline branch: `fintrack-2.5.0-development`
Baseline commit: `3c1590ed5bfe79b8d8d67f7bb0c7c3dd65a8c73b`
Application ID: `com.charan.fintrack`
Current version: `2.5.0 (27)`

## User-approved product decisions

- Offline-only. No Internet permission, cloud, OTP, sign-in, or remote sync.
- Native Kotlin + Jetpack Compose is the target UI. WebView must not remain the main UI.
- Room/SQLite is the canonical runtime store after migration.
- Money is stored as signed 64-bit integer paise. Convert legacy decimal rupee values with decimal arithmetic and an explicit HALF_UP paise rounding rule; reject overflow and invalid/non-finite values.
- Loan repayments update loan repayment history only. They do not create an account expense or alter account balances.
- Savings contributions are transfers from a selected account into a savings account. Both sides must be committed atomically and linked by one transfer identifier.
- Restore policy is MERGE. Validate the entire backup before writing, preserve existing records, use stable source IDs/natural keys to prevent duplicates, remap foreign keys transactionally, and roll back the whole import on failure.
- No app lock.
- Migration from the legacy HTML application's localStorage is manual: user exports/selects a JSON backup and explicitly imports it. Do not silently clear WebView data or claim automatic localStorage access after removing WebView.
- All existing screens and workflows are in scope.
- First QA gate is a full interaction regression across all visible controls and navigation.

## Existing source preservation

The legacy HTML/CSS/JavaScript application remains tracked in Git at the baseline commit above. Keep it intact throughout migration. Do not delete or rewrite `app/src/main/assets/index.html` until native feature parity, manual backup import, and migration regression tests are demonstrated. Do not change the main branch as part of this work.

## Current architecture facts from baseline

- `MainActivity.kt` creates a WebView and calls `setContentView(webView)`.
- `index.html` remains the active application UI and uses browser localStorage.
- Compose and Room dependencies are configured, but no native Compose navigation root is wired into MainActivity.
- Room entities currently persist monetary values as REAL/Double.
- Room database schema is version 3 and has migrations 1->2 and 2->3.
- Room repository restore currently uses hand-written JSON parsing and does not safely restore all loan payment records.
- The existing Room database is not currently the active source of truth for the UI.

## Migration gates

1. **Baseline and preservation:** retain this Git baseline; no destructive changes to legacy source.
2. **Money model:** add paise conversion and arithmetic tests before changing Room schema.
3. **Schema migration:** create a new Room schema version with explicit migrations from every shipped schema. Never use destructive migration. Preserve IDs and relationships.
4. **Backup merge:** versioned JSON parser, validation, duplicate detection, ID remapping, and a single Room transaction. Add malformed, duplicate, nested, large-file, and rollback tests. No artificial byte cap.
5. **Repositories and ViewModels:** one Room-backed data path; all writes use repository transactions and Flow observations.
6. **Native UI parity:** Compose destinations for Dashboard, Accounts, Transactions, Reports, More, categories, budgets, goals, people, borrowing/lending, loans, reminders, backup/export, and settings. Every control must have a tested action.
7. **Accounting invariants:** transfers net to zero across accounts; loan repayments are history-only; savings contributions are linked account transfers; no double counting in income/expense/net-worth reports.
8. **Offline/security:** no Internet permission or network dependency; safe file import/export via Storage Access Framework; no secrets or financial data in logs.
9. **Release verification:** Gradle unit tests, Room migration tests, Compose UI tests, APK metadata/package checks, install/launch and workflow tests on an emulator/device where available.

## Acceptance criteria

- No WebView as the main app.
- No data loss and no automatic deletion of legacy records.
- Paise-based financial persistence and calculations.
- Merge restore with duplicate protection and rollback.
- All existing screens and functions preserved.
- Offline operation without Internet permission.
- All visible controls and navigation verified by tests.
- Debug APK and release signing instructions delivered.
- Report distinguishes implemented, built, automated-tested, and device-tested features.

## Scope intentionally not claimed by this commit

This file records the agreed migration contract. It does not claim that the Compose migration, Room schema conversion, feature parity, or device QA is complete.
