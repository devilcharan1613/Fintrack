# Tracmio 1.3 Migration Audit and Execution Plan

**Baseline:** `main` at `fb4a7f51308458e3e6683035cf128ebe507cdfe6`  
**Working branch:** `feature/tracmio-1.3-native-migration`  
**Target:** Tracmio 1.3, Kotlin + Jetpack Compose + Room

## Owner decisions
- Requested target application ID: `com.tracmio.app`
- Data reconciliation: merge legacy HTML data and Room data
- Legacy recovery: on-device extraction from the existing installed app
- Encryption: harden encrypted backups first; do not migrate Room to encrypted storage in this phase
- Preserve all user data; no destructive migration or deletion

## Audit findings
1. The production launcher is `com.tracmio.app.MainActivity`, which hosts the HTML application in a WebView.
2. A separate native Compose activity and app implementation exists, but the native UI is not feature-parity complete.
3. The inspected `app/build.gradle.kts` already sets both `namespace` and `applicationId` to `com.tracmio.app`. Thus the requested target ID is already configured in the current repository source. The package ID actually installed on the user's device must still be checked before release; source configuration alone cannot prove the installed APK identity.
4. Room is present at schema version 7. Schema export is currently disabled.
5. Existing Kotlin financial calculators, repositories, account/transaction models, custom tracker models, backup/security components, and reminder receivers should be reused where possible.
6. `LegacyMigrationGuard.shouldImportLegacyJson()` currently skips legacy import when Room already has accounts or transactions. This is incompatible with merge semantics and must not be used as the final merge policy.
7. `NativeBackupService.restoreJson()` clears live Room tables before restore. It is a restore path, not a safe merge importer. Do not call it for migration reconciliation.
8. The existing backup encryption implementation uses AES-GCM and PBKDF2-HMAC-SHA256. Input validation and password handling need hardening. The live Room database is not proven encrypted.
9. Root-level debug APKs and `public/assets/aistudio` contain release/development artifacts. Cleanup is deferred until a recoverable backup and reference audit exist.
10. The latest inspected CI run on the baseline commit completed successfully, but it does not establish native feature parity or instrumentation coverage.

## Critical package-ID / on-device migration constraint
The current repository source already targets `com.tracmio.app`. If the installed legacy app on the user's device has a different package ID, Android isolates its private WebView data, SharedPreferences, and Room database. The new package cannot directly read that private data.

Before release, inspect the installed app's actual package ID and version. If it differs from `com.tracmio.app`, migration must be staged:
1. Keep the legacy app installed and operational.
2. Add/run an export bridge in the legacy app while it still has access to its private data.
3. Export a versioned, integrity-checked migration bundle through Android's Storage Access Framework.
4. Install/run the `com.tracmio.app` target and import the bundle into a staging database.
5. Merge with target Room records using stable IDs, relationship validation, and duplicate detection.
6. Verify counts, relationships, and financial totals before marking migration complete.
7. Keep the old app and exported bundle until the user verifies the new app. Do not silently uninstall or clear the old app.

If the installed app already uses `com.tracmio.app`, an in-place package upgrade may be possible, subject to signing-key compatibility and a verified app-data backup. Do not assume this without checking the actual installed APK.

## Required implementation stages

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


## Initial recovery exporter implementation

The migration branch now includes an initial, user-triggered recovery export path:

- The existing encrypted-backup panel has a separate **Export Full Migration Recovery Bundle** action.
- The page captures the current JavaScript app state and every WebView localStorage key/value.
- It attempts to enumerate and export IndexedDB databases and object stores when the WebView supports `indexedDB.databases()`.
- The Android bridge takes a read-only snapshot of every non-internal SQLite table in the existing Room database, preserving column names, values, blobs (Base64), and the SQLite user version.
- The bundle records the running source package name and export timestamp, adds a SHA-256 payload digest, and is encrypted using the existing AES-GCM/PBKDF2 backup envelope before the user chooses a destination with SAF.
- The exporter does not delete, update, or migrate any source data. It enforces a 20-million-character legacy payload cap and 50 MB final bundle cap.
- The CI workflow now includes the migration branch so the new code can be compiled by GitHub Actions.

### Known limitations of this first exporter pass

- The bundle has not yet been compiled or exercised on a physical Android device; do not use it as the only recovery copy until CI and device verification pass.
- The source package field records the package of the running build, but this does not establish which package is currently installed on the owner's phone.
- Attachment database rows and paths are captured, but referenced app-private attachment files are not yet embedded in the bundle. A follow-up pass must copy those files safely and map them into the target app's private storage.
- IndexedDB extraction depends on WebView support for database enumeration. The bundle records whether that API was available; it must not be assumed complete if unsupported.
- The bundle importer and deterministic merge engine are not implemented yet. The encrypted bundle is a recovery artifact only, not an import-ready migration.
- The existing Room database is still schema version 7 with schema export disabled. No Room schema migration was made by this exporter.
- Existing backup encryption parameters and password cleanup still need dedicated security hardening; this implementation reuses the existing encryption engine rather than claiming it is fully hardened.


## Recovery exporter follow-up

- Tracker attachment payloads are now copied into the encrypted bundle when their canonical paths resolve inside the app-private `files/tracker_attachments` directory.
- Attachment file bytes are embedded as Base64 with filename, MIME type, size, stable attachment UID, and SHA-256 digest. Each file is capped at 8 MB and total attachment bytes at 24 MB.
- Paths outside the expected app-private attachment directory are not read. This blocks arbitrary file path traversal from database content.
- Backup encryption now uses 310,000 PBKDF2-HMAC-SHA256 iterations for new exports. Decryption remains compatible with existing 100,000-iteration V1 backups, validates KDF bounds and salt/IV/ciphertext lengths, and clears password/key/plaintext byte buffers after use.
- A unit test now checks password-buffer clearing. CI must pass after these latest changes before any device test.

- Attachment payloads are now embedded from app-private storage with per-file and aggregate limits; paths outside the attachment root are excluded.
- Encryption work factor increased for new exports, legacy V1 decryption retained, and sensitive byte/password buffers are cleared in finally blocks.
- The attachment exporter now emits an explicit warning record for every skipped or inaccessible attachment, including missing files, unsafe paths, invalid paths, empty files, per-file/aggregate size limits, and URI-only records. The bundle includes `attachmentExportComplete`, warning details, and original content-URI metadata; partial recovery is no longer silently represented as complete.
- Added a read-only `MigrationBundleValidator` that verifies envelope/payload versions, SHA-256 payload integrity, attachment Base64, byte sizes, per-file/aggregate limits, and per-attachment SHA-256 checksums before any future importer can stage data.
- Added JVM tests for valid bundle validation, partial attachment warnings, payload tampering, and attachment checksum mismatch. These tests still require a successful CI run.

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
- [x] Target application ID confirmed in current Gradle source as `com.tracmio.app`.
- [ ] Actual installed package ID and signing compatibility verified.
- [x] Initial on-device encrypted recovery export bridge implemented (runtime verification still pending).
- [ ] Staging merge importer implemented and tested.
- [ ] Native feature parity completed.
- [ ] Launcher/package transition validated on a real upgrade path.
- [ ] Security hardening and asset cleanup completed.
- [ ] Unit/instrumentation tests and release build verified.
