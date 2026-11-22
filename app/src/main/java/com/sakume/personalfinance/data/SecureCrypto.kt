package com.sakume.personalfinance.data

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec










object SecureCrypto {
    private const val MAGIC = "SAKUMEENC1"
    private const val MAGIC_PW = "SAKUMEENCPW1"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val KEY_BITS = 256
    private const val PBKDF2_ITERATIONS = 120_000
    private const val SALT_LENGTH = 16

    fun isEncrypted(text: String): Boolean {
        val t = text.trimStart()
        return t.startsWith(MAGIC) || t.startsWith(MAGIC_PW)
    }

    fun isPasswordEncrypted(text: String): Boolean =
        text.trimStart().startsWith(MAGIC_PW)

    
    fun encrypt(plain: String, key: ByteArray): String {
        require(key.size == 32) { "Key must be 32 bytes" }
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val combined = iv + encrypted
        return MAGIC + "\n" + Base64.encodeToString(combined, Base64.NO_WRAP)
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

    
    fun encryptWithPassword(plain: String, password: CharArray): String {
        require(password.isNotEmpty()) { "Password wajib diisi" }
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val combined = salt + iv + encrypted
        return MAGIC_PW + "\n" + Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decryptWithPassword(payload: String, password: CharArray): String {
        require(password.isNotEmpty()) { "Password wajib diisi" }
        val body = payload.trimStart()
            .removePrefix(MAGIC_PW)
            .trimStart('\n', '\r', ' ')
        val combined = Base64.decode(body, Base64.NO_WRAP)
        require(combined.size > SALT_LENGTH + GCM_IV_LENGTH) { "Payload terlalu pendek" }
        val salt = combined.copyOfRange(0, SALT_LENGTH)
        val iv = combined.copyOfRange(SALT_LENGTH, SALT_LENGTH + GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(SALT_LENGTH + GCM_IV_LENGTH, combined.size)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }
}
