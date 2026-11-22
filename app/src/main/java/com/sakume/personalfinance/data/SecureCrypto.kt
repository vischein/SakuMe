package com.sakume.personalfinance.data

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Dilempar saat backup berpassword tidak bisa dibuka.
 * [wrong] = true  -> password yang diberikan salah.
 * [wrong] = false -> belum ada password yang cocok (perlu diminta ke pengguna).
 */
class BackupPasswordException(val wrong: Boolean, message: String) : IllegalArgumentException(message)

/** Metadata non-sensitif di header file backup berpassword (dipakai untuk menampilkan daftar tanpa mendekripsi). */
data class PasswordHeader(
    val iterations: Int,
    val createdAtMillis: Long,
    val accounts: Int,
    val transactions: Int
)

/**
 * Format yang dikenal:
 *  - SAKUMEENC1   : AES-256-GCM, kunci perangkat (Keystore). Hanya terbaca di HP + instalasi yang sama.
 *  - SAKUMEENCPW1 : AES-256-GCM, kunci dari password (PBKDF2 120k). Format lama, tanpa header.
 *  - SAKUMEENCPW2 : seperti PW1 + header metadata (iterasi, tanggal, jumlah data) yang ikut
 *                   diautentikasi sebagai AAD GCM. Format baku untuk backup portabel.
 *
 * Layout PW2:
 *   SAKUMEENCPW2\n
 *   iter=210000;created=<ms>;acc=<n>;tx=<n>\n
 *   base64( salt[16] + iv[12] + ciphertext+tag )
 */
object SecureCrypto {
    private const val MAGIC = "SAKUMEENC1"
    private const val MAGIC_PW_PREFIX = "SAKUMEENCPW"
    private const val MAGIC_PW1 = "SAKUMEENCPW1"
    private const val MAGIC_PW2 = "SAKUMEENCPW2"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val KEY_BITS = 256
    private const val PBKDF2_ITERATIONS_V1 = 120_000
    private const val PBKDF2_ITERATIONS_V2 = 210_000
    private const val MIN_ITERATIONS = 10_000
    private const val MAX_ITERATIONS = 2_000_000
    private const val SALT_LENGTH = 16
    const val MIN_PASSWORD_LENGTH = 8

    /** Terenkripsi apa pun (kunci perangkat ATAU password). */
    fun isEncrypted(text: String): Boolean {
        val t = text.trimStart()
        return t.startsWith(MAGIC) || t.startsWith(MAGIC_PW_PREFIX)
    }

    /** Terenkripsi dengan kunci perangkat (tidak portabel). */
    fun isDeviceEncrypted(text: String): Boolean = text.trimStart().startsWith(MAGIC)

    /** Terenkripsi dengan password (portabel). */
    fun isPasswordEncrypted(text: String): Boolean =
        text.trimStart().startsWith(MAGIC_PW_PREFIX)

    // ---------------------------------------------------------------- kunci perangkat

    fun encrypt(plain: String, key: ByteArray): String {
        require(key.size == 32) { "Key must be 32 bytes" }
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return MAGIC + "\n" + Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    fun decrypt(payload: String, key: ByteArray): String {
        require(key.size == 32) { "Key must be 32 bytes" }
        val body = payload.trimStart()
            .removePrefix(MAGIC)
            .trimStart('\n', '\r', ' ')
        val combined = Base64.decode(body, Base64.NO_WRAP)
        require(combined.size > GCM_IV_LENGTH) { "Payload terlalu pendek" }
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    // ---------------------------------------------------------------- password (portabel)

    fun encryptWithPassword(
        plain: String,
        password: CharArray,
        accounts: Int,
        transactions: Int,
        createdAtMillis: Long
    ): String {
        require(password.isNotEmpty()) { "Password wajib diisi" }
        val header = "iter=$PBKDF2_ITERATIONS_V2;created=$createdAtMillis;acc=$accounts;tx=$transactions"
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt, PBKDF2_ITERATIONS_V2)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(header.toByteArray(Charsets.UTF_8))
            val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            return MAGIC_PW2 + "\n" + header + "\n" +
                Base64.encodeToString(salt + iv + encrypted, Base64.NO_WRAP)
        } finally {
            key.fill(0)
        }
    }

    /** Baca header PW2 dari awal file (cukup beberapa ratus karakter pertama). Null jika bukan PW2. */
    fun readPasswordHeader(textOrHead: String): PasswordHeader? {
        val lines = textOrHead.trimStart().lineSequence().take(2).toList()
        if (lines.size < 2 || lines[0].trim() != MAGIC_PW2) return null
        return parseHeader(lines[1].trim())
    }

    private fun parseHeader(line: String): PasswordHeader? {
        val kv = line.split(';').mapNotNull { part ->
            part.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] }
        }.toMap()
        val iter = kv["iter"]?.toIntOrNull() ?: return null
        if (iter !in MIN_ITERATIONS..MAX_ITERATIONS) return null
        return PasswordHeader(
            iterations = iter,
            createdAtMillis = kv["created"]?.toLongOrNull() ?: 0L,
            accounts = kv["acc"]?.toIntOrNull() ?: 0,
            transactions = kv["tx"]?.toIntOrNull() ?: 0
        )
    }

    fun decryptWithPassword(payload: String, password: CharArray): String {
        require(password.isNotEmpty()) { "Password wajib diisi" }
        val t = payload.trimStart()
        val iterations: Int
        val aad: ByteArray?
        val body: String
        when {
            t.startsWith(MAGIC_PW2) -> {
                val lines = t.lines()
                val headerLine = lines.getOrNull(1)?.trim().orEmpty()
                val header = parseHeader(headerLine)
                    ?: throw IllegalArgumentException("Header backup rusak")
                iterations = header.iterations
                aad = headerLine.toByteArray(Charsets.UTF_8)
                body = lines.drop(2).joinToString("").trim()
            }
            t.startsWith(MAGIC_PW1) -> {
                iterations = PBKDF2_ITERATIONS_V1
                aad = null
                body = t.removePrefix(MAGIC_PW1).trimStart('\n', '\r', ' ')
            }
            else -> throw IllegalArgumentException("Bukan file backup berpassword")
        }
        val combined = try {
            Base64.decode(body, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Isi backup rusak", e)
        }
        require(combined.size > SALT_LENGTH + GCM_IV_LENGTH) { "Payload terlalu pendek" }
        val salt = combined.copyOfRange(0, SALT_LENGTH)
        val iv = combined.copyOfRange(SALT_LENGTH, SALT_LENGTH + GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(SALT_LENGTH + GCM_IV_LENGTH, combined.size)
        val key = deriveKey(password, salt, iterations)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
            if (aad != null) cipher.updateAAD(aad)
            return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: BadPaddingException) {
            // AEADBadTagException turunan BadPaddingException: password salah ATAU file diubah.
            throw BackupPasswordException(wrong = true, message = "Password salah (atau file backup rusak).")
        } finally {
            key.fill(0)
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
