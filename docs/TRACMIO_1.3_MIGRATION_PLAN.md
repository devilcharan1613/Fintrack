# Tracmio 1.3 Migration Audit and Execution Plan

**Baseline:** `main` at `fb4a7f51308458e3e6683035cf128ebe507cdfe6`  
**Working branch:** `feature/tracmio-1.3-native-migration`  
**Target:** Tracmio 1.3, Kotlin + Jetpack Compose + Room  
**Decisions confirmed by owner:**
- Target application ID: `com.tracmio.app` (change from `com.charan.tracmio`)
- Data reconciliation: merge legacy HTML data and Room data
- Legacy recovery: on-device extraction from the existing installed app
- Encryption: harden encrypted backups first; do not migrate the Room database to encrypted storage in this phase
- Preserve all user data; no destructive migration or deletion

## Audit findings

1. The current production launcher is `com.tracmio.app.MainActivity`, which hosts the HTML application in a WebView.
2. A separate native Compose activity and app implementation exists, but the native UI is not feature-parity complete.
3. Room is present at schema version 7. Schema export is currently disabled.
4. Existing Kotlin financial calculators, repositories, account/transaction models, custom tracker models, backup/security components, and reminder receivers should be reused where possible.
5. `LegacyMigrationGuard.shouldImportLegacyJson()` currently skips legacy import when Room already has accounts or transactions. This is incompatible with merge semantics and must not be used as the final merge policy.
6. `NativeBackupService.restoreJson()` clears live Room tables before restore. It is a restore path, not a safe merge importer. Do not call it for migration reconciliation.
7. The existing backup encryption implementation uses AES-GCM and PBKDF2-HMAC-SHA256. Input validation and password handling need hardening. The live Room database is not proven encrypted.
8. Root-level debug APKs and `public/assets/aistudio` contain release/development artifacts. Cleanup is deferred until a recoverable backup and reference audit exist.
9. Latest inspected CI run on the baseline commit completed successfully, but it does not establish native feature parity or instrumentation coverage.

## Critical package-ID / on-device migration constraint

Changing the application ID from `com.charan.tracmio` to `com.tracmio.app` means Android treats the target as a different application. The new package cannot directly read the old package's private WebView data, SharedPreferences, or Room database.

Therefore the migration must be staged:
1. Ship/run a data-export bridge in the currently installed `com.charan.tracmio` app while it still has access to its private data.
2. Export a versioned, integrity-checked migration bundle through Android's Storage Access Framework or another explicit user-mediated handoff.
3. Install/run the `com.tracmio.app` target and import the bundle into a staging database.
4. Merge with target Room records using stable IDs, relationship validation, and duplicate detection.
5. Verify counts, relationships, and financial totals before marking migration complete.
6. Keep the old app and exported bundle until the user verifies the new app. Do not silently uninstall or clear the old app.

Do not change the application ID in the old installed build before its export path is available. A single APK cannot perform a normal in-place update across two different application IDs.

## Required next implementation stages

### Stage A - safe recovery/export
- Add a versioned migration bundle format and manifest.
- Export all relevant WebView storage sources and legacy bridge-accessible data.
- Inventory and export Room data if it is part of the old app's private storage.
- Include accounts, transactions, categories, budgets, goals/contributions, loans/payments, people/settlements, custom trackers and all tracker child records, settings, reminders, and attachment references.
- Include record IDs, timestamps, schema versions, and checksums.
- Do not include secrets or backup passwords in plaintext.
- Provide explicit SAF export and clear success/failure reporting.

### Stage B - merge/import
- Parse and validate bundle size, version, checksums, JSON types, IDs, amounts, and references.
- Import into a separate staging database or transactionally isolated staging tables.
- Merge both datasets; preserve unique records from either source.
- Treat matching IDs with differing payloads as conflicts; do not silently overwrite.
- Deduplicate only with defensible stable identifiers and relationship-aware rules.
- Ask the owner to resolve ambiguous financial conflicts.
- Verify foreign keys and compare per-account and aggregate totals before commit.
- Make import idempotent and rollback-safe.

### Stage C - native parity
- Complete Compose screens and workflows for dashboard, transactions, accounts, trackers, reports, budgets, goals, loans, people/settlements, settings, reminders, exports, backup/restore, and global amount privacy.
- Reuse existing domain calculations and repositories.
- Add loading, empty, error, and validation states.
- Keep WebView available only as a temporary legacy export surface until native acceptance tests pass.

### Stage D - hardening and cleanup
- Enable Room schema export and commit schema snapshots.
- Add non-destructive Room migration tests.
- Harden AES-GCM/PBKDF2 backup input validation and restore verification.
- Review manifest exported components, PendingIntent flags, file providers, logs, and permissions.
- Remove WebView production startup only after parity and migration acceptance tests pass.
- Perform asset cleanup only after references are checked and recoverable copies exist.
- Update version metadata and release workflow only after package-ID and upgrade behavior are validated.

## Safety gates

- Never run destructive Room migrations.
- Never clear live tables as part of merge import.
- Never switch the launcher before feature parity and migration tests.
- Never delete the old package's app data or legacy export assets before successful import verification.
- Never claim tests/builds passed unless executed by CI or a local Android build environment.
- Keep this branch separate from `main` until review and acceptance.

## Status

- [x] Repository baseline inspected.
- [x] Owner decisions recorded.
- [x] Recoverable migration branch created from baseline.
- [ ] Legacy on-device export bridge implemented and tested.
- [ ] Staging merge importer implemented and tested.
- [ ] Native feature parity completed.
- [ ] Launcher/package transition validated on a real upgrade path.
- [ ] Security hardening and asset cleanup completed.
- [ ] Unit/instrumentation tests and release build verified.
