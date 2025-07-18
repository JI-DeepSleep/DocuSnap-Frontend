// TODO: This is the code that is refactored from the webui mockup in the backend repo.
// Many things needs to be changed close to the end of this file.
// But for the functions closed to the beginning of the file, I would say they are good to go.

package cn.edu.sjtu.deepsleep.docusnap.service

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import cn.edu.sjtu.deepsleep.docusnap.data.SettingsManager
import cn.edu.sjtu.deepsleep.docusnap.util.CryptoUtil
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
        return settingsManager.settings.first().backendPublicKey
    }

    // Convert Bitmap to Base64 string
    private fun bitmapToBase64(bitmap: Bitmap): String {
        return CryptoUtil.bitmapToBase64(bitmap)
    }

    // Generate random AES key (32 bytes)
    private fun generateAesKey(): ByteArray {
        return CryptoUtil.generateAesKey()
    }

    // Convert PEM string to PublicKey
    private fun getPublicKey(publicKeyPem: String): PublicKey {
        return CryptoUtil.getPublicKey(publicKeyPem)
    }

    // RSA encryption
    private fun rsaEncrypt(data: ByteArray, publicKey: PublicKey): ByteArray {
        return CryptoUtil.rsaEncrypt(data, publicKey)
    }

    // AES-CBC encryption
    private fun aesEncrypt(data: ByteArray, key: ByteArray): String {
        return CryptoUtil.aesEncrypt(data, key)
    }

    // AES-CBC decryption
    private fun aesDecrypt(encryptedData: String, key: ByteArray): String {
        return CryptoUtil.aesDecrypt(encryptedData, key)
    }

    // Compute SHA256 hash
    private fun computeSHA256(data: String): String {
        return CryptoUtil.computeSHA256(data)
    }

    // Main processing function called by the handlers
    // TODO: modify the main logic here. It should return on the first response it gets.
    // TODO: If complete, good. If error, do error handling, or actually it can just pretend the user never clicked the button.
    // TODO: Most of the cases, the response will be processing, should add this job  to the job table and let the daemon to the rest of the work.
    // TODO: I dont think this function needs a return type, since its whole job is to submit the job to the server.
    // TODO: If very lucky there's result (status=completed), then it should not have return values as well but just writes those results to the db.
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


    // TODO: !!!!! IMPORTANT !!!!!! The function finger prints are not set yet.
    // TODO: !!!!! IMPORTANT !!!!!! For example, the current processDocument is not that good
    // TODO: !!!!! IMPORTANT !!!!!! It cannot satisfy the function that serializing db to skip the file being passed.
    // TODO: !!!!! IMPORTANT !!!!!! You can change the arg and return type to anything make sense.
    // TODO: !!!!! IMPORTANT !!!!!!  I would say that having an id as argument is probably the best choice.
    //  Also, these three function handlers dont need a return type. Since they should be the sender in a async function call.
    //  They just need to make sure that process() returns (so the job is submitted to the server).

    // Document Handler
    // TODO: submit job once and add job to db. Let the db polling worker do the polling job.
    suspend fun processDocument(images: List<Bitmap>): ProcessingResult {
        val base64Images = images.map { bitmapToBase64(it) }
        return process("doc", base64Images)
    }

    // Form Handler
    // TODO: submit job once and add job to db. Let the db polling worker do the polling job.
    suspend fun processForm(images: List<Bitmap>): ProcessingResult {
        val base64Images = images.map { bitmapToBase64(it) }
        return process("form", base64Images)
    }

    // Auto-fill form
    // TODO: submit job once and add job to db. Let the db polling worker do the polling job.
    suspend fun fillForm(): ProcessingResult {
        val formData = lastFormResult ?: return ProcessingResult.Error("No form result available")
        return process("fill", JSONObject(formData).toMap())
    }


    // TODO: write a polling worker that runs whenever the app runs. It should check the db job table,
    // and call the api.
    // on result received should upload the db.
    // It should be called when the app starts and it should take both latency and power saving in mind


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

