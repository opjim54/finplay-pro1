package com.example.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cryptographic utility supporting 120,000-iteration PBKDF2-HMAC-SHA256 key derivation
 * for irreversible Admin/User credential hashing, with constant-time verification,
 * API 24+ pure-HmacSHA256 fallback, and backwards compatibility for legacy SHA-256 user records.
 */
object CryptoUtil {
    private val random = SecureRandom()
    const val PBKDF2_PREFIX = "pbkdf2_sha256\$"
    private const val PBKDF2_ITERATIONS = 120_000
    private const val PBKDF2_KEY_BITS = 256

    // Pre-derived 120,000-iteration PBKDF2-HMAC-SHA256 digests (zero plaintext secrets stored in APK!)
    const val SEALED_ADMIN_KEY_SALT = "9f4b2e7c1a8d5f03e6b9c2a4d7e1f8b0c3a5d9e2f4b6a8c0d2e4f6a8b0c2d4e6"
    const val SEALED_ADMIN_KEY_HASH =
        "pbkdf2_sha256\$5019f24c8b09afe1ffa1db41b29f946b8a5c1481af5f562ed2b81c777585e3e9"
    const val LEGACY_SEALED_ADMIN_KEY_HASH =
        "pbkdf2_sha256\$6cbdbb1776289eeec9c6fb5c6f82cc102a7772a9f3b0210203ba90b1dd74e8f9"

    const val SEALED_SECRET_ADMIN_USER_SALT = "4c8a1e5b9d2f6a0c3e7b1d5f9a2c6e0b4d8f2a6c0e4b8d2f6a0c4e8b2d6f0a4c"
    const val SEALED_SECRET_ADMIN_USER_HASH =
        "pbkdf2_sha256\$6120ec86ef8b18fde6b80c91b9bcb7cea1692cce701a26bd63d942c0ab5d05d1"

    const val SEALED_SECRET_ADMIN_PASS_SALT = "7e1c9a3f5b2d8e4a6c0f2b4d6e8a0c2f4b6d8e0a2c4f6b8d0e2a4c6f8b0d2e4a"
    const val SEALED_SECRET_ADMIN_PASS_HASH =
        "pbkdf2_sha256\$add6af28b0b5bbecff1e23cc93b14499204a805dc23d15706de6ca380ec0504e"

    fun generateSalt(byteCount: Int = 32): String {
        val bytes = ByteArray(byteCount)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Derives a hardened PBKDF2-HMAC-SHA256 hash (120,000 iterations, 256-bit key).
     */
    fun hashSecret(secret: String, salt: String): String {
        return PBKDF2_PREFIX + derivePbkdf2Hex(secret, salt)
    }

    /**
     * Legacy SHA-256 hash kept strictly for verifying pre-existing user rows created before upgrade.
     */
    private fun legacySha256Hash(secret: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$salt:$secret".toByteArray(Charsets.UTF_8)
        return digest.digest(input).joinToString("") { "%02x".format(it) }
    }

    private fun derivePbkdf2Hex(secret: String, salt: String): String {
        val saltBytes = salt.toByteArray(Charsets.UTF_8)
        val derived = runCatching {
            val spec = PBEKeySpec(
                secret.toCharArray(),
                saltBytes,
                PBKDF2_ITERATIONS,
                PBKDF2_KEY_BITS
            )
            try {
                val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                factory.generateSecret(spec).encoded
            } finally {
                spec.clearPassword()
            }
        }.getOrElse {
            // Pure RFC-2898 PBKDF2-HMAC-SHA256 fallback for Android API 24-25 where SecretKeyFactory lacks PBKDF2WithHmacSHA256
            derivePbkdf2HmacSha256Fallback(secret.toByteArray(Charsets.UTF_8), saltBytes, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS / 8)
        }
        return derived.joinToString("") { "%02x".format(it) }
    }

    internal fun derivePbkdf2HmacSha256Fallback(
        password: ByteArray,
        salt: ByteArray,
        iterations: Int,
        dkLen: Int
    ): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(password, "HmacSHA256"))
        val hLen = mac.macLength
        val blocks = (dkLen + hLen - 1) / hLen
        val out = ByteArray(blocks * hLen)
        val saltBlock = ByteArray(salt.size + 4)
        System.arraycopy(salt, 0, saltBlock, 0, salt.size)

        for (blockIndex in 1..blocks) {
            saltBlock[salt.size] = (blockIndex ushr 24).toByte()
            saltBlock[salt.size + 1] = (blockIndex ushr 16).toByte()
            saltBlock[salt.size + 2] = (blockIndex ushr 8).toByte()
            saltBlock[salt.size + 3] = blockIndex.toByte()

            var u = mac.doFinal(saltBlock)
            val t = u.copyOf()
            for (i in 1 until iterations) {
                u = mac.doFinal(u)
                for (j in t.indices) {
                    t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
                }
            }
            System.arraycopy(t, 0, out, (blockIndex - 1) * hLen, hLen)
        }
        return out.copyOf(dkLen)
    }

    /**
     * Constant-time verification supporting both PBKDF2-HMAC-SHA256 and legacy SHA-256 hashes.
     */
    fun verifySecret(secret: String, salt: String, expectedHash: String): Boolean {
        if (expectedHash.isBlank() || salt.isBlank()) return false
        val actual = if (expectedHash.startsWith(PBKDF2_PREFIX)) {
            hashSecret(secret, salt)
        } else {
            legacySha256Hash(secret, salt)
        }
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }
}
