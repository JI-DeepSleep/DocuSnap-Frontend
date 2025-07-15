// TODO: This is the code that is refactored from the webui mockup in the backend repo.
// Many things needs to be changed close to the end of this file.
// But for the functions closed to the beginning of the file, I'd say they are good to go.


package cn.edu.sjtu.deepsleep.docusnap.service

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import cn.edu.sjtu.deepsleep.docusnap.data.SettingsManager
import kotlinx.coroutines.flow.first
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
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


// Manager of all interactions with backend server

// Sealed class for processing results
sealed class ProcessingResult {
    data class Success(val result: String, val sha256: String) : ProcessingResult()
    data class Processing(val sha256: String) : ProcessingResult()
    data class Error(val message: String) : ProcessingResult()
}

// Manager of all interactions with backend server
class BackendApiService(private val context: Context) {
    private val settingsManager = SettingsManager(context)
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json".toMediaType()
    private val aesKeyCache = mutableMapOf<String, ByteArray>()
    private var lastFormResult: String? = null

    // Get the current backend URL from settings
    private suspend fun getBackendUrl(): String {
        return settingsManager.settings.first().backendUrl
    }

    // Get public key from settings
    private suspend fun getPublicKeyPem(): String {
        return """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqvRk6hI+G8RfuFC6nrxD
X3K7LJrTorEhkBwWfZH2rqbK0sjMDHtOleiFKmgr3rgzVbyqFXy/eTlbIvozcVge
brMcD9cRLXWfq/UerNpJKuZsjKHVeMop0Q1lS5AJkkZpFEQ0osGvKgJn1UTYiaS9
4sfHEW/AONmzWbZvQMseU15sxF26QYaNrMb9kc8BzBW6L73Quq6LZRHSqeF71JjA
Mw4OtvS9pxDaRbN1FzRYGLcA3iaxSEmbsloPXipBKZJntiO9zDNGI1EQOou2GUfB
BIeNH+P2EW+6e8khrwpafawTMyUkxpqBK0QL8/qWgc7FblVzSDfE43aJc9jnNm3A
rwIDAQAB
-----END PUBLIC KEY-----

        """.trimIndent()
        // TODO: implement the line below and delete the one above
//        return settingsManager.settings.first().publicKey
    }

    // Convert Bitmap to Base64 string
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    // Generate random AES key (32 bytes)
    private fun generateAesKey(): ByteArray {
        val key = ByteArray(32)
        SecureRandom().nextBytes(key)
        return key
    }

    // Convert PEM string to PublicKey
    private fun getPublicKey(publicKeyPem: String): PublicKey {
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
    private fun rsaEncrypt(data: ByteArray, publicKey: PublicKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(data)
    }

    // AES-CBC encryption
    private fun aesEncrypt(data: ByteArray, key: ByteArray): String {
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

        val encrypted = cipher.doFinal(data)
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    // AES-CBC decryption
    private fun aesDecrypt(encryptedData: String, key: ByteArray): String {
        val combined = Base64.decode(encryptedData, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, 16)
        val ciphertext = combined.copyOfRange(16, combined.size)

        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    // Compute SHA256 hash
    private fun computeSHA256(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(data.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Main processing function
    private suspend fun process(
        type: String,
        payload: Any
    ): ProcessingResult {
        val backendUrl = getBackendUrl()
        val publicKeyPem = getPublicKeyPem()

        try {
            // Create payload
            val innerPayload = mapOf(
                "to_process" to payload,
                // TODO: implement the line below
//                "file_lib" to  xxx
            )
            val innerJson = JSONObject(innerPayload).toString()

            // Generate encryption keys
            val aesKey = generateAesKey()
            val encryptedContent = aesEncrypt(innerJson.toByteArray(), aesKey)
            val sha256 = computeSHA256(encryptedContent)
            val encryptedAesKey = rsaEncrypt(aesKey, getPublicKey(publicKeyPem))
            val encryptedAesKeyBase64 = Base64.encodeToString(encryptedAesKey, Base64.NO_WRAP)

            // Cache AES key
            aesKeyCache[sha256] = aesKey

            // Build request
            val requestBody = JSONObject().apply {
                // TODO: implement the line below
//                put("client_id", settingsManager.getClientId())
                put("type", type)
                put("SHA256", sha256)
                put("has_content", true)
                put("aes_key", encryptedAesKeyBase64)
                put("content", encryptedContent)
            }.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("$backendUrl/process")
                .post(requestBody)
                .build()

            // Execute request
            val response = client.newCall(request).execute()
            val responseBody =
                response.body?.string() ?: return ProcessingResult.Error("Empty response")
            val jsonResponse = JSONObject(responseBody)

            return when (jsonResponse.getString("status")) {
                "processing" -> {
                    if (type == "form") lastFormResult = innerJson
                    ProcessingResult.Processing(sha256)
                }

                "completed" -> ProcessingResult.Success(
                    jsonResponse.getString("result"),
                    sha256
                )

                else -> ProcessingResult.Error("Processing error: ${jsonResponse.optString("error_detail")}")
            }
        } catch (e: Exception) {
            return ProcessingResult.Error("Processing failed: ${e.message}")
        }
    }

    // Document Handler
    // TODO: see comments for getResult
    suspend fun processDocument(images: List<Bitmap>): ProcessingResult {
        val base64Images = images.map { bitmapToBase64(it) }
        return process("doc", base64Images)
    }

    // Form Handler
    // TODO: see comments for getResult
    suspend fun processForm(images: List<Bitmap>): ProcessingResult {
        val base64Images = images.map { bitmapToBase64(it) }
        return process("form", base64Images)
    }

    // Auto-fill form
    // TODO: see comments for getResult
    suspend fun fillForm(): ProcessingResult {
        val formData = lastFormResult ?: return ProcessingResult.Error("No form result available")
        return process("fill", JSONObject(formData).toMap())
    }

    // Get result from server
    // TODO: this is just the logic copied form the webui, which is not gonna work in the frontend.
    // the correct way is to have a record in db which files are being processed, the type of process and SHA256 of that process,
    // and there should be some kind of multithread way to do this polling job in the background.
    // So, really this is kind of the code of that polling worker, and you should modify the processDocument, processForm and processFill functions to let them register the task into the db and let the polling working behind the scene
    suspend fun getResult(type: String, sha256: String): ProcessingResult {
        val backendUrl = getBackendUrl()

        try {
            val requestBody = JSONObject().apply {
                // TODO: implement the line below
//                put("client_id", settingsManager.getClientId())
                put("type", type)
                put("SHA256", sha256)
                put("has_content", false)
            }.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("$backendUrl/process")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody =
                response.body?.string() ?: return ProcessingResult.Error("Empty response")
            val jsonResponse = JSONObject(responseBody)

            return when (jsonResponse.getString("status")) {
                "processing" -> ProcessingResult.Processing(sha256)
                "completed" -> ProcessingResult.Success(
                    jsonResponse.getString("result"),
                    sha256
                )

                else -> ProcessingResult.Error("Error: ${jsonResponse.optString("error_detail")}")
            }
        } catch (e: Exception) {
            return ProcessingResult.Error("Request failed: ${e.message}")
        }
    }

    // Decrypt result using cached AES key
    fun decryptResult(encryptedResult: String, sha256: String): String {
        val aesKey = aesKeyCache[sha256] ?: throw IllegalStateException("AES key not found")
        return aesDecrypt(encryptedResult, aesKey)
    }

    // Extension function to convert JSONObject to Map
    private fun JSONObject.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        for (key in keys()) {
            map[key] = get(key)
        }
        return map
    }
}