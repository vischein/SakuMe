package com.sakume.personalfinance.data

import org.junit.Assert.*
import org.junit.Test

class SecureCryptoTest {
    @Test fun passwordBackupPw2RoundTripsPlaintextAndMetadata() {
        val encrypted = SecureCrypto.encryptWithPassword(
            "{\"payload\":\"SakuMe\"}", "correct horse".toCharArray(), 2, 7, 1_700_000_000_000
        )
        assertTrue(encrypted.startsWith("SAKUMEENCPW2\n"))
        assertEquals("{\"payload\":\"SakuMe\"}", SecureCrypto.decryptWithPassword(encrypted, "correct horse".toCharArray()))
        assertEquals(2, SecureCrypto.readPasswordHeader(encrypted)!!.accounts)
        assertEquals(7, SecureCrypto.readPasswordHeader(encrypted)!!.transactions)
    }

    @Test fun wrongPasswordThrowsBackupPasswordExceptionWithWrongFlag() {
        val encrypted = SecureCrypto.encryptWithPassword("secret", "password-1".toCharArray(), 0, 0, 1L)
        val error = try {
            SecureCrypto.decryptWithPassword(encrypted, "password-2".toCharArray())
            fail("Expected password failure")
            null
        } catch (e: BackupPasswordException) { e }
        assertNotNull(error)
        assertTrue(error!!.wrong)
    }

    @Test fun authenticatedHeaderModificationFailsDecryption() {
        val encrypted = SecureCrypto.encryptWithPassword("secret", "password-1".toCharArray(), 1, 2, 1L)
        val modified = encrypted.replace("acc=1", "acc=2")
        val error = try {
            SecureCrypto.decryptWithPassword(modified, "password-1".toCharArray())
            fail("Expected authenticated header failure")
            null
        } catch (e: BackupPasswordException) { e }
        assertNotNull(error)
        assertTrue(error!!.wrong)
    }

    @Test fun passwordBackupRejectsIterationsOutsideAllowedRange() {
        val encrypted = SecureCrypto.encryptWithPassword("secret", "password-1".toCharArray(), 1, 2, 1L)
        val tooLow = encrypted.replace(Regex("iter=\\d+"), "iter=9999")
        assertThrows(IllegalArgumentException::class.java) {
            SecureCrypto.decryptWithPassword(tooLow, "password-1".toCharArray())
        }
        val tooHigh = encrypted.replace(Regex("iter=\\d+"), "iter=2000001")
        assertThrows(IllegalArgumentException::class.java) {
            SecureCrypto.decryptWithPassword(tooHigh, "password-1".toCharArray())
        }
    }


    @Test fun legacyPw1PasswordBackupRemainsReadable() {
        val password = "legacy-password".toCharArray()
        val salt = ByteArray(16) { (it + 1).toByte() }
        val iv = ByteArray(12) { (it + 21).toByte() }
        val spec = javax.crypto.spec.PBEKeySpec(password, salt, 120_000, 256)
        val key = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec).encoded
        spec.clearPassword()
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, javax.crypto.spec.SecretKeySpec(key, "AES"),
            javax.crypto.spec.GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal("legacy-backup".toByteArray(Charsets.UTF_8))
        val payload = "SAKUMEENCPW1\n" + java.util.Base64.getEncoder().encodeToString(salt + iv + encrypted)
        assertEquals("legacy-backup", SecureCrypto.decryptWithPassword(payload, password))
        key.fill(0)
    }

    @Test fun backupCodecReadsPreviousJsonWithoutNewOptionalMetadata() {
        val oldJson = """
            {
              "accounts": [],
              "transactions": [],
              "bills": [],
              "investments": [],
              "contributions": [],
              "debts": []
            }
        """.trimIndent()
        val decoded = BackupCodec.decode(oldJson)
        assertTrue(decoded.accounts.isEmpty())
        assertTrue(decoded.schedules.isEmpty())
        assertNull(decoded.meta)
        assertNull(decoded.settings)
    }
}
