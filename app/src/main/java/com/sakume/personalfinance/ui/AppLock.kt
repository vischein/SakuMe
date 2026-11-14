package com.sakume.personalfinance.ui

import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed interface PinResult {
    data object Success : PinResult
    data class Wrong(val attemptsLeft: Int) : PinResult
    data class Locked(val remainingMs: Long) : PinResult
}

/**
 * Kunci aplikasi (PIN + biometrik).
 * - PIN disimpan sebagai hash PBKDF2 + salt acak (bukan teks asli) di SharedPreferences terpisah.
 * - Status terkunci disimpan di memori proses: setelah proses mati, app selalu mulai dalam keadaan terkunci.
 * - Pengaturan kunci TIDAK ikut backup JSON (spesifik perangkat).
 */
object AppLock {
    private const val PREF = "sakume_security"
    private const val KEY_ENABLED = "lock_enabled"
    private const val KEY_SALT = "pin_salt"
    private const val KEY_HASH = "pin_hash"
    private const val KEY_BIOMETRIC = "biometric_enabled"
    private const val KEY_TIMEOUT = "lock_timeout_ms"
    private const val KEY_FAILS = "pin_fails"
    private const val KEY_LOCK_UNTIL = "pin_lock_until"
    private const val KEY_PIN_LENGTH = "pin_length"

    /** Panjang PIN yang bisa dipilih. PIN lama (sebelum ada pilihan) dianggap 6 digit. */
    val PIN_LENGTH_OPTIONS = listOf(4, 6)
    private const val DEFAULT_PIN_LENGTH = 6
    private const val MAX_TRIES_BEFORE_LOCKOUT = 5
    private const val DEFAULT_TIMEOUT_MS = 60_000L

    /** Jeda sebelum app terkunci lagi setelah ditinggalkan. */
    val TIMEOUT_OPTIONS = listOf(0L, 30_000L, 60_000L, 300_000L)

    var isLocked by mutableStateOf(false)
        private set

    private var initialized = false
    private var backgroundAt = 0L

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ---- status ----

    /** Dipanggil dari onCreate; hanya berpengaruh saat proses baru dimulai. */
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        isLocked = isEnabled(context)
    }

    fun isEnabled(context: Context): Boolean {
        val p = prefs(context)
        return p.getBoolean(KEY_ENABLED, false) && p.contains(KEY_HASH) && p.contains(KEY_SALT)
    }

    fun onBackgrounded() {
        backgroundAt = SystemClock.elapsedRealtime()
    }

    fun onForegrounded(context: Context) {
        if (isLocked || !isEnabled(context) || backgroundAt == 0L) return
        if (SystemClock.elapsedRealtime() - backgroundAt >= timeoutMs(context)) isLocked = true
    }

    fun unlock(context: Context) {
        prefs(context).edit().putInt(KEY_FAILS, 0).putLong(KEY_LOCK_UNTIL, 0L).apply()
        backgroundAt = 0L
        isLocked = false
    }

    // ---- PIN ----

    fun pinLength(context: Context): Int =
        prefs(context).getInt(KEY_PIN_LENGTH, DEFAULT_PIN_LENGTH).takeIf { it in PIN_LENGTH_OPTIONS } ?: DEFAULT_PIN_LENGTH

    fun setPin(context: Context, pin: String) {
        require(pin.length in PIN_LENGTH_OPTIONS && pin.all { it.isDigit() }) { "PIN harus 4 atau 6 digit" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs(context).edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash(pin, salt), Base64.NO_WRAP))
            .putBoolean(KEY_ENABLED, true)
            .putInt(KEY_PIN_LENGTH, pin.length)
            .putInt(KEY_FAILS, 0)
            .putLong(KEY_LOCK_UNTIL, 0L)
            .apply()
    }

    fun disable(context: Context) {
        prefs(context).edit()
            .remove(KEY_SALT).remove(KEY_HASH).remove(KEY_PIN_LENGTH)
            .putBoolean(KEY_ENABLED, false)
            .putBoolean(KEY_BIOMETRIC, false)
            .putInt(KEY_FAILS, 0)
            .putLong(KEY_LOCK_UNTIL, 0L)
            .apply()
        isLocked = false
    }

    fun lockoutRemainingMs(context: Context): Long =
        (prefs(context).getLong(KEY_LOCK_UNTIL, 0L) - System.currentTimeMillis()).coerceAtLeast(0L)

    /** Verifikasi PIN (berat ±100 ms: panggil dari Dispatchers.Default). Memberi jeda bila terlalu sering salah. */
    fun verify(context: Context, pin: String): PinResult {
        val remaining = lockoutRemainingMs(context)
        if (remaining > 0) return PinResult.Locked(remaining)

        val p = prefs(context)
        val salt = p.getString(KEY_SALT, null)?.let { Base64.decode(it, Base64.NO_WRAP) }
        val stored = p.getString(KEY_HASH, null)?.let { Base64.decode(it, Base64.NO_WRAP) }
        if (salt != null && stored != null && MessageDigest.isEqual(hash(pin, salt), stored)) {
            p.edit().putInt(KEY_FAILS, 0).putLong(KEY_LOCK_UNTIL, 0L).apply()
            return PinResult.Success
        }

        val fails = p.getInt(KEY_FAILS, 0) + 1
        return if (fails % MAX_TRIES_BEFORE_LOCKOUT == 0) {
            // 30 dtk, 60 dtk, 120 dtk, ... maksimum 15 menit
            val step = (fails / MAX_TRIES_BEFORE_LOCKOUT - 1).coerceAtMost(5)
            val ms = (30_000L shl step).coerceAtMost(900_000L)
            p.edit().putInt(KEY_FAILS, fails).putLong(KEY_LOCK_UNTIL, System.currentTimeMillis() + ms).apply()
            PinResult.Locked(ms)
        } else {
            p.edit().putInt(KEY_FAILS, fails).apply()
            PinResult.Wrong(MAX_TRIES_BEFORE_LOCKOUT - fails % MAX_TRIES_BEFORE_LOCKOUT)
        }
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 100_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    // ---- timeout ----

    fun timeoutMs(context: Context): Long =
        prefs(context).getLong(KEY_TIMEOUT, DEFAULT_TIMEOUT_MS).takeIf { it in TIMEOUT_OPTIONS } ?: DEFAULT_TIMEOUT_MS

    fun setTimeoutMs(context: Context, ms: Long) {
        prefs(context).edit().putLong(KEY_TIMEOUT, ms).apply()
    }

    // ---- biometrik ----

    fun biometricAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    fun biometricEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_BIOMETRIC, false)

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    /** Dialog biometrik sistem. Jika dibatalkan atau gagal, pengguna tetap bisa memakai PIN. */
    fun promptBiometric(activity: FragmentActivity, onSuccess: () -> Unit) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Buka SakuMe")
            .setSubtitle("Gunakan sidik jari atau wajah")
            .setNegativeButtonText("Gunakan PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is FragmentActivity) return c
        c = c.baseContext
    }
    return null
}
