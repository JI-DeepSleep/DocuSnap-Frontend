package cn.edu.sjtu.deepsleep.docusnap.service

import android.content.Context
import android.util.Log
import cn.edu.sjtu.deepsleep.docusnap.data.local.AppDatabase
import cn.edu.sjtu.deepsleep.docusnap.data.local.JobEntity
import kotlinx.coroutines.*
import java.util.*
import retrofit2.HttpException
import org.json.JSONObject
import cn.edu.sjtu.deepsleep.docusnap.data.SettingsManager
import cn.edu.sjtu.deepsleep.docusnap.util.CryptoUtil
import kotlinx.coroutines.flow.first
import android.util.Base64
import org.json.JSONArray
import javax.crypto.KeyGenerator

class JobPollingService(private val context: Context) {
    private val TAG = "JobPollingService"
    private val database by lazy { AppDatabase.getInstance(context) }
    private val jobDao by lazy { database.jobDao() }
    private val backendApi by lazy { BackendApiClient.create() }
    private val pollingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null

    // Generate a unique client ID for this device
    private val clientId = UUID.randomUUID().toString()

    fun startPolling() {
        if (pollingJob?.isActive == true) {
            Log.d(TAG, "Polling already active")
            return
        }

        pollingJob = pollingScope.launch {
            Log.d(TAG, "Polling loop started")
            while (isActive) {
                try {
                    Log.d(TAG, "Starting polling cycle...")
                    pollPendingJobs()
                    pollProcessingJobs()
                    Log.d(TAG, "Polling cycle completed, waiting 5 seconds...")
                    delay(5000) // Poll every 5 seconds
                } catch (e: Exception) {
                    Log.e(TAG, "Error in polling loop", e)
                    delay(60000) // Wait longer on error
                }
            }
            Log.d(TAG, "Polling loop ended")
        }
        Log.d(TAG, "Started job polling service")
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        Log.d(TAG, "Stopped job polling service")
    }

    fun isPollingActive(): Boolean {
        return pollingJob?.isActive == true
    }

