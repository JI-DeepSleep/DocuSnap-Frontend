package cn.edu.sjtu.deepsleep.docusnap.service

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import cn.edu.sjtu.deepsleep.docusnap.data.FileType
import cn.edu.sjtu.deepsleep.docusnap.data.FormField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

class BackendApiService(private val context: Context) {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val backendUrl = "https://docusnap.zjyang.dev/api/v1"
    
    // Cache for AES keys
    private val aesKeyCache = mutableMapOf<String, ByteArray>()
    
    // RSA public key (you'll need to get this from your backend)
    private val publicKeyPem = """
        -----BEGIN PUBLIC KEY-----
        YOUR_PUBLIC_KEY_HERE
        -----END PUBLIC KEY-----
    """.trimIndent()

    sealed class ProcessingResult {
        data class Processing(val sha256: String) : ProcessingResult()
        data class Success(val result: String, val sha256: String) : ProcessingResult()
        data class Error(val message: String) : ProcessingResult()
    }

    // Check server status
    suspend fun checkServerStatus(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$backendUrl/check_status")
                .get()
                .build()

            val response = client.newCall(request).execute()
            return@withContext response.isSuccessful
        } catch (e: Exception) {
            Log.e("BackendApiService", "Error checking server status: ${e.message}", e)
            return@withContext false
        }
    }

    // Submit a processing job
    suspend fun submitJob(
        type: String, // "doc", "form", "fill"
        images: List<Bitmap>? = null,
        formData: Map<String, Any>? = null,
        excludeType: FileType? = null,
        excludeId: String? = null,
        fileLibJson: String? = null
    ): ProcessingResult = withContext(Dispatchers.IO) {
        try {
            val clientId = UUID.randomUUID().toString()
            
            // Prepare the content payload
            val contentPayload = JSONObject().apply {
                when (type) {
                    "doc", "form" -> {
                        val base64Images = images?.map { bitmapToBase64(it) } ?: emptyList()
                        put("to_process", JSONArray(base64Images))
                    }
                    "fill" -> {
                        put("to_process", JSONObject(formData))
                    }
                }
                
                // Add file_lib if provided
                if (fileLibJson != null) {
                    put("file_lib", JSONObject(fileLibJson))
                }
            }

            val contentJson = contentPayload.toString()
            Log.d("BackendApiService", "Content payload: $contentJson")

            // Generate encryption keys
            val aesKey = generateAesKey()
            val encryptedContent = aesEncrypt(contentJson.toByteArray(), aesKey)
            val encryptedContentBase64 = Base64.encodeToString(encryptedContent, Base64.NO_WRAP)
            
            // Compute SHA256
            val sha256 = computeSHA256(encryptedContent)
            
            // Encrypt AES key with RSA
            val encryptedAesKey = rsaEncrypt(aesKey, getPublicKey(publicKeyPem))
            val encryptedAesKeyBase64 = Base64.encodeToString(encryptedAesKey, Base64.NO_WRAP)

            // Cache AES key
            aesKeyCache[sha256] = aesKey

            // Build request
            val requestBody = JSONObject().apply {
                put("client_id", clientId)
                put("type", type)
                put("SHA256", sha256)
                put("has_content", true)
                put("content", encryptedContentBase64)
                put("aes_key", encryptedAesKeyBase64)
            }.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("$backendUrl/process")
                .post(requestBody)
                .build()

            Log.d("BackendApiService", "Submitting job: type=$type, sha256=$sha256")

            // Execute request
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext ProcessingResult.Error("Empty response")
            val jsonResponse = JSONObject(responseBody)

            Log.d("BackendApiService", "Response: $responseBody")

            return@withContext when (jsonResponse.getString("status")) {
                "processing" -> ProcessingResult.Processing(sha256)
                "completed" -> {
                    val result = jsonResponse.getString("result")
                    ProcessingResult.Success(result, sha256)
                }
                else -> ProcessingResult.Error("Processing error: ${jsonResponse.optString("error_detail")}")
            }
        } catch (e: Exception) {
            Log.e("BackendApiService", "Error submitting job: ${e.message}", e)
            return@withContext ProcessingResult.Error("Job submission failed: ${e.message}")
        }
    }

    // Poll job status
    suspend fun pollJobStatus(sha256: String): ProcessingResult = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("SHA256", sha256)
            }.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("$backendUrl/process")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext ProcessingResult.Error("Empty response")
            val jsonResponse = JSONObject(responseBody)

            return@withContext when (jsonResponse.getString("status")) {
                "processing" -> ProcessingResult.Processing(sha256)
                "completed" -> {
                    val encryptedResult = jsonResponse.getString("result")
                    val decryptedResult = decryptResult(encryptedResult, sha256)
                    ProcessingResult.Success(decryptedResult, sha256)
                }
                else -> ProcessingResult.Error("Processing error: ${jsonResponse.optString("error_detail")}")
            }
        } catch (e: Exception) {
            Log.e("BackendApiService", "Error polling job status: ${e.message}", e)
            return@withContext ProcessingResult.Error("Status polling failed: ${e.message}")
        }
    }

    // Utility functions
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun generateAesKey(): ByteArray {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        return keyGenerator.generateKey().encoded
    }

    private fun aesEncrypt(data: ByteArray, key: ByteArray): ByteArray {
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(data)
    }

    private fun aesDecrypt(data: String, key: ByteArray): String {
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decryptedBytes = cipher.doFinal(Base64.decode(data, Base64.NO_WRAP))
        return String(decryptedBytes)
    }

    private fun computeSHA256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun getPublicKey(publicKeyPem: String): java.security.PublicKey {
        val keyBytes = publicKeyPem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .replace(" ", "")
        val decodedKey = Base64.decode(keyBytes, Base64.NO_WRAP)
        val keySpec = X509EncodedKeySpec(decodedKey)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec)
    }

    private fun rsaEncrypt(data: ByteArray, publicKey: java.security.PublicKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(data)
    }

    fun decryptResult(encryptedResult: String, sha256: String): String {
        val aesKey = aesKeyCache[sha256] ?: throw IllegalStateException("AES key not found")
        return aesDecrypt(encryptedResult, aesKey)
    }
}