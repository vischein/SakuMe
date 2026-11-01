package com.sakume.personalfinance.ui

import android.content.Context
import java.security.MessageDigest

object AppPrefs {
    private const val PREF = "sakume_prefs"

    private const val KEY_TRAILING_ZEROS = "trailing_zeros"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"
    private const val KEY_APP_LOCK = "app_lock_enabled"
    private const val KEY_PIN_HASH = "app_pin_hash"
    private const val KEY_MAX_CATCHUP_DAYS = "max_catchup_days"

    const val DEFAULT_TRAILING_ZEROS = 3
    const val DEFAULT_MAX_CATCHUP_DAYS = 30
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

    fun notificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIFICATIONS, true)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    fun appLockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_APP_LOCK, false) && pinHash(context).isNotEmpty()

    fun setAppLockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_APP_LOCK, enabled).apply()
    }

    fun pinHash(context: Context): String =
        prefs(context).getString(KEY_PIN_HASH, "") ?: ""

    fun setPin(context: Context, pin: String) {
        prefs(context).edit().putString(KEY_PIN_HASH, hashPin(pin)).apply()
    }

    fun clearPin(context: Context) {
        prefs(context).edit()
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_APP_LOCK, false)
            .apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val stored = pinHash(context)
        if (stored.isEmpty()) return false
        return stored == hashPin(pin)
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun maxCatchupDays(context: Context): Int =
        prefs(context).getInt(KEY_MAX_CATCHUP_DAYS, DEFAULT_MAX_CATCHUP_DAYS).coerceIn(7, 365)

    fun setMaxCatchupDays(context: Context, days: Int) {
        prefs(context).edit().putInt(KEY_MAX_CATCHUP_DAYS, days.coerceIn(7, 365)).apply()
    }
}