    suspend fun pollPendingJobs() {
        try {
            Log.d(TAG, "Polling pending jobs...")
            val pendingJobs = jobDao.getPendingJobs().first()
            Log.d(TAG, "Found ${pendingJobs.size} pending jobs")

            pendingJobs.forEach { job ->
                try {
                    Log.d(TAG, "Processing pending job: ${job.id}")

                    // Call backend API
                    val request = ProcessRequest(
                        client_id = job.clientId,
                        type = job.type,
                        SHA256 = job.sha256,
                        has_content = job.hasContent,
                        content = job.content,
                        aes_key = job.aesKey
                    )

                    try {
                        val response = backendApi.processDocument(request)
                        Log.d(TAG, "Backend response for job ${job.id}: status=${response.status}")

                        when (response.status) {
                            "processing" -> {
                                // Update job status to processing
                                jobDao.updateJobStatus(job.id, "processing")
                                Log.d(TAG, "Job ${job.id} is now processing")
                            }
                            "completed" -> {
                                // Update job with result
                                jobDao.updateJobStatus(job.id, "completed", response.result)
                                Log.d(TAG, "Job ${job.id} completed successfully")
                                Log.d(TAG, "Job ${job.id} result length: ${response.result?.length ?: 0}")

                                // Print the encrypted result
                                Log.i(TAG, "=== JOB ${job.id} COMPLETED ===")
                                Log.i(TAG, "Encrypted result: ${response.result}")

                                // Try to decrypt and print the result
                                try {
                                    val decryptedResult = decryptJobResult(response.result ?: "", job)
                                    if (decryptedResult != null) {
                                        Log.i(TAG, "Decrypted result: $decryptedResult")
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Could not decrypt result (expected - need original AES key): ${e.message}")
                                }
                                Log.i(TAG, "=== END JOB ${job.id} RESULT ===")
                            }
                            "error" -> {
                                // Update job with error
                                val errorDetail = response.error_detail ?: "Unknown error"
                                jobDao.updateJobStatus(job.id, "error", errorDetail = errorDetail)
                                Log.e(TAG, "Job ${job.id} failed with error: $errorDetail")
                                Log.e(TAG, "Job ${job.id} request details: clientId=${job.clientId}, type=${job.type}, sha256=${job.sha256}, hasContent=${job.hasContent}")

                                // Print detailed error information
                                Log.e(TAG, "=== JOB ${job.id} ERROR ===")
                                Log.e(TAG, "Error detail: $errorDetail")
                                Log.e(TAG, "Request type: ${job.type}")
                                Log.e(TAG, "Request SHA256: ${job.sha256}")
                                Log.e(TAG, "Request hasContent: ${job.hasContent}")
                                Log.e(TAG, "=== END JOB ${job.id} ERROR ===")
                            }
                        }

                    } catch (e: HttpException) {
                        val errorBody = e.response()?.errorBody()?.string()
                        val errorDetail = try {
                            errorBody?.let { JSONObject(it).optString("error_detail", "Unknown error") } ?: "HTTP ${e.code()}: ${e.message()}"
                        } catch (jsonEx: Exception) {
                            "HTTP ${e.code()}: ${e.message()}"
                        }
                        jobDao.updateJobStatus(job.id, "error", errorDetail = errorDetail)
                        Log.e(TAG, "Job ${job.id} failed with HTTP error: $errorDetail")
                        Log.e(TAG, "Job ${job.id} request details: clientId=${job.clientId}, type=${job.type}, sha256=${job.sha256}, hasContent=${job.hasContent}")
                    }

                    // Small delay between requests to avoid overwhelming the server
                    delay(1000)

                } catch (e: Exception) {
                    Log.e(TAG, "Error processing job ${job.id}", e)
                    jobDao.updateJobStatus(job.id, "error", errorDetail = e.message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error polling pending jobs", e)
        }
    }

    suspend fun pollProcessingJobs() {
        try {
            Log.d(TAG, "Polling processing jobs...")
            val processingJobs = jobDao.getProcessingJobs().first()
            Log.d(TAG, "Found ${processingJobs.size} processing jobs")

            processingJobs.forEach { job ->
                try {
                    Log.d(TAG, "Checking processing job: ${job.id}")

                    // For processing jobs, we need to check status using SHA256
                    // This would require a different endpoint or we can use the same process endpoint
                    // For now, let's use the same endpoint but with has_content=false
                    val request = ProcessRequest(
                        client_id = job.clientId,
                        type = job.type,
                        SHA256 = job.sha256,
                        has_content = false
                    )

                    try {
                        val response = backendApi.processDocument(request)
                        Log.d(TAG, "Backend response for processing job ${job.id}: status=${response.status}")

                        when (response.status) {
                            "processing" -> {
                                // Still processing, no update needed
                                Log.d(TAG, "Job ${job.id} still processing")
                            }
                            "completed" -> {
                                // Update job with result
                                jobDao.updateJobStatus(job.id, "completed", response.result)
                                Log.d(TAG, "Job ${job.id} completed successfully")
                                Log.d(TAG, "Job ${job.id} result length: ${response.result?.length ?: 0}")

                                // Print the encrypted result
                                Log.i(TAG, "=== JOB ${job.id} COMPLETED (from processing) ===")
                                Log.i(TAG, "Encrypted result: ${response.result}")

                                // Try to decrypt and print the result
                                try {
                                    val decryptedResult = decryptJobResult(response.result ?: "", job)
                                    if (decryptedResult != null) {
                                        Log.i(TAG, "Decrypted result: $decryptedResult")
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Could not decrypt result (expected - need original AES key): ${e.message}")
                                }
                                Log.i(TAG, "=== END JOB ${job.id} RESULT ===")
                            }
                            "error" -> {
                                // Update job with error
                                val errorDetail = response.error_detail ?: "Unknown error"
                                jobDao.updateJobStatus(job.id, "error", errorDetail = errorDetail)
                                Log.e(TAG, "Job ${job.id} failed with error: $errorDetail")
                                Log.e(TAG, "Job ${job.id} request details: clientId=${job.clientId}, type=${job.type}, sha256=${job.sha256}, hasContent=${job.hasContent}")

                                // Print detailed error information
                                Log.e(TAG, "=== JOB ${job.id} ERROR (from processing) ===")
                                Log.e(TAG, "Error detail: $errorDetail")
                                Log.e(TAG, "Request type: ${job.type}")
                                Log.e(TAG, "Request SHA256: ${job.sha256}")
                                Log.e(TAG, "Request hasContent: ${job.hasContent}")
                                Log.e(TAG, "=== END JOB ${job.id} ERROR ===")
                            }
                        }

                    } catch (e: HttpException) {
                        val errorBody = e.response()?.errorBody()?.string()
                        val errorDetail = try {
                            errorBody?.let { JSONObject(it).optString("error_detail", "Unknown error") } ?: "HTTP ${e.code()}: ${e.message()}"
                        } catch (jsonEx: Exception) {
                            "HTTP ${e.code()}: ${e.message()}"
                        }
                        jobDao.updateJobStatus(job.id, "error", errorDetail = errorDetail)
                        Log.e(TAG, "Job ${job.id} failed with HTTP error: $errorDetail")
                        Log.e(TAG, "Job ${job.id} request details: clientId=${job.clientId}, type=${job.type}, sha256=${job.sha256}, hasContent=${job.hasContent}")
                    }

                    delay(1000)

                } catch (e: Exception) {
                    Log.e(TAG, "Error checking processing job ${job.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error polling processing jobs", e)
        }
    }

    // JobPollingService.kt
    fun decryptJobResult(encryptedResult: String, job: JobEntity): String? {
        return try {
            // 1. Check if we have the plain AES key
            if (job.plainAesKey.isNullOrEmpty()) {
                Log.e(TAG, "No plain AES key available for decryption")
                return null
            }

            // 2. Decode the plain AES key from Base64
            val aesKeyBytes = Base64.decode(job.plainAesKey, Base64.NO_WRAP)
            if (aesKeyBytes.isEmpty()) {
                Log.e(TAG, "Failed to decode AES key")
                return null
            }

            // 3. Reconstruct the AES key
            val keySpec = javax.crypto.spec.SecretKeySpec(aesKeyBytes, "AES")

            // 4. Initialize cipher with CBC mode and PKCS5 padding (matches web version)
            val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")

            // 5. Extract IV from the encrypted data (first 16 bytes)
            val encryptedBytes = Base64.decode(encryptedResult, Base64.NO_WRAP)
            if (encryptedBytes.size <= 16) {
                Log.e(TAG, "Invalid encrypted data length")
                return null
            }

            val iv = encryptedBytes.copyOfRange(0, 16)
            val encryptedData = encryptedBytes.copyOfRange(16, encryptedBytes.size)

            val parameterSpec = javax.crypto.spec.IvParameterSpec(iv)
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, parameterSpec)

            // 6. Decrypt the result
            val decryptedBytes = cipher.doFinal(encryptedData)

            // 7. Return the decrypted string
            String(decryptedBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting result: ${e.message}", e)
            null
        }
    }

    suspend fun createJob(
        type: String,
        id: String,
        payload: Any,
        hasContent: Boolean = true
    ): JobEntity {
        try {
            // Create inner payload structure
            val innerPayload = mutableMapOf<String, Any>(
                "to_process" to payload
            )

            // Get file_lib data (all documents/forms except specified exclusion)
            val deviceDBService = DeviceDBService(context)
            val dbJsonString = deviceDBService.exportDatabaseToJson(
                excludeType = type,
                excludeId = id
            )
            val dbJson = JSONObject(dbJsonString)

            // Convert to required format: "docs" and "forms" arrays
            val fileLib = JSONObject().apply {
                put("docs", dbJson.optJSONArray("doc") ?: JSONArray())
                put("forms", dbJson.optJSONArray("form") ?: JSONArray())
            }
            innerPayload["file_lib"] = fileLib

            // Convert to JSON string for encryption
            val innerJson = JSONObject(innerPayload as Map<*, *>).toString()

            // Generate encryption keys
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val secretKey = keyGenerator.generateKey()
            val aesKeyBytes = secretKey.encoded
            val encryptedContent = CryptoUtil.aesEncrypt(innerJson.toByteArray(), aesKeyBytes)
            val sha256 = CryptoUtil.computeSHA256(encryptedContent)

            // Encrypt AES key with backend public key
            val settingsManager = SettingsManager(context)
            val publicKeyPem = settingsManager.settings.first().backendPublicKey
            val publicKey = CryptoUtil.getPublicKey(publicKeyPem)
            val encryptedAesKey = CryptoUtil.rsaEncrypt(aesKeyBytes, publicKey)
            val encryptedAesKeyBase64 = Base64.encodeToString(encryptedAesKey, Base64.NO_WRAP)

            // Create job entity
            val job = JobEntity(
                clientId = clientId,
                type = type,
                sha256 = sha256,
                hasContent = hasContent,
                content = encryptedContent,
                aesKey = encryptedAesKeyBase64,
                plainAesKey = Base64.encodeToString(aesKeyBytes, Base64.NO_WRAP)
            )

            // Insert job into database
            val jobId = jobDao.insertJob(job)
            Log.d(TAG, "Created job with ID: $jobId, SHA256: $sha256")
            return job.copy(id = jobId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating job", e)
            throw e
        }
    }

    // Clean up old completed jobs (older than 7 days)
    suspend fun cleanupOldJobs(): Int {
        try {
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            val deletedCount = jobDao.deleteOldCompletedJobs(sevenDaysAgo)
            Log.d(TAG, "Cleaned up $deletedCount old completed jobs")
            return deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
            throw e
        }
    }
}