# Changelog

## [1.3] - 2026-11-13

### Added
- Automatic backup: saved whenever the app is closed (only if data changed) and every 12 hours; one file per day, history kept for 7/14/30/60 days
- Backup list by date with one-tap restore; a "before import" safety copy is saved automatically before every restore
- Optional backup folder (Storage Access Framework) so copies survive uninstall; picking the same folder after reinstall lists the old backups again
- Backup now includes app settings (nominal input, notifications, bill reminder, catch-up, auto-backup) and format metadata
- Bill due reminder: one notification on the due day (optional H-1 / H-3 / H-7), at a configurable hour; follows each bill's own due date
- Tapping a notification opens the relevant tab (Bills / Invest / Home)

### Changed
- Redesigned salary / investment / schedule-failure notification text (formatted rupiah, account, balance after)
- Notifications use a proper monochrome status-bar icon
- Turning notifications off no longer stops background salary / investment processing — it only silences notifications
- Export now includes settings and metadata
- Redesigned dropdown menu (consistent item layout, clearer selected/hover states, smoother animation, adjusted padding).

### Fixed
- Deleting an income / incoming transfer could push an account balance below zero; it is now blocked with a clear message
- Batch delete now processes expenses before incomes so a valid selection no longer fails because of ordering
- Notifications on app open ignored the notification switch

### Removed
- PIN / app lock feature
- Removed the "000" shortcut button from the nominal input sheet; amounts are now typed in full.

## [1.2] - 2026-11-12

### Added
- Analysis screen replacing the Accounts tab: income/expense summary, net amount, daily average, savings rate, short insights, breakdown by category & account, largest expense, plus bills/debts paid during the period
- Period filter in Analysis: this month, last month, 3 months, and this year, with comparison against the previous period
- Automatic category grouping for similar names (e.g. multiple "Rokok …" variations merged into one group, bank admin fees, bills, fuel, etc.)
- Manage accounts accessible from Analysis or via "See all" on Home (add, reorder, delete, transfer, and cash withdrawal are still available)

### Changed
- Account picker sheet (bill payment, debt payment, etc.): checklist replaced with cards; the selected card gets a thin black border, unselected cards show only the card background
- Account list in the sheet is now more compact (name + balance on one line) and scrollable when long
- Mini account cards on Home now open the account's transaction history instead of the accounts screen
- Petty Cash / Cash no longer appears in the Home mini cards (it already exists on the dashboard Cash card); the cash account remains in the system
- Bottom bar redesigned as a floating pill, more compact, with shorter labels

### Removed
- Accounts tab from the bottom bar (replaced by Analysis; account management moved to a sub-screen)

## [Unreleased]

### Fixed
- Fixed legacy backup import failing when paid bills/debts have no linked payment transaction
- Fixed withdrawing from investments imported from legacy backups (no contribution lots)
- Fixed deleting income/transfer-in transactions making an account balance negative (now rejected with a clear message); batch delete now removes expenses first
- Fixed backup validation rejecting accounts with negative balance (could block restoring own backups)
- Fixed salary schedule dialog picking hour/account/frequency by value instead of position
- Fixed per-account history showing transfer destination as "Rekening"
- Fixed debt pay/unpay using a stale UI object (possible double payment); now re-read inside the transaction
- Fixed delete-account dialog text to match actual behavior
- Removed O(n²) transfer pairing in transaction display

## [1.1] - 2026-11-01

### Added
- Added settings screen (input nominal, notifications, PIN lock, schedule catch-up, backup link)
- Added configurable trailing zeros for nominal input (Off / 0–6 zeros)
- Added app lock with PIN (SHA-256 hashed)
- Added schedule catch-up day limit to prevent mass retroactive transactions
- Added edit button for investment assets (name and target)
- Added FormDialog initial values support for edit flows
- Added money scale documentation (1 unit = Rp1 whole rupiah)

### Changed
- Changed money field to fully controlled input so the 000 button works without prior focus
- Changed transfer “from account” to dropdown for consistency with “to account”
- Changed bill “total nominal” to full manual input (no auto trailing zeros)
- Changed bill tenor to horizontal scrollable chips (1 / 3 / 6 / 9 / 12 months)
- Changed debt add sheet to hide type chips when Piutang or Utang filter card is active
- Changed schedule worker to respect notification and catch-up preferences

### Fixed
- Fixed nominal 000 button requiring field focus before use
- Fixed debt type filter not syncing from More screen to add dialog

## [1.0] - 2026-10-30

### Fixed
- Fixed transaction and balance calculation issues
- Fixed bill and debt payment reversal
- Fixed investment transaction handling
- Fixed backup import validation
- Fixed account deletion validation
- Fixed opening balance calculation

### Changed
- Rebrand application from DompetKu to SakuMe
- Removed bill notifications
- Improved database migration to v9
- Improved data consistency during transaction deletion

### Removed
- Bill due notifications
