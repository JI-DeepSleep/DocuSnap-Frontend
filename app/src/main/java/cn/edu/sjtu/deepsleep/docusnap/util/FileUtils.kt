// FileUtils.kt
package cn.edu.sjtu.deepsleep.docusnap.util

import android.content.Context
import cn.edu.sjtu.deepsleep.docusnap.service.DeviceDBService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileUtils {
    suspend fun getFileNameById(context: Context, id: String): String? {
        return withContext(Dispatchers.IO) {
            val deviceDBService = DeviceDBService(context)
            deviceDBService.getFileNameById(id)
        }
    }

    suspend fun getFileUploadTimeById(context: Context, id: String): String? {
        return withContext(Dispatchers.IO) {
            val deviceDBService = DeviceDBService(context)
            deviceDBService.getFileUploadTimeById(id)
        }
    }

    suspend fun getFormattedTimeForFile(context: Context, id: String): String {
        return getFileUploadTimeById(context, id) ?: "Unknown date"
    }

    suspend fun getFileTypeById(context: Context, id: String): String? {
        return withContext(Dispatchers.IO) {
            val deviceDBService = DeviceDBService(context)
            deviceDBService.getFileTypeById(id)
        }
    }

    suspend fun navigateToFileDetail(
        context: Context,
        onNavigate: (String) -> Unit,
        id: String,
        type: String? = null
    ) {
        when (type) {
            "doc" -> onNavigate("document_detail?documentId=$id&fromImageProcessing=false")
            "form" -> onNavigate("form_detail?formId=$id&fromImageProcessing=false")
            else -> {
                // Try to determine the type if not provided
                val determinedType = getFileTypeById(context, id)
                when (determinedType) {
                    "doc" -> onNavigate("document_detail?documentId=$id&fromImageProcessing=false")
                    "form" -> onNavigate("form_detail?formId=$id&fromImageProcessing=false")
                    else -> onNavigate("document_detail?documentId=$id&fromImageProcessing=false") // Default fallback
                }
            }
        }
    }
}