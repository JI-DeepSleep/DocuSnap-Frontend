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
                            }
                            "error" -> {
                                // Update job with error
                                val errorDetail = response.error_detail ?: "Unknown error"
                                jobDao.updateJobStatus(job.id, "error", errorDetail = errorDetail)
                                Log.e(TAG, "Job ${job.id} failed with error: $errorDetail")
                                Log.e(TAG, "Job ${job.id} request details: clientId=${job.clientId}, type=${job.type}, sha256=${job.sha256}, hasContent=${job.hasContent}")
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
                            }
                            "error" -> {
                                // Update job with error
                                val errorDetail = response.error_detail ?: "Unknown error"
                                jobDao.updateJobStatus(job.id, "error", errorDetail = errorDetail)
                                Log.e(TAG, "Job ${job.id} failed with error: $errorDetail")
                                Log.e(TAG, "Job ${job.id} request details: clientId=${job.clientId}, type=${job.type}, sha256=${job.sha256}, hasContent=${job.hasContent}")
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
    
    // Helper function to create a new job with proper encryption and SHA256
    suspend fun createJob(
        type: String,
        payload: Any,
        hasContent: Boolean = true
    ): Long {
        try {
            // Create the inner payload structure
            val innerPayload = mapOf(
                "to_process" to payload,
                // TODO: implement file_lib when available
                // "file_lib" to fileLibrary
            )
            val innerJson = org.json.JSONObject(innerPayload).toString()
            
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