# FinTrack Pro native migration decisions

Status: approved implementation contract for `fintrack-2.5.0-development`.

This document records the owner's confirmed decisions. It does not claim that the native migration has been implemented.

## Product constraints

- Product: FinTrack Pro Offline Edition.
- Preserve all existing offline features and user records.
- No internet permission, authentication, OTP, cloud sync, cloud backup, or remote services.
- Keep the existing HTML/CSS/JavaScript application and its assets intact as the migration source and rollback reference.
- Do not switch the launcher to Compose until every existing screen and workflow has a native implementation and passes regression tests.
- No fixed artificial backup-size limit. Handle large local backups without claiming infinite memory capacity.
- No app lock in this release.
- Minimum supported Android version remains API 23 (Android 6.0), with modern Android coverage as well.

## Confirmed data decisions

### Money representation

- Canonical persisted monetary unit for the native database: signed 64-bit integer paise.
- Convert legacy rupee `Double` values using `BigDecimal.valueOf(value).movePointRight(2).setScale(0, HALF_UP).longValueExact()`.
- Reject NaN, infinities, negative amounts where the domain requires non-negative money, and values that overflow a signed 64-bit paise amount.
- Do not use floating-point arithmetic for new financial calculations.
- Keep explicit decimal conversion only at UI/import/export boundaries.
- Migration must report row/table/value failures and roll back atomically. It must never silently drop or clamp an amount.

### Account balances

- Store opening balance separately from ledger activity.
- Derive current account balance from opening balance plus signed ledger entries.
- Transfers must be represented as a balanced pair of source and destination ledger entries sharing one stable transfer identifier.
- A transfer must not change total net worth or be counted as income/expense.
- Existing legacy account balance values must not be assumed to be opening balances until the migration can reconcile them against the legacy transaction history. Build a reconciliation report and use a deterministic conversion rule with a rollback snapshot.

### Backup restore

- Merge by stable record IDs and avoid duplicates.
- Restore in a single database transaction after validating schema/version, record identifiers, references, required fields, monetary values, and integrity metadata.
- Existing local records must not be erased by a failed or partial restore.
- Preserve relationships and original IDs where possible. Resolve ID collisions explicitly; do not silently overwrite unrelated local records.
- Keep local JSON backup/restore. Cloud backup is out of scope for the offline edition.
- No arbitrary fixed file-size cap. Use streaming/chunked IO where practical and surface storage/RAM limitations honestly.

### Loan repayments and savings contributions

- Repayments and savings contributions remain separate tracking records.
- They do not create ledger entries or change account balances automatically.
- A user may separately record a corresponding income/expense/transfer transaction.
- Do not double count these records in cash flow or account balances.

### App lock

- No biometric, PIN, or app lock in this release.

### Migration rollout

- Implement all existing screens and workflows in Compose before switching the launcher.
- Until feature parity is proven, retain the existing WebView application as the active app and as a data migration source.
- Never run a destructive migration on the user's only copy of local data.
- The final launcher switch must be a separate, reviewable change after migration and regression gates pass.

### Versioning

- The native migration will use a new version name and a higher version code than the current 2.5.0 / 27.
- Chosen native release version: 3.0.0 / 28, unless repository release history reveals a higher code already used.
- Verify application ID and signing lineage before claiming in-place upgrade compatibility. A changed signing key can prevent upgrade even when the package name matches.

## Required implementation gates

1. Inventory and preserve every existing screen, data field, action, export, reminder, and navigation path.
2. Define Room entities and explicit, tested migrations. Do not edit a persisted entity without a matching Room schema migration.
3. Migrate all money-bearing tables to paise: accounts, transactions, budgets, goals, loans, loan payments, and any additional money fields discovered in source.
4. Add stable UUIDs or another deterministic stable key for records currently identified only by auto-increment IDs. Preserve legacy IDs in migration metadata.
5. Add an account opening-balance field and a ledger reconciliation/migration strategy.
6. Implement transaction-safe repositories, including atomic paired transfers.
7. Implement native Compose screens and ViewModels for every existing workflow.
8. Implement offline JSON backup/restore merge, CSV, XLSX, and PDF export.
9. Implement native reminders and reboot rescheduling without internet access.
10. Add unit, Room migration, backup/restore, financial, navigation, UI, and offline tests.
11. Verify a clean Gradle build, APK package/version metadata, install/upgrade path, and emulator/device launch before switching the launcher.

## Current source risks observed during inspection

- The active launcher is still `MainActivity` hosting a WebView that loads `app/src/main/assets/index.html`.
- Room account and transaction entities currently store monetary values as `Double`.
- Budget, goal, loan, and loan-payment entities also contain monetary `Double` fields.
- The current account balance query adds `accounts.balance` to transaction deltas. It does not yet implement the approved opening-balance/ledger model.
- The Kotlin repository export/import code must be audited and replaced with a structured JSON parser and stable-ID merge before it is used for production restore.
- Room currently declares schema version 3. New schema changes require explicit migrations and migration tests.

These are findings from the inspected development-branch source, not claims that the listed components are already migrated.

## Release acceptance

The native launcher must not be enabled until:
- all listed feature workflows have Compose implementations;
- migration tests prove record counts, IDs, relationships, and monetary totals are preserved;
- legacy and native financial summaries reconcile;
- restore merge is idempotent;
- transfers are balanced and excluded from income/expense totals;
- repayments and savings contributions remain tracking-only;
- all offline tests pass with no network permission;
- the APK installs and launches on API 23+ test coverage and a modern Android target;
- a verified debug APK and a truthful test report are available.
