package com.sakume.personalfinance.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom






object SecureStore {
    private const val PREF_FILE = "sakume_secure"
    private const val KEY_MASTER = "master_key_v1"
    private const val KEY_MIGRATED = "db_sqlcipher_migrated"
    private const val KEY_BACKUP_PW = "backup_password_v1"

    @Volatile
    private var cachedKey: ByteArray? = null

    private fun encryptedPrefsOrNull(context: Context) = try {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        null
    }

    private fun prefs(context: Context) = encryptedPrefsOrNull(context)
        ?: context.applicationContext.getSharedPreferences(PREF_FILE + "_fallback", Context.MODE_PRIVATE)

    // ---- Password backup (portabel) ----
    // Disimpan HANYA di EncryptedSharedPreferences (tanpa fallback ke prefs polos),
    // supaya backup otomatis di background bisa memakainya tanpa meminta ke pengguna.

    fun hasBackupPassword(context: Context): Boolean =
        encryptedPrefsOrNull(context)?.contains(KEY_BACKUP_PW) == true

    /** Salinan password; pemanggil wajib menimpa array-nya (fill) setelah dipakai. */
    fun getBackupPassword(context: Context): CharArray? =
        encryptedPrefsOrNull(context)?.getString(KEY_BACKUP_PW, null)?.toCharArray()

    /** @return false bila penyimpanan aman tidak tersedia (password TIDAK disimpan). */
    fun setBackupPassword(context: Context, password: CharArray): Boolean {
        if (password.isEmpty()) return false
        val p = encryptedPrefsOrNull(context) ?: return false
        return p.edit().putString(KEY_BACKUP_PW, String(password)).commit()
    }

    fun clearBackupPassword(context: Context) {
        encryptedPrefsOrNull(context)?.edit()?.remove(KEY_BACKUP_PW)?.commit()
    }

    
    fun getOrCreateMasterKey(context: Context): ByteArray {
        cachedKey?.let { return it }
        synchronized(this) {
            cachedKey?.let { return it }
            val p = prefs(context)
            val existing = p.getString(KEY_MASTER, null)
            if (existing != null) {
                val key = Base64.decode(existing, Base64.NO_WRAP)
                if (key.size == 32) {
                    cachedKey = key
                    return key
                }
            }
            val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
            p.edit().putString(KEY_MASTER, Base64.encodeToString(key, Base64.NO_WRAP)).apply()
            cachedKey = key
            return key
        }
    }

    
    fun sqlCipherPassphrase(context: Context): ByteArray =
        getOrCreateMasterKey(context).copyOf()

    fun isDbMigrated(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MIGRATED, false)

    fun setDbMigrated(context: Context, value: Boolean = true) {
        prefs(context).edit().putBoolean(KEY_MIGRATED, value).apply()
    }
}
