package cn.edu.sjtu.deepsleep.docusnap.util

import java.security.MessageDigest
import java.util.Base64

class CryptoUtil {
    // --- Mock encryption for development ---
    // In production, use AES/CBC/PKCS5Padding + RSA for keys + real secure random IVs, etc.

    // Returns Pair<encryptedBase64, rsaEncryptedAesKey>
    fun encryptAndEncode(plain: String): Pair<String, String> {
        // For mocking, just base64 encode the string
        val base64 = Base64.getEncoder().encodeToString(plain.toByteArray(Charsets.UTF_8))
        // Mock the AES key as a string
        val aesKey = "mocked_aes_key"
        val rsaEncryptedKey = Base64.getEncoder().encodeToString(aesKey.toByteArray(Charsets.UTF_8))
        return Pair(base64, rsaEncryptedKey)
    }

    // Decrypts the result using the mock key (in reality, use real AES key and base64 decode then AES decryption)
    fun decryptAndDecode(encryptedBase64: String, aesKey: String): String {
        // For mocking, just base64 decode
        return String(Base64.getDecoder().decode(encryptedBase64), Charsets.UTF_8)
    }

    // Generate SHA256 of a string (hex)
    fun sha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}