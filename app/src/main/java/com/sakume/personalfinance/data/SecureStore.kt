package com.sakume.personalfinance.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom






/**
 * Dilempar bila kunci master database tidak bisa diambil dengan aman: Android Keystore gagal,
 * data kunci tidak terbaca/rusak, atau kunci hilang padahal database terenkripsi masih ada.
 * Aplikasi sengaja berhenti dengan pesan ini, TIDAK membuat kunci baru (kunci baru tidak akan
 * bisa membuka database yang sudah ada). Tidak ada data yang diubah atau dihapus.
 */
class MasterKeyUnavailableException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

object SecureStore {
    private const val PREF_FILE = "sakume_secure"
    private const val FALLBACK_FILE = PREF_FILE + "_fallback"
    private const val KEY_MASTER = "master_key_v1"
    private const val KEY_MIGRATED = "db_sqlcipher_migrated"
    private const val KEY_BACKUP_PW = "backup_password_v1"

    private const val MSG_KEYSTORE =
        "Penyimpanan kunci aman Android (Keystore) tidak bisa dipakai saat ini, jadi database SakuMe " +
            "yang terenkripsi tidak bisa dibuka. Data kamu tidak diubah atau dihapus. " +
            "Coba mulai ulang HP, lalu buka SakuMe lagi. Jika terus berulang, hubungi pengembang " +
            "sebelum menghapus data aplikasi."
    private const val MSG_KEY_CORRUPT =
        "Kunci database yang tersimpan tidak bisa dibaca (rusak). Data kamu tidak diubah atau dihapus. " +
            "Coba mulai ulang HP, lalu buka SakuMe lagi. Jika terus berulang, hubungi pengembang " +
            "sebelum menghapus data aplikasi."
    private const val MSG_KEY_LOST =
        "Kunci database tidak ditemukan, padahal file database terenkripsi masih ada. SakuMe tidak membuat " +
            "kunci baru karena tidak akan bisa membuka database itu. Data kamu tidak diubah atau dihapus. " +
            "Jika kamu punya backup berpassword, kamu bisa menghapus data aplikasi lalu memulihkan dari backup."

    @Volatile
    private var cachedKey: ByteArray? = null

    @Volatile
    private var cachedPrefs: SharedPreferences? = null

    @Volatile
    private var lastKeystoreError: Throwable? = null

    /** EncryptedSharedPreferences; hasil yang berhasil dibuat disimpan agar tidak dibuat ulang tiap panggilan. */
    private fun encryptedPrefsOrNull(context: Context): SharedPreferences? {
        cachedPrefs?.let { return it }
        synchronized(this) {
            cachedPrefs?.let { return it }
            return try {
                val masterKey = MasterKey.Builder(context.applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context.applicationContext,
                    PREF_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                ).also {
                    cachedPrefs = it
                    lastKeystoreError = null
                }
            } catch (e: Exception) {
                lastKeystoreError = e
                null
            }
        }
    }

