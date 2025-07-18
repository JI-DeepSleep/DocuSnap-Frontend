package cn.edu.sjtu.deepsleep.docusnap.service

import android.content.Context
import android.util.Log
import cn.edu.sjtu.deepsleep.docusnap.data.local.AppDatabase
import cn.edu.sjtu.deepsleep.docusnap.data.local.JobEntity
import kotlinx.coroutines.*
import java.util.*

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
            while (isActive) {
                try {
                    pollPendingJobs()
                    pollProcessingJobs()
                    delay(30000) // Poll every 30 seconds
                } catch (e: Exception) {
                    Log.e(TAG, "Error in polling loop", e)
                    delay(60000) // Wait longer on error
                }
            }
        }
        Log.d(TAG, "Started job polling service")
    }
    
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        Log.d(TAG, "Stopped job polling service")
    }
    
    private suspend fun pollPendingJobs() {
        try {
            jobDao.getPendingJobs().collect { pendingJobs ->
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
                        
                        val response = backendApi.processDocument(request)
                        
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
                            }
                            "error" -> {
                                // Update job with error
                                jobDao.updateJobStatus(job.id, "error", errorDetail = response.error_detail)
                                Log.e(TAG, "Job ${job.id} failed: ${response.error_detail}")
                            }
                        }
                        
                        // Small delay between requests to avoid overwhelming the server
                        delay(1000)
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing job ${job.id}", e)
                        jobDao.updateJobStatus(job.id, "error", errorDetail = e.message)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error polling pending jobs", e)
        }
    }
    
    private suspend fun pollProcessingJobs() {
        try {
            jobDao.getProcessingJobs().collect { processingJobs ->
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
                        
                        val response = backendApi.processDocument(request)
                        
                        when (response.status) {
                            "processing" -> {
                                // Still processing, no update needed
                                Log.d(TAG, "Job ${job.id} still processing")
                            }
                            "completed" -> {
                                // Update job with result
                                jobDao.updateJobStatus(job.id, "completed", response.result)
                                Log.d(TAG, "Job ${job.id} completed successfully")
                            }
                            "error" -> {
                                // Update job with error
                                jobDao.updateJobStatus(job.id, "error", errorDetail = response.error_detail)
                                Log.e(TAG, "Job ${job.id} failed: ${response.error_detail}")
                            }
                        }
                        
                        delay(1000)
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking processing job ${job.id}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error polling processing jobs", e)
        }
    }
    
    // Helper function to create a new job
    suspend fun createJob(
        type: String,
        sha256: String,
        hasContent: Boolean,
        content: String? = null,
        aesKey: String? = null
    ): Long {
        try {
            val job = JobEntity(
                clientId = clientId,
                type = type,
                sha256 = sha256,
                hasContent = hasContent,
                content = content,
                aesKey = aesKey
            )
            val jobId = jobDao.insertJob(job)
            Log.d(TAG, "Created job with ID: $jobId")
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