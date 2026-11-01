# Changelog

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
