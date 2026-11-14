package com.sakume.personalfinance.ui

import android.content.Context

object AppPrefs {
    private const val PREF = "sakume_prefs"

    private const val KEY_TRAILING_ZEROS = "trailing_zeros"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"
    private const val KEY_HIDE_HOME_BALANCE = "hide_home_balance"
    private const val KEY_MAX_CATCHUP_DAYS = "max_catchup_days"
    private const val KEY_BILL_REMINDER = "bill_reminder_enabled"
    private const val KEY_BILL_REMINDER_HOUR = "bill_reminder_hour"
    private const val KEY_BILL_REMINDER_EARLY = "bill_reminder_early_days"
    private const val KEY_BILL_REMINDER_LAST_DAY = "bill_reminder_last_day"
    private const val KEY_AUTO_BACKUP = "auto_backup_enabled"
    private const val KEY_AUTO_BACKUP_KEEP = "auto_backup_keep"
    private const val KEY_AUTO_BACKUP_FOLDER = "auto_backup_folder_uri"
    private const val KEY_AUTO_BACKUP_HASH = "auto_backup_last_hash"
    private const val KEY_AUTO_BACKUP_LAST = "auto_backup_last_millis"

    const val DEFAULT_TRAILING_ZEROS = 3
    const val DEFAULT_MAX_CATCHUP_DAYS = 30
    const val DEFAULT_BILL_REMINDER_HOUR = 8
    const val DEFAULT_BACKUP_KEEP = 30
    /** Jam pengingat tagihan yang bisa dipilih. */
    val REMINDER_HOURS = listOf(6, 7, 8, 9, 12, 18, 20)
    /** Pengingat lebih awal (hari sebelum jatuh tempo). 0 = hanya di hari H. */
    val EARLY_OPTIONS = listOf(0, 1, 3, 7)
    val BACKUP_KEEP_OPTIONS = listOf(7, 14, 30, 60)
    val OPTIONS = listOf(0, 1, 2, 3, 4, 5, 6)
    val CATCHUP_OPTIONS = listOf(7, 14, 30, 60, 90, 180)

    const val MONEY_SCALE_RUPIAH = 1L

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun trailingZeros(context: Context): Int =
        prefs(context).getInt(KEY_TRAILING_ZEROS, DEFAULT_TRAILING_ZEROS).coerceIn(0, 6)

    fun setTrailingZeros(context: Context, n: Int) {
        prefs(context).edit().putInt(KEY_TRAILING_ZEROS, n.coerceIn(0, 6)).apply()
    }

    fun multiplier(n: Int): Long {
        var m = 1L
        repeat(n.coerceIn(0, 6)) { m *= 10L }
        return m
    }

    fun zerosLabel(n: Int): String = if (n <= 0) "Off" else "0".repeat(n)

    /** Sembunyikan nominal di Home saja (total saldo, cash, masuk/keluar, kartu rekening). */
    fun hideHomeBalance(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HIDE_HOME_BALANCE, false)

    fun setHideHomeBalance(context: Context, hidden: Boolean) {
        prefs(context).edit().putBoolean(KEY_HIDE_HOME_BALANCE, hidden).apply()
    }

    fun notificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIFICATIONS, true)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    /** Fitur PIN sudah dihapus; bersihkan sisa data lama supaya tidak ada hash PIN tertinggal. */
    fun removeLegacyPin(context: Context) {
        prefs(context).edit().remove("app_pin_hash").remove("app_lock_enabled").apply()
    }

    fun maxCatchupDays(context: Context): Int =
        prefs(context).getInt(KEY_MAX_CATCHUP_DAYS, DEFAULT_MAX_CATCHUP_DAYS).coerceIn(7, 365)

    fun setMaxCatchupDays(context: Context, days: Int) {
        prefs(context).edit().putInt(KEY_MAX_CATCHUP_DAYS, days.coerceIn(7, 365)).apply()
    }

    // ---- Pengingat tagihan ----
    fun billReminderEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BILL_REMINDER, true)

    fun setBillReminderEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BILL_REMINDER, enabled).apply()
    }

    fun billReminderHour(context: Context): Int =
        prefs(context).getInt(KEY_BILL_REMINDER_HOUR, DEFAULT_BILL_REMINDER_HOUR).coerceIn(0, 23)

    fun setBillReminderHour(context: Context, hour: Int) {
        prefs(context).edit().putInt(KEY_BILL_REMINDER_HOUR, hour.coerceIn(0, 23)).apply()
    }

    fun billReminderEarlyDays(context: Context): Int =
        prefs(context).getInt(KEY_BILL_REMINDER_EARLY, 0).coerceIn(0, 30)

    fun setBillReminderEarlyDays(context: Context, days: Int) {
        prefs(context).edit().putInt(KEY_BILL_REMINDER_EARLY, days.coerceIn(0, 30)).apply()
    }

    /** Tanggal (yyyyMMdd) terakhir pengingat dikirim, supaya tidak dobel dalam satu hari. */
    fun billReminderLastDay(context: Context): Int =
        prefs(context).getInt(KEY_BILL_REMINDER_LAST_DAY, 0)

    fun setBillReminderLastDay(context: Context, day: Int) {
        prefs(context).edit().putInt(KEY_BILL_REMINDER_LAST_DAY, day).apply()
    }

    // ---- Backup otomatis ----
    fun autoBackupEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_BACKUP, true)

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_BACKUP, enabled).apply()
    }

    fun autoBackupKeep(context: Context): Int =
        prefs(context).getInt(KEY_AUTO_BACKUP_KEEP, DEFAULT_BACKUP_KEEP).coerceIn(3, 365)

    fun setAutoBackupKeep(context: Context, keep: Int) {
        prefs(context).edit().putInt(KEY_AUTO_BACKUP_KEEP, keep.coerceIn(3, 365)).apply()
    }

    /** URI folder (Storage Access Framework) tempat salinan backup juga ditulis; null = belum dipilih. */
    fun autoBackupFolder(context: Context): String? =
        prefs(context).getString(KEY_AUTO_BACKUP_FOLDER, null)?.takeIf { it.isNotBlank() }

    fun setAutoBackupFolder(context: Context, uri: String?) {
        prefs(context).edit().apply {
            if (uri.isNullOrBlank()) remove(KEY_AUTO_BACKUP_FOLDER) else putString(KEY_AUTO_BACKUP_FOLDER, uri)
            // Folder baru -> paksa tulis ulang walau data belum berubah.
            remove(KEY_AUTO_BACKUP_HASH)
        }.apply()
    }

    fun autoBackupLastHash(context: Context): String? =
        prefs(context).getString(KEY_AUTO_BACKUP_HASH, null)

    fun autoBackupLastMillis(context: Context): Long =
        prefs(context).getLong(KEY_AUTO_BACKUP_LAST, 0L)

    fun setAutoBackupState(context: Context, hash: String, millis: Long) {
        prefs(context).edit().putString(KEY_AUTO_BACKUP_HASH, hash).putLong(KEY_AUTO_BACKUP_LAST, millis).apply()
    }
}
