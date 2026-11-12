# Changelog

## [1.2] - 2026-11-12

### Added
- Layar Analisis menggantikan tab Accounts: ringkasan pemasukan/pengeluaran, netto, rata-rata harian, tingkat tabungan, insight singkat, breakdown per kategori & rekening, pengeluaran terbesar, serta tagihan/utang yang dibayar di periode itu
- Filter periode di Analisis: bulan ini, bulan lalu, 3 bulan, dan tahun ini, plus perbandingan vs periode sebelumnya
- Pengelompokan kategori otomatis untuk nama yang mirip (mis. beberapa variasi “Rokok …” digabung jadi satu grup, admin bank, tagihan, BBM, dll.)
- Akses Kelola rekening dari Analisis atau dari “Lihat semua” di Home (tambah, urut, hapus, transfer, tarik tunai tetap ada)

### Changed
- Sheet pilih rekening (bayar tagihan, pembayaran utang, dll.): checklist diganti card; yang dipilih dapat border hitam tipis, yang tidak hanya background card
- Daftar rekening di sheet dibuat lebih pendek (nama + saldo satu baris) dan bisa di-scroll jika panjang
- Mini card rekening di Home membuka histori akun itu, bukan layar rekening
- Dompet Tunai / Cash tidak lagi muncul di mini card Home (sudah ada di card Cash dashboard); akun cash tetap ada di sistem
- Bottom bar diganti gaya pill mengambang, lebih compact, dengan label yang lebih ringkas

### Removed
- Tab Accounts di bottom bar (fungsinya diganti Analisis; kelola rekening jadi sub-screen)

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
