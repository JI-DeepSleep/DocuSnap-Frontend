package cn.edu.sjtu.deepsleep.docusnap.service

import android.content.Context
import android.util.Log
import cn.edu.sjtu.deepsleep.docusnap.data.local.JobEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class JobPollingService(
    private val context: Context,
    private val deviceDBService: DeviceDBService,
    private val backendApiService: BackendApiService
) {
    private val pollingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private val pollingInterval = 5000L // 5 seconds

    fun startPolling() {
        if (pollingJob?.isActive == true) {
            Log.d("JobPollingService", "Polling already active")
            return
        }

        pollingJob = pollingScope.launch {
            Log.d("JobPollingService", "Starting job polling service")
            
            while (isActive) {
                try {
                    pollPendingJobs()
                    delay(pollingInterval)
                } catch (e: Exception) {
                    Log.e("JobPollingService", "Error in polling loop: ${e.message}", e)
                    delay(pollingInterval * 2) // Wait longer on error
                }
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        Log.d("JobPollingService", "Job polling service stopped")
    }

    private suspend fun pollPendingJobs() {
        val pendingJobs = deviceDBService.getPendingJobs()
        
        for (job in pendingJobs) {
            try {
                Log.d("JobPollingService", "Polling job: ${job.sha256}")
                
                when (val result = backendApiService.pollJobStatus(job.sha256)) {
                    is BackendApiService.ProcessingResult.Processing -> {
                        // Job is still processing, update timestamp
                        val updatedJob = job.copy(
                            status = "processing",
                            updatedAt = System.currentTimeMillis()
                        )
                        deviceDBService.updateJob(updatedJob)
                        Log.d("JobPollingService", "Job still processing: ${job.sha256}")
                    }
                    
                    is BackendApiService.ProcessingResult.Success -> {
                        // Job completed successfully
                        val updatedJob = job.copy(
                            status = "completed",
                            result = result.result,
                            updatedAt = System.currentTimeMillis()
                        )
                        deviceDBService.updateJob(updatedJob)
                        
                        // Process the result and update database
                        processJobResult(job, result.result)
                        Log.d("JobPollingService", "Job completed: ${job.sha256}")
                    }
                    
                    is BackendApiService.ProcessingResult.Error -> {
                        // Job failed
                        val updatedJob = job.copy(
                            status = "error",
                            errorDetail = result.message,
                            updatedAt = System.currentTimeMillis()
                        )
                        deviceDBService.updateJob(updatedJob)
                        Log.e("JobPollingService", "Job failed: ${job.sha256}, error: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("JobPollingService", "Error polling job ${job.sha256}: ${e.message}", e)
                
                // Mark job as error if polling fails repeatedly
                val updatedJob = job.copy(
                    status = "error",
                    errorDetail = "Polling error: ${e.message}",
                    updatedAt = System.currentTimeMillis()
                )
                deviceDBService.updateJob(updatedJob)
            }
        }
    }

    private suspend fun processJobResult(job: JobEntity, resultJson: String) {
        try {
            val result = JSONObject(resultJson)
            
            when (job.type) {
                "doc" -> processDocumentResult(job, result)
                "form" -> processFormResult(job, result)
                "fill" -> processFillResult(job, result)
                else -> Log.w("JobPollingService", "Unknown job type: ${job.type}")
            }
        } catch (e: Exception) {
            Log.e("JobPollingService", "Error processing job result: ${e.message}", e)
        }
    }

    private suspend fun processDocumentResult(job: JobEntity, result: JSONObject) {
        val documentId = UUID.randomUUID().toString()
        val documentData = JSONObject().apply {
            put("name", result.optString("title"))
            put("description", result.optString("description"))
            put("imageUris", "[]") // Will be updated when we have image handling
            put("extractedInfo", result.optJSONObject("kv") ?: JSONObject())
            put("tags", result.optJSONArray("tags") ?: JSONArray())
            put("uploadDate", java.time.LocalDate.now().toString())
            put("relatedFileIds", "[]") // Will be updated when we have related file handling
            put("sha256", job.sha256)
            put("isProcessed", true)
        }
        
        deviceDBService.saveDocument(documentId, documentData)
        Log.d("JobPollingService", "Document saved: $documentId")
    }

    private suspend fun processFormResult(job: JobEntity, result: JSONObject) {
        val formId = UUID.randomUUID().toString()
        val formData = JSONObject().apply {
            put("name", result.optString("title"))
            put("description", result.optString("description"))
            put("imageUris", "[]") // Will be updated when we have image handling
            put("formFields", result.optJSONArray("fields") ?: JSONArray())
            put("extractedInfo", result.optJSONObject("kv") ?: JSONObject())
            put("tags", result.optJSONArray("tags") ?: JSONArray())
            put("uploadDate", java.time.LocalDate.now().toString())
            put("relatedFileIds", "[]") // Will be updated when we have related file handling
            put("sha256", job.sha256)
            put("isProcessed", true)
        }
        
        deviceDBService.saveForm(formId, formData)
        Log.d("JobPollingService", "Form saved: $formId")
    }

    private suspend fun processFillResult(job: JobEntity, result: JSONObject) {
        // For fill results, we need to update the existing form with the filled data
        // This is a simplified implementation - you may need to enhance this based on your requirements
        Log.d("JobPollingService", "Fill result processed: ${result.toString()}")
        
        // TODO: Implement form field filling logic
        // This would involve:
        // 1. Finding the target form using job metadata
        // 2. Updating the form fields with the filled values
        // 3. Marking the form as filled
    }
} 