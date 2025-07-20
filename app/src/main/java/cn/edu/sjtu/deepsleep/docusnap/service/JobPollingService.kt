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
import kotlinx.coroutines.flow.first

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
                    Log.d(TAG, "Polling cycle completed, waiting 30 seconds...")
                    delay(30000) // Poll every 30 seconds
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
    
    // Helper function to decrypt job result using the original AES key
    private fun decryptJobResult(encryptedResult: String, job: JobEntity): String? {
        return try {
            // Decode the encrypted AES key from base64
            val encryptedAesKeyBytes = android.util.Base64.decode(job.aesKey, android.util.Base64.NO_WRAP)
            
            // Decrypt the AES key using the private key (this would need to be implemented)
            // For now, we'll just return the encrypted result since we don't have the private key
            Log.w(TAG, "Cannot decrypt result - private key not available")
            encryptedResult
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting result: ${e.message}")
            null
        }
    }
    
    // Helper function to create a new job with proper encryption and SHA256
    suspend fun createJob(
        type: String,
        payload: Any,
        hasContent: Boolean = true
    ): Long {
        try {
            // Create the inner payload structure
            val innerPayload = mutableMapOf<String, Any>(
                "to_process" to payload
            )
            // Always include file_lib, default to empty arrays
            innerPayload["file_lib"] = mapOf(
                "docs" to emptyList<Any>(),
                "forms" to emptyList<Any>()
            )
            val innerJson = org.json.JSONObject(innerPayload as Map<*, *>?).toString()
            
            // Generate encryption keys
            val aesKey = cn.edu.sjtu.deepsleep.docusnap.util.CryptoUtil.generateAesKey()
            val encryptedContent = cn.edu.sjtu.deepsleep.docusnap.util.CryptoUtil.aesEncrypt(innerJson.toByteArray(), aesKey)
            val sha256 = cn.edu.sjtu.deepsleep.docusnap.util.CryptoUtil.computeSHA256(encryptedContent)
            
            // Use the default public key from settings
            val settingsManager = SettingsManager(context)
            val publicKeyPem = settingsManager.settings.first().backendPublicKey
            val publicKey = cn.edu.sjtu.deepsleep.docusnap.util.CryptoUtil.getPublicKey(publicKeyPem)
            val encryptedAesKey = cn.edu.sjtu.deepsleep.docusnap.util.CryptoUtil.rsaEncrypt(aesKey, publicKey)
            val encryptedAesKeyBase64 = android.util.Base64.encodeToString(encryptedAesKey, android.util.Base64.NO_WRAP)
            
            val job = JobEntity(
                clientId = clientId,
                type = type,
                sha256 = sha256,
                hasContent = hasContent,
                content = encryptedContent,
                aesKey = encryptedAesKeyBase64
            )
            val jobId = jobDao.insertJob(job)
            Log.d(TAG, "Created job with ID: $jobId, SHA256: $sha256")
            return jobId
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