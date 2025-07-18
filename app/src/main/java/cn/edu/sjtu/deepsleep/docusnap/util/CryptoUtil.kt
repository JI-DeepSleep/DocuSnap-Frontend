package cn.edu.sjtu.deepsleep.docusnap.util

import android.graphics.Bitmap
import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtil {
    
    // Convert Bitmap to Base64 string
    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    // Generate random AES key (32 bytes)
    fun generateAesKey(): ByteArray {
        val key = ByteArray(32)
        SecureRandom().nextBytes(key)
        return key
    }

    // Convert PEM string to PublicKey
    fun getPublicKey(publicKeyPem: String): PublicKey {
        val pemContent = publicKeyPem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\n", "")
            .trim()

        val keyBytes = Base64.decode(pemContent, Base64.NO_WRAP)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec)
    }

    // RSA encryption
    fun rsaEncrypt(data: ByteArray, publicKey: PublicKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(data)
    }

    // AES-CBC encryption
    fun aesEncrypt(data: ByteArray, key: ByteArray): String {
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

        val encrypted = cipher.doFinal(data)
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    // AES-CBC decryption
    fun aesDecrypt(encryptedData: String, key: ByteArray): String {
        val combined = Base64.decode(encryptedData, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, 16)
        val ciphertext = combined.copyOfRange(16, combined.size)

        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    // Compute SHA256 hash
    fun computeSHA256(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(data.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }


}