    /** Prefs polos peninggalan versi lama (hanya dibaca; tidak pernah dipakai untuk membuat kunci baru). */
    private fun legacyFallbackPrefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE)

    // ---- Password backup (portabel) ----
    // Disimpan HANYA di EncryptedSharedPreferences (tanpa fallback ke prefs polos),
    // supaya backup otomatis di background bisa memakainya tanpa meminta ke pengguna.

    fun hasBackupPassword(context: Context): Boolean =
        try {
            encryptedPrefsOrNull(context)?.contains(KEY_BACKUP_PW) == true
        } catch (_: Exception) {
            false
        }

    /** Salinan password; pemanggil wajib menimpa array-nya (fill) setelah dipakai. */
    fun getBackupPassword(context: Context): CharArray? =
        encryptedPrefsOrNull(context)?.getString(KEY_BACKUP_PW, null)?.toCharArray()

    /** @return false bila penyimpanan aman tidak tersedia atau password kosong (password TIDAK disimpan). */
    fun setBackupPassword(context: Context, password: CharArray): Boolean {
        if (password.isEmpty()) return false
        val p = encryptedPrefsOrNull(context) ?: return false
        return p.edit().putString(KEY_BACKUP_PW, String(password)).commit()
    }

    fun clearBackupPassword(context: Context) {
        encryptedPrefsOrNull(context)?.edit()?.remove(KEY_BACKUP_PW)?.commit()
    }

    private fun decodeKey(encoded: String?): ByteArray? {
        if (encoded == null) return null
        return try {
            Base64.decode(encoded, Base64.NO_WRAP).takeIf { it.size == 32 }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Mengembalikan kunci master (32 byte). Kunci baru dibuat HANYA bila Keystore bekerja, belum ada kunci
     * di mana pun, dan tidak ada file database terenkripsi yang menunggu kunci itu.
     * Selain itu dilempar [MasterKeyUnavailableException]; tidak ada fallback diam-diam.
     */
    fun getOrCreateMasterKey(context: Context): ByteArray {
        cachedKey?.let { return it }
        synchronized(this) {
            cachedKey?.let { return it }

            val enc = encryptedPrefsOrNull(context)
            val legacy = legacyFallbackPrefs(context)

            if (enc != null) {
                val stored = try {
                    enc.getString(KEY_MASTER, null)
                } catch (e: Exception) {
                    throw MasterKeyUnavailableException(MSG_KEYSTORE, e)
                }
                if (stored != null) {
                    val key = decodeKey(stored) ?: throw MasterKeyUnavailableException(MSG_KEY_CORRUPT)
                    cachedKey = key
                    return key
                }
            }

            // Peninggalan versi lama: kunci pernah tersimpan di prefs polos saat Keystore gagal.
            // Database lama dienkripsi dengan kunci itu, jadi tetap dipakai (dan dipindah ke prefs aman bila bisa).
            val legacyStored = legacy.getString(KEY_MASTER, null)
            if (legacyStored != null) {
                val key = decodeKey(legacyStored) ?: throw MasterKeyUnavailableException(MSG_KEY_CORRUPT)
                if (enc != null) {
                    val moved = try {
                        enc.edit().putString(KEY_MASTER, legacyStored).commit() &&
                            decodeKey(enc.getString(KEY_MASTER, null))?.contentEquals(key) == true
                    } catch (_: Exception) {
                        false
                    }
                    if (moved) legacy.edit().remove(KEY_MASTER).commit()
                }
                cachedKey = key
                return key
            }

            if (enc == null) throw MasterKeyUnavailableException(MSG_KEYSTORE, lastKeystoreError)

            // Tidak ada kunci di mana pun. Jangan buat yang baru kalau database terenkripsi sudah ada.
            if (classifyDbFile(context.getDatabasePath(DB_FILE_NAME)) == DbFileKind.OTHER) {
                throw MasterKeyUnavailableException(MSG_KEY_LOST)
            }

            val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val saved = try {
                enc.edit().putString(KEY_MASTER, Base64.encodeToString(key, Base64.NO_WRAP)).commit()
            } catch (e: Exception) {
                throw MasterKeyUnavailableException(MSG_KEYSTORE, e)
            }
            if (!saved) throw MasterKeyUnavailableException(MSG_KEYSTORE)
            cachedKey = key
            return key
        }
    }

    fun sqlCipherPassphrase(context: Context): ByteArray =
        getOrCreateMasterKey(context).copyOf()

    fun isDbMigrated(context: Context): Boolean {
        val encFlag = try {
            encryptedPrefsOrNull(context)?.getBoolean(KEY_MIGRATED, false) == true
        } catch (_: Exception) {
            false
        }
        return encFlag || legacyFallbackPrefs(context).getBoolean(KEY_MIGRATED, false)
    }

    fun setDbMigrated(context: Context, value: Boolean = true) {
        val p = encryptedPrefsOrNull(context) ?: legacyFallbackPrefs(context)
        p.edit().putBoolean(KEY_MIGRATED, value).apply()
    }
}
