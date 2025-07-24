package cn.edu.sjtu.deepsleep.docusnap.service

import android.content.Context
import androidx.room.Room
import cn.edu.sjtu.deepsleep.docusnap.data.local.AppDatabase
import cn.edu.sjtu.deepsleep.docusnap.data.local.DocumentEntity
import cn.edu.sjtu.deepsleep.docusnap.data.local.FormEntity
import cn.edu.sjtu.deepsleep.docusnap.data.FormField
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.content.ContentValues
import android.util.Base64
import android.util.Log
import android.widget.Toast
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import androidx.navigation.Navigation.findNavController
import cn.edu.sjtu.deepsleep.docusnap.data.SettingsManager
import cn.edu.sjtu.deepsleep.docusnap.data.ExtractedInfoItem
import cn.edu.sjtu.deepsleep.docusnap.data.FileType

class DeviceDBService(private val context: Context) {
    private val db: AppDatabase by lazy {
        Log.d("DeviceDBService", "Initializing Room database...")
        try {
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "docusnap.db"
            )
                .fallbackToDestructiveMigration()
                .build().also {
                    Log.d("DeviceDBService", "Room database initialized successfully")
                }
        } catch (e: Exception) {
            Log.e("DeviceDBService", "Error initializing database: ${e.message}", e)
            throw e
        }
    }
    private val documentDao get() = db.documentDao()
    private val formDao get() = db.formDao()

    // Document storage operations
    suspend fun saveDocument(documentId: String, data: JSONObject) {
        Log.d("DeviceDBService", "Saving document: $documentId")
        try {
            // Convert extractedInfo to new format if it's still in old format
            val extractedInfo = data.optJSONObject("extractedInfo")
            val extractedInfoString = if (extractedInfo != null) {
                try {
                    // Try to parse as new format first
                    Json.decodeFromString<List<ExtractedInfoItem>>(extractedInfo.toString())
                    extractedInfo.toString()
                } catch (e: Exception) {
                    // Convert from old Map format to new List format
                    val items = mutableListOf<ExtractedInfoItem>()
                    extractedInfo.keys().forEach { key ->
                        val value = extractedInfo.optString(key, "")
                        if (value.isNotEmpty()) {
                            items.add(ExtractedInfoItem(key = key, value = value))
                        }
                    }
                    Json.encodeToString(items)
                }
            } else {
                "[]"
            }
            
            val entity = DocumentEntity(
                id = documentId,
                name = data.optString("name"),
                description = data.optString("description"),
                imageBase64s = data.optString("imageBase64s", "[]"),
                extractedInfo = extractedInfoString,
                tags = data.optString("tags", "[]"),
                uploadDate = data.optString("uploadDate"),
                relatedFileIds = data.optString("relatedFileIds", "[]"),
                sha256 = data.optString("sha256"),
                isProcessed = data.optBoolean("isProcessed", false),
                jobId = data.optLong("jobId").takeIf { it != 0L },
                usageCount = data.optInt("usageCount", 0),
                lastUsed = data.optString("lastUsed", data.optString("uploadDate"))
            )
            documentDao.insert(entity)
            Log.d("DeviceDBService", "Document saved successfully: $documentId")
        } catch (e: Exception) {
            Log.e("DeviceDBService", "Error saving document: ${e.message}", e)
            throw e
        }
    }

    suspend fun getDocument(documentId: String): JSONObject? {
        val entity = documentDao.getById(documentId) ?: return null
        return JSONObject().apply {
            put("id", entity.id)
            put("name", entity.name)
            put("description", entity.description)
            put("imageBase64s", entity.imageBase64s)
            put("extractedInfo", entity.extractedInfo)
            put("tags", entity.tags)
            put("uploadDate", entity.uploadDate)
            put("relatedFileIds", entity.relatedFileIds)
            put("sha256", entity.sha256)
            put("isProcessed", entity.isProcessed)
            put("jobId", entity.jobId ?: JSONObject.NULL)
            put("usageCount", entity.usageCount)
            put("lastUsed", entity.lastUsed)
        }
    }

    suspend fun incrementDocumentUsage(documentId: String, lastUsed: String) {
        Log.d("DeviceDBService","document ${documentId} usage updated")
        documentDao.incrementUsage(documentId, lastUsed)
    }

    suspend fun incrementFormUsage(formId: String, lastUsed: String) {
        formDao.incrementUsage(formId, lastUsed)
    }

    suspend fun updateDocument(documentId: String, updates: JSONObject) {
        Log.d("DeviceDBService.updateDocument", "Incoming updates JSON: ${updates.toString(2)}")
        val entity = documentDao.getById(documentId) ?: return
        val updated = entity.copy(
            name = updates.optString("name", entity.name),
            description = updates.optString("description", entity.description),
            imageBase64s = updates.optString("imageBase64s", entity.imageBase64s),
            extractedInfo = updates.optString("extractedInfo", entity.extractedInfo),
            tags = updates.optString("tags", entity.tags),
            uploadDate = updates.optString("uploadDate", entity.uploadDate),
            relatedFileIds = updates.optString("relatedFileIds", entity.relatedFileIds),
            sha256 = updates.optString("sha256", entity.sha256),
            isProcessed = updates.optBoolean("isProcessed", entity.isProcessed),
            jobId = updates.optLong("jobId").takeIf { it != 0L } ?: entity.jobId,
            usageCount = updates.optInt("usageCount", entity.usageCount),
            lastUsed = updates.optString("lastUsed", entity.lastUsed)
        )
        documentDao.update(updated)
    }

    suspend fun exportDocuments(documentIds: List<String>) {
        val entities = documentDao.getByIds(documentIds)
        entities.forEach { entity ->
            // CHANGED: Decode base64 strings instead of URIs
            val base64Images = Json.decodeFromString<List<String>>(entity.imageBase64s)
            base64Images.forEach { base64 ->
                saveBase64ToGallery(base64)
            }
        }
    }

    // Add to DeviceDBService.kt
    suspend fun getFileNameById(id: String): String? {
        return documentDao.getById(id)?.name ?: formDao.getById(id)?.name
    }

    suspend fun getFileUploadTimeById(id: String): String? {
        return try {
            // First try to get from documents
            documentDao.getById(id)?.uploadDate ?:
            // If not found in documents, try forms
            formDao.getById(id)?.uploadDate
        } catch (e: Exception) {
            Log.e("DeviceDBService", "Error getting upload date for file $id", e)
            null
        }
    }

    suspend fun getFileTypeById(id: String): String? {
        return when {
            documentDao.getById(id) != null -> "doc"
            formDao.getById(id) != null -> "form"
            else -> null
        }
    }

    suspend fun addRelatedFile(sourceId: String, targetId: String) {
        try {
            // Add to source document
            documentDao.getById(sourceId)?.let { docEntity ->
                val relatedIds = Json.decodeFromString<List<String>>(docEntity.relatedFileIds)
                if (!relatedIds.contains(targetId)) {
                    val updatedIds = relatedIds + targetId
                    documentDao.update(docEntity.copy(
                        relatedFileIds = Json.encodeToString(updatedIds)
                    ))
                }
            }

            // Add to source form
            formDao.getById(sourceId)?.let { formEntity ->
                val relatedIds = Json.decodeFromString<List<String>>(formEntity.relatedFileIds)
                if (!relatedIds.contains(targetId)) {
                    val updatedIds = relatedIds + targetId
                    formDao.update(formEntity.copy(
                        relatedFileIds = Json.encodeToString(updatedIds)
                    ))
                }
            }

            // Add to target document
            documentDao.getById(targetId)?.let { docEntity ->
                val relatedIds = Json.decodeFromString<List<String>>(docEntity.relatedFileIds)
                if (!relatedIds.contains(sourceId)) {
                    val updatedIds = relatedIds + sourceId
                    documentDao.update(docEntity.copy(
                        relatedFileIds = Json.encodeToString(updatedIds)
                    ))
                }
            }

            // Add to target form
            formDao.getById(targetId)?.let { formEntity ->
                val relatedIds = Json.decodeFromString<List<String>>(formEntity.relatedFileIds)
                if (!relatedIds.contains(sourceId)) {
                    val updatedIds = relatedIds + sourceId
                    formDao.update(formEntity.copy(
                        relatedFileIds = Json.encodeToString(updatedIds)
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e("DeviceDBService", "Error adding related file", e)
        }
    }

    suspend fun deleteDocuments(documentIds: List<String>) {
        documentDao.deleteByIds(documentIds)
    }

    suspend fun getDocumentGallery(): List<JSONObject> {
        Log.d("DeviceDBService", "Getting document gallery...")
        try {
            val entities = documentDao.getAll().first()
            Log.d("DeviceDBService", "Found ${entities.size} documents in database")
            return entities.map { entity ->
                JSONObject().apply {
                    put("id", entity.id)
                    put("name", entity.name)
                    put("description", entity.description)
                    // CHANGED: imageUris -> imageBase64s
                    put("imageBase64s", entity.imageBase64s)
                    put("extractedInfo", entity.extractedInfo)
                    put("tags", entity.tags)
                    put("uploadDate", entity.uploadDate)
                    put("relatedFileIds", entity.relatedFileIds)
                    put("sha256", entity.sha256)
                    put("isProcessed", entity.isProcessed)
                    put("usageCount", entity.usageCount)
                    put("lastUsed", entity.lastUsed)
                }
            }
        } catch (e: Exception) {
            Log.e("DeviceDBService", "Error getting document gallery: ${e.message}", e)
            throw e
        }
    }

    // Form data storage operations
    suspend fun saveForm(formId: String, data: JSONObject) {
        Log.d("DeviceDBService", "Saving form: $formId")
        try {
            // Convert extractedInfo to new format if it's still in old format
            val extractedInfo = data.optJSONObject("extractedInfo")
            val extractedInfoString = if (extractedInfo != null) {
                try {
                    // Try to parse as new format first
                    Json.decodeFromString<List<ExtractedInfoItem>>(extractedInfo.toString())
                    extractedInfo.toString()
                } catch (e: Exception) {
                    // Convert from old Map format to new List format
                    val items = mutableListOf<ExtractedInfoItem>()
                    extractedInfo.keys().forEach { key ->
                        val value = extractedInfo.optString(key, "")
                        if (value.isNotEmpty()) {
                            items.add(ExtractedInfoItem(key = key, value = value))
                        }
                    }
                    Json.encodeToString(items)
                }
            } else {
                "[]"
            }
            
            val entity = FormEntity(
                id = formId,
                name = data.optString("name"),
                description = data.optString("description"),
                imageBase64s = data.optString("imageBase64s", "[]"),
                formFields = data.optString("formFields", "[]"),
                extractedInfo = extractedInfoString,
                tags = data.optString("tags", "[]"),
                uploadDate = data.optString("uploadDate"),
                relatedFileIds = data.optString("relatedFileIds", "[]"),
                sha256 = data.optString("sha256"),
                isProcessed = data.optBoolean("isProcessed", false),
                usageCount = data.optInt("usageCount", 0),
                lastUsed = data.optString("lastUsed", data.optString("uploadDate"))
            )
            formDao.insert(entity)
            Log.d("DeviceDBService", "Form saved successfully: $formId")
        } catch (e: Exception) {
            Log.e("DeviceDBService", "Error saving form: ${e.message}", e)
            throw e
        }
    }

    suspend fun getForm(formId: String): JSONObject? {
        val entity = formDao.getById(formId) ?: return null
        return JSONObject().apply {
            put("id", entity.id)
            put("name", entity.name)
            put("description", entity.description)
            put("imageBase64s", entity.imageBase64s)
            put("formFields", entity.formFields)
            put("extractedInfo", entity.extractedInfo)
            put("tags", entity.tags)
            put("uploadDate", entity.uploadDate)
            put("relatedFileIds", entity.relatedFileIds)
            put("sha256", entity.sha256)
            put("isProcessed", entity.isProcessed)
            put("usageCount", entity.usageCount)
            put("lastUsed", entity.lastUsed)
        }
    }

    suspend fun updateForm(formId: String, updates: JSONObject) {
        val entity = formDao.getById(formId) ?: return
        val updated = entity.copy(
            name = updates.optString("name", entity.name),
            description = updates.optString("description", entity.description),
            imageBase64s = updates.optString("imageBase64s", entity.imageBase64s),
            formFields = updates.optString("formFields", entity.formFields),
            extractedInfo = updates.optString("extractedInfo", entity.extractedInfo),
            tags = updates.optString("tags", entity.tags),
            uploadDate = updates.optString("uploadDate", entity.uploadDate),
            relatedFileIds = updates.optString("relatedFileIds", entity.relatedFileIds),
            sha256 = updates.optString("sha256", entity.sha256),
            isProcessed = updates.optBoolean("isProcessed", entity.isProcessed),
            usageCount = updates.optInt("usageCount", entity.usageCount),
            lastUsed = updates.optString("lastUsed", entity.lastUsed)
        )
        formDao.update(updated)
    }

    suspend fun exportForms(formIds: List<String>) {
        val entities = formDao.getByIds(formIds)
        entities.forEach { entity ->
            // CHANGED: Decode base64 strings instead of URIs
            val base64Images = Json.decodeFromString<List<String>>(entity.imageBase64s)
            base64Images.forEach { base64 ->
                saveBase64ToGallery(base64)
            }
        }
    }

    suspend fun deleteForms(formIds: List<String>) {
        formDao.deleteByIds(formIds)
    }

    suspend fun getFormGallery(): List<JSONObject> {
        return formDao.getAll().first().map { entity ->
            JSONObject().apply {
                put("id", entity.id)
                put("name", entity.name)
                put("description", entity.description)
                // CHANGED: imageUris -> imageBase64s
                put("imageBase64s", entity.imageBase64s)
                put("formFields", entity.formFields)
                put("extractedInfo", entity.extractedInfo)
                put("tags", entity.tags)
                put("uploadDate", entity.uploadDate)
                put("relatedFileIds", entity.relatedFileIds)
                put("sha256", entity.sha256)
                put("isProcessed", entity.isProcessed)
                put("usageCount", entity.usageCount)
                put("lastUsed", entity.lastUsed)
            }
        }
    }


    suspend fun exportDatabaseToJson(excludeType: String, excludeId: String): String {
        Log.d("DeviceDBService", "Exporting database to JSON. Exclude type: $excludeType, exclude id: $excludeId")
        try {
            val allDocuments = documentDao.getAll().first()
            val allForms = formDao.getAll().first()

            val filteredDocuments = if (excludeType == "doc" && excludeId.isNotEmpty()) {
                allDocuments.filter { it.id != excludeId }
            } else {
                allDocuments
            }

            val filteredForms = if (excludeType == "form" && excludeId.isNotEmpty()) {
                allForms.filter { it.id != excludeId }
            } else {
                allForms
            }

            val docList = filteredDocuments.map { doc ->
                JSONObject().apply {
                    put("id", doc.id)
                    put("name", doc.name)
                    put("description", doc.description)
                    put("extractedInfo", try {
                        val extractedInfo = Json.decodeFromString<List<ExtractedInfoItem>>(doc.extractedInfo)
                        JSONObject(extractedInfo.associate { it.key to it.value })
                    } catch (e: Exception) {
                        JSONObject()
                    })
                    put("tags", doc.tags)
                    put("uploadDate", doc.uploadDate)
                }
            }

            val formList = filteredForms.map { form ->
                JSONObject().apply {
                    put("id", form.id)
                    put("name", form.name)
                    put("description", form.description)
                    put("extractedInfo", try {
                        val extractedInfo = Json.decodeFromString<List<ExtractedInfoItem>>(form.extractedInfo)
                        JSONObject(extractedInfo.associate { it.key to it.value })
                    } catch (e: Exception) {
                        JSONObject()
                    })
                    put("formFields", form.formFields)
                    put("tags", form.tags)
                    put("uploadDate", form.uploadDate)
                }
            }

            val result = JSONObject().apply {
                put("doc", JSONArray(docList))
                put("form", JSONArray(formList))
            }

            return result.toString()
        } catch (e: Exception) {
            Log.e("DeviceDBService", "Error exporting database to JSON: ${e.message}", e)
            throw e
        }
    }

    // match query with related docs / forms / textual info
    suspend fun searchByQuery(query: String): List<JSONObject> {
        val docResults = documentDao.searchByQuery(query)
        val formResults = formDao.searchByQuery(query)
        val docJsons = docResults.map { entity ->
            Log.d("SERVICE_DEBUG", "Mapping a DOC with ID: ${entity.id}")
            JSONObject().apply {
                put("type", "document")
                put("id", entity.id)
                put("name", entity.name)
                put("description", entity.description)
                // CHANGED: imageUris -> imageBase64s
                put("imageBase64s", entity.imageBase64s)
                put("extractedInfo", entity.extractedInfo)
                put("tags", entity.tags)
                put("uploadDate", entity.uploadDate)
                put("relatedFileIds", entity.relatedFileIds)
                put("sha256", entity.sha256)
                put("isProcessed", entity.isProcessed)
            }
        }
        val formJsons = formResults.map { entity ->
            Log.d("SERVICE_DEBUG", "Mapping a FORM with ID: ${entity.id}")
            JSONObject().apply {
                put("type", "form")
                put("id", entity.id)
                put("name", entity.name)
                put("description", entity.description)
                // CHANGED: imageUris -> imageBase64s
                put("imageBase64s", entity.imageBase64s)
                put("formFields", entity.formFields)
                put("extractedInfo", entity.extractedInfo)
                put("tags", entity.tags)
                put("uploadDate", entity.uploadDate)
                put("relatedFileIds", entity.relatedFileIds)
                put("sha256", entity.sha256)
                put("isProcessed", entity.isProcessed)
            }
        }
        return docJsons + formJsons
    }

    suspend fun getFrequentTextInfo(): List<JSONObject> {
        try {
            val documents = documentDao.getAll().first()
            val forms = formDao.getAll().first()
            val settingsManager = SettingsManager(context)
            val maxCount = settingsManager.getCurrentSettings().frequentTextInfoCount
            
            val textInfoList = mutableListOf<JSONObject>()
            
            // Process documents
            documents.forEach { docEntity ->
                val extractedInfo = try {
                    Json.decodeFromString<List<ExtractedInfoItem>>(docEntity.extractedInfo)
                } catch (e: Exception) {
                    emptyList<ExtractedInfoItem>()
                }
                
                extractedInfo.forEach { item ->
                    if (item.value.isNotEmpty()) {
                        // Calculate combined frequency score
                        val combinedScore = calculateFrequencyScore(
                            itemUsageCount = item.usageCount,
                            fileUsageCount = docEntity.usageCount ?: 0,
                            itemLastUsed = item.lastUsed,
                            fileLastUsed = docEntity.lastUsed ?: docEntity.uploadDate
                        )
                        
                        textInfoList.add(JSONObject().apply {
                            put("key", item.key)
                            put("value", item.value)
                            put("srcFileId", docEntity.id)
                            put("srcFileType", "DOCUMENT")
                            put("usageCount", item.usageCount)
                            put("lastUsed", item.lastUsed)
                            put("combinedScore", combinedScore)
                        })
                    }
                }
            }
            
            // Process forms
            forms.forEach { formEntity ->
                val extractedInfo = try {
                    Json.decodeFromString<List<ExtractedInfoItem>>(formEntity.extractedInfo)
                } catch (e: Exception) {
                    emptyList<ExtractedInfoItem>()
                }
                
                extractedInfo.forEach { item ->
                    if (item.value.isNotEmpty()) {
                        // Calculate combined frequency score
                        val combinedScore = calculateFrequencyScore(
                            itemUsageCount = item.usageCount,
                            fileUsageCount = formEntity.usageCount ?: 0,
                            itemLastUsed = item.lastUsed,
                            fileLastUsed = formEntity.lastUsed ?: formEntity.uploadDate
                        )
                        
                        textInfoList.add(JSONObject().apply {
                            put("key", item.key)
                            put("value", item.value)
                            put("srcFileId", formEntity.id)
                            put("srcFileType", "FORM")
                            put("usageCount", item.usageCount)
                            put("lastUsed", item.lastUsed)
                            put("combinedScore", combinedScore)
                        })
                    }
                }
            }
            
            // Sort by combined frequency score and trim to maxCount
            return textInfoList
                .sortedByDescending { it.getDouble("combinedScore") }
                .take(maxCount)
                
        } catch (e: Exception) {
            Log.e("DeviceDBService", "Error getting frequent text info: ${e.message}", e)
            return emptyList()
        }
    }
    
    private fun calculateFrequencyScore(
        itemUsageCount: Int,
        fileUsageCount: Int,
        itemLastUsed: String,
        fileLastUsed: String
    ): Double {
        // Weight factors for different components
        val itemWeight = 0.7 // Individual item usage has higher weight
        val fileWeight = 0.3 // File usage has lower weight
        val recencyWeight = 0.1 // Recency has small weight
        
        // Calculate recency score (days since last used, lower is better)
        val currentDate = java.time.LocalDate.now()
        val itemDays = try {
            java.time.LocalDate.parse(itemLastUsed).until(currentDate).days
        } catch (e: Exception) {
            365 // Default to 1 year if parsing fails
        }
        val fileDays = try {
            java.time.LocalDate.parse(fileLastUsed).until(currentDate).days
        } catch (e: Exception) {
            365 // Default to 1 year if parsing fails
        }
        
        // Recency score (1.0 for today, decreasing over time)
        val itemRecency = 1.0 / (1.0 + itemDays * 0.1)
        val fileRecency = 1.0 / (1.0 + fileDays * 0.1)
        
        // Combined score
        return (itemUsageCount * itemWeight + fileUsageCount * fileWeight) * 
               (1.0 + recencyWeight * (itemRecency + fileRecency) / 2.0)
    }

    // NEW: Helper to save base64 image to gallery
    private fun saveBase64ToGallery(base64: String) {
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            saveBitmapToGallery(bitmap)
        } catch (e: Exception) {
            Log.e("DeviceDBService", "Failed to save base64 image", e)
        }
    }

    // Existing helper modified to work with Bitmap
    private fun saveBitmapToGallery(bitmap: Bitmap) {
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "DocuSnap_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val itemUri = resolver.insert(imageCollection, contentValues)
            if (itemUri != null) {
                resolver.openOutputStream(itemUri)?.use { outStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)
                }
            }
        } catch (e: Exception) {
            Log.e("DeviceDBService", "Failed to save image to gallery", e)
        }
    }
}