# Changelog

All notable changes to SakuMe are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/), and versions follow SemVer.

## [2.0] - 2026-11-22

A major release: encrypted storage and backups, a full redesign of Home, Bills and the navigation, and smarter backup cleanup.

### Security
- **SQLCipher**: the Room database (`sakume.db`) is now encrypted at rest with SQLCipher. A random 32-byte passphrase is stored in EncryptedSharedPreferences (Android Keystore).
- **Backup encryption**: all automatic / internal / exported backups are encrypted with AES-256-GCM (`SAKUMEENC1` format). The key is the same device master key.
- **Portable backup**: `BackupManager.buildPortableEncrypted(password)` is available — AES-256-GCM + PBKDF2-HMAC-SHA256 (120k iterations) so a backup can be restored on another device with a password.
- **Legacy plain JSON** backups can still be imported (backward compatible).
- **Automatic migration**: existing installs with a plain database are detected, the data is snapshotted to a safety backup, the database is deleted, and the data is re-imported into the encrypted database. The migration flag is stored in SecureStore.
- The master key is never written to disk in plain form; the SharedPreferences fallback is used only if the Keystore fails completely.

### Added

**Navigation & quick add**
- Center **+** button in the bottom bar. It rotates into an × and opens a quick-add menu with six actions: Transaction, Transfer, Bill, Investment, Savings goal and Receivable / Debt. Each action opens the existing add dialog.
- The Invest tab has a third segment, **Receivable / Debt**. The tab label and icon follow the selected segment (Savings, Investments, Receivable).

**Home**
- Settings icon in the top-right corner of the Home hero; it opens Settings.
- The **In** and **Out** tiles are tappable and open the history filtered to this month's income or expenses (the same figures shown on Home, excluding transfers). The **Cash** tile opens the cash history.
- The In and Out icons are green and red.
- Tapping the total balance hides / shows all amounts.
- Collapsed top bar while scrolling: tap the amount to hide / show amounts, tap the arrow to scroll back to the top.

**Bills**
- Summary hero: total unpaid, number of bills, number overdue and the nearest due bill (follows the provider filter).
- Day badge on every bill (days left, due today or days overdue), plus an installment progress bar for multi-month bills.
- Paid history is always visible at the bottom of the screen.

**Backup**
- Automatic cleanup of old backups by age (see Changed → Backup).

**Security helpers & API**
- `SecureStore` — device master key management
- `SecureCrypto` — AES-GCM + PBKDF2 helpers and encrypted-format detection
- `BackupCodec.encodeEncrypted` / `decodeMaybeEncrypted` (device key or password)
- `BackupManager.buildPortableEncrypted`
- `FinanceRepository.importJson(context, json, password?)`
- Dependencies: `net.zetetic:sqlcipher-android:4.6.1`, `androidx.security:security-crypto`

### Changed

**Home (full redesign)**
- New compact black-and-white layout. The hero shows the **SakuMe** title, total balance, this month's net, an In / Out / Cash strip and three action pills (Add, Withdraw, Transfer).
- The visibility (eye) button was removed; tap the total balance instead.
- Mini account cards are smaller and no longer include the Cash account (it already has its own tile).
- Recent transactions use the same row component as the history screen, so colors and icons match.
- Due-soon bills use a single-line banner; sections, cards and typography now match the sizes used on the other screens.
- "See all" under Recent transactions opens the **universal history** (all accounts, no filter). Previously a leftover account filter could carry over after using the back button; account and filter state are now cleared whenever history is left.

**Bills (full relayout)**
- The add button was removed from the header (use the **+** button in the bottom bar).
- New order: title → summary hero → provider filter → bills grouped by month → paid history.
- Bill cards show provider, item, installment and due date, a Pay button, and an overflow (⋮) menu with Delete.

**Receivable / Debt (moved from More)**
- The former More screen content moved into the Invest tab as the **Receivable / Debt** segment: net position, Everything / Receivable / Debt filter and the debt cards.
- The header **+** button on that segment opens the add receivable / debt dialog.
- The 2×2 shortcut cards (Accounts, History, Backup, Settings) were removed. Accounts and History are reachable through "See all" on Home, Backup through Settings, and Settings through the new Home icon.
- The **More** tab was removed. Debt notifications, the Home "Others" tiles and Analysis → debts now open the Receivable / Debt segment directly.

**History**
- The type chips (All / Income / Expense / Transfer-Cash) now use the same segmented control as the period filter.

**Chips (visual refresh)**
- New "tactile key" design: white buttons with a thick ink border and a hard offset shadow that visually presses in when tapped.
- Segmented control: the indicator morphs like a liquid (the leading edge moves faster than the trailing edge); the active label is ink and the active count badge is black.
- Selectable chip: active = white with an ink border and raised shadow; inactive = flat with a thin border and muted text.
- Action chip: always raised, bold label. The metric chip on dark surfaces is a translucent gradient glass.
- The public API of all chips is unchanged.

**Backup**
- **Automatic cleanup by age**: when a new daily backup is created and there are more than 4 daily backups, backups older than 3 days are deleted (oldest first) until only 4 remain, so at least 4 are always kept. It runs only when a new daily backup is added (overwriting the same day's file does not trigger it), and applies to both app storage and the chosen backup folder. "Before import" copies are never touched. The existing "keep N backups" setting still applies as an upper limit.
- `BackupManager.runBackup` / `buildFullJson` / `savePreImport` write encrypted payloads.
- The backup list can read both encrypted (device key) and legacy plain files.

**Build & texts**
- `versionName` is now **2.0.0** (previously 1.0.0 in the build file, out of sync with this changelog).
- The Settings "Backup & restore" text explains the encryption and its device limitation.
- The "About data" text shows version 2.0.0 and that SQLCipher is active.

### Fixed
- Legacy backup import failed when a paid bill / debt had no linked payment transaction.
- Investment withdrawals from legacy backups without a contribution lot.
- Deleting an income / incoming transfer that would make a balance negative is rejected with a clear message; batch delete removes expenses first.
- Backup validation rejected negative balances (which could block restoring your own backup) — negative balances are now allowed on restore.
- The salary schedule dialog picked the hour / account / frequency by position instead of by value.
- Per-account history showed the transfer destination as "Account".
- Paying / un-paying a debt used a stale UI object (risk of double payment) — it is now re-read inside the transaction.
- The delete-account dialog text now matches the actual behavior.
- The O(n²) transfer pairing in the transaction display was removed.
- The bottom bar could stretch to the full screen height because of the center button slot; its height is now bounded.

### Removed
- The **More** tab and its four shortcut cards.
- The visibility (eye) button on Home.
- The add buttons in the Bills header and the former More screen (replaced by the quick-add menu).

### Notes / known limitations
- Backups encrypted with the **device key** can only be restored on the same device (the master key is bound to the Keystore).
- To move to another device or share a backup safely, use the password-protected export (`buildPortableEncrypted`).
- PIN / App Lock is **still active** (see the correction under 1.3 below).
- There is no UI yet for entering the password when restoring a portable backup (the API is ready in the ViewModel / Repository).
- Most on-screen texts in the app are still in Indonesian; this changelog is in English.

---

## [1.3] - 2026-11-13

### Added
- Automatic backup: saved whenever the app is closed (only if data changed) and every 12 hours; one file per day, history kept for 7/14/30/60 days
- Backup list by date with one-tap restore; a "before import" safety copy is saved automatically before every restore
- Optional backup folder (Storage Access Framework) so copies survive uninstall
- Backups now include app settings (nominal input, notifications, bill reminder, catch-up, auto-backup) and format metadata
- Bill due reminder: one notification on the due day (optional D-1 / D-3 / D-7), with a configurable hour
- Tapping a notification opens the relevant tab (Bills / Invest / Home)

### Changed
- Redesigned salary / investment / schedule-failure notification text (formatted rupiah, account, balance after)
- Notifications use a proper monochrome status-bar icon
- Turning notifications off only silences them — background salary / investment processing keeps running
- Export now includes settings and metadata
- Redesigned dropdown menu (consistent item layout, clearer selected / hover states, smoother animation)

### Fixed
- Deleting an income / incoming transfer could push a balance below zero; it is now blocked with a clear message
- Batch delete now processes expenses before incomes so a valid selection no longer fails because of ordering
- Notifications on app open ignored the notification switch

### Removed
- The "000" shortcut button in the nominal input sheet; amounts are now typed in full

### Correction (synced with the actual code)
- The earlier entry stating **"Removed — PIN / app lock feature" was wrong**.
  The PIN + biometric + timeout feature **still exists and is active** in the code (`AppLock`, `LockScreen`, Settings, `MainActivity`).
  The PIN hash uses PBKDF2-HMAC-SHA256 with 100,000 iterations and a 16-byte salt (not plain SHA-256 as noted in 1.1).

---

## [1.2] - 2026-11-12

### Added
- Analysis screen replaces the Accounts tab: income / expense summary, net, daily average, savings rate, short insights, category and account breakdowns, largest expenses, bills / debts paid in the period
- Period filter in Analysis: this month, last month, 3 months, this year, with comparison to the previous period
- Automatic grouping of similar category names
- Manage accounts from Analysis or from "See all" on Home

### Changed
- Account picker sheet: checklist replaced by cards; the selected card has a thin black border
- The account list in the sheet is more compact and scrollable
- The mini account cards on Home open that account's transaction history
- Petty Cash / Cash is no longer a mini card on Home (it already has its own Cash dashboard)
- The bottom bar was replaced with a floating pill, more compact

### Removed
- The Accounts tab from the bottom bar (replaced by Analysis; account management moved to a sub-screen)

---

## [1.1] - 2026-11-01

### Added
- Settings screen (nominal input, notifications, PIN lock, schedule catch-up, backup link)
- Configurable trailing zeros for nominal input (Off / 0–6)
- App lock with PIN (stored as hash + salt; the actual implementation is PBKDF2, updated in the 2.0.0 notes)
- Catch-up day limit for schedules so they do not flood retroactive transactions
- Edit button for investment assets (name and target)
- FormDialog supports initial values for edit flows
- Documentation of the money scale (1 unit = Rp1)

### Changed
- The nominal field is fully controlled so the 000 button works without focus
- Transfer "from account" uses a dropdown for consistency
- Bill total amount is entered in full by hand
- Bill tenor uses horizontal chips (1 / 3 / 6 / 9 / 12 months)
- The add-debt sheet hides the type chips when the Receivable / Debt filter is active
- The schedule worker respects notification and catch-up preferences

### Fixed
- The 000 nominal button needed focus before it worked
- The debt type filter was out of sync between More and the add dialog

---

## [1.0] - 2026-10-30

### Fixed
- Transaction and balance calculations
- Bill and debt payment reversal
- Investment transaction handling
- Backup import validation
- Account deletion validation
- Opening balance calculation

### Changed
- Rebranded from DompetKu to SakuMe
- Database migration upgraded to v9
- Data consistency when deleting transactions improved

### Removed
- Bill due notifications (later added back in 1.3 with a new design)
