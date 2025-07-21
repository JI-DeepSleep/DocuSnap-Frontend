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

// Manager of all interactions with local SQLite database

class DeviceDBService(private val context: Context) {
    private val db: AppDatabase by lazy {
        android.util.Log.d("DeviceDBService", "Initializing Room database...")
        try {
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "docusnap.db"
            )
            .fallbackToDestructiveMigration()
            .build().also {
                android.util.Log.d("DeviceDBService", "Room database initialized successfully")
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceDBService", "Error initializing database: ${e.message}", e)
            throw e
        }
    }
    private val documentDao get() = db.documentDao()
    private val formDao get() = db.formDao()

    // Document storage operations
    suspend fun saveDocument(documentId: String, data: JSONObject) {
        android.util.Log.d("DeviceDBService", "Saving document: $documentId")
        try {
            val entity = DocumentEntity(
                id = documentId,
                name = data.optString("name"),
                description = data.optString("description"),
                imageUris = data.optJSONArray("imageUris")?.toString() ?: "[]",
                extractedInfo = data.optJSONObject("extractedInfo")?.toString() ?: "{}",
//                tags = data.optString("tags", "[]"), //tags = data.optJSONArray("tags")?.toString() ?: "[]",
                tags = data.optJSONArray("tags")?.toString() ?: "[]",
                uploadDate = data.optString("uploadDate"),
                relatedFileIds = data.optJSONArray("relatedFileIds")?.toString() ?: "[]",
                sha256 = data.optString("sha256"),
                isProcessed = data.optBoolean("isProcessed", false)
            )
            documentDao.insert(entity)
            android.util.Log.d("DeviceDBService", "Document saved successfully: $documentId")
        } catch (e: Exception) {
            android.util.Log.e("DeviceDBService", "Error saving document: ${e.message}", e)
            throw e
        }
    }

    suspend fun getDocument(documentId: String): JSONObject? {
        val entity = documentDao.getById(documentId) ?: return null
        return JSONObject().apply {
            put("id", entity.id)
            put("name", entity.name)
            put("description", entity.description)
            put("imageUris", Json.decodeFromString<List<String>>(entity.imageUris))
            put("extractedInfo", JSONObject(entity.extractedInfo))
            put("tags", Json.decodeFromString<List<String>>(entity.tags))
            put("uploadDate", entity.uploadDate)
            put("relatedFileIds", Json.decodeFromString<List<String>>(entity.relatedFileIds))
            put("sha256", entity.sha256)
            put("isProcessed", entity.isProcessed)
        }
    }

    suspend fun updateDocument(documentId: String, updates: JSONObject) {
        val entity = documentDao.getById(documentId) ?: return
        val updated = entity.copy(
            name = updates.optString("name", entity.name),
            description = updates.optString("description", entity.description),
            imageUris = updates.optJSONArray("imageUris")?.toString() ?: entity.imageUris,
            extractedInfo = updates.optJSONObject("extractedInfo")?.toString() ?: entity.extractedInfo,
            tags = updates.optJSONArray("tags")?.toString() ?: entity.tags,
            uploadDate = updates.optString("uploadDate", entity.uploadDate),
            relatedFileIds = updates.optJSONArray("relatedFileIds")?.toString() ?: entity.relatedFileIds,
            sha256 = updates.optString("sha256", entity.sha256),
            isProcessed = updates.optBoolean("isProcessed", entity.isProcessed)
        )
        documentDao.update(updated)
    }

    suspend fun exportDocuments(documentIds: List<String>): List<JSONObject> {
        return documentDao.getByIds(documentIds).map { entity ->
            JSONObject().apply {
                put("id", entity.id)
                put("name", entity.name)
                put("description", entity.description)
                put("imageUris", Json.decodeFromString<List<String>>(entity.imageUris))
                put("extractedInfo", JSONObject(entity.extractedInfo))
                put("tags", Json.decodeFromString<List<String>>(entity.tags))
                put("uploadDate", entity.uploadDate)
                put("relatedFileIds", Json.decodeFromString<List<String>>(entity.relatedFileIds))
                put("sha256", entity.sha256)
                put("isProcessed", entity.isProcessed)
            }
        }
    }

    suspend fun deleteDocuments(documentIds: List<String>) {
        documentDao.deleteByIds(documentIds)
    }

    suspend fun getDocumentGallery(): List<JSONObject> {
        android.util.Log.d("DeviceDBService", "Getting document gallery...")
        try {
            val entities = documentDao.getAll().first()
            android.util.Log.d("DeviceDBService", "Found ${entities.size} documents in database")
            return entities.map { entity ->
                JSONObject().apply {
                    put("id", entity.id)
                    put("name", entity.name)
                    put("description", entity.description)
                    put("imageUris", Json.decodeFromString<List<String>>(entity.imageUris))
                    put("extractedInfo", JSONObject(entity.extractedInfo))
                    put("tags", Json.decodeFromString<List<String>>(entity.tags))
                    put("uploadDate", entity.uploadDate)
                    put("relatedFileIds", Json.decodeFromString<List<String>>(entity.relatedFileIds))
                    put("sha256", entity.sha256)
                    put("isProcessed", entity.isProcessed)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceDBService", "Error getting document gallery: ${e.message}", e)
            throw e
        }
    }

    // Form data storage operations
    suspend fun saveForm(formId: String, data: JSONObject) {
        android.util.Log.d("DeviceDBService", "Saving form: $formId")
        try {
            val entity = FormEntity(
                id = formId,
                name = data.optString("name"),
                description = data.optString("description"),
                imageUris = data.optJSONArray("imageUris")?.toString() ?: "[]",
                formFields = data.optJSONArray("formFields")?.toString() ?: "[]",
                extractedInfo = data.optJSONObject("extractedInfo")?.toString() ?: "{}",
//                tags = data.optString("tags", "[]"), // tags = data.optJSONArray("tags")?.toString() ?: "[]",
                tags = data.optJSONArray("tags")?.toString() ?: "[]",
                uploadDate = data.optString("uploadDate"),
                relatedFileIds = data.optJSONArray("relatedFileIds")?.toString() ?: "[]",
                sha256 = data.optString("sha256"),
                isProcessed = data.optBoolean("isProcessed", false)
            )
            formDao.insert(entity)
            android.util.Log.d("DeviceDBService", "Form saved successfully: $formId")
        } catch (e: Exception) {
            android.util.Log.e("DeviceDBService", "Error saving form: ${e.message}", e)
            throw e
        }
    }

    suspend fun getForm(formId: String): JSONObject? {
        val entity = formDao.getById(formId) ?: return null
        return JSONObject().apply {
            put("id", entity.id)
            put("name", entity.name)
            put("description", entity.description)
            put("imageUris", Json.decodeFromString<List<String>>(entity.imageUris))
            put("formFields", Json.decodeFromString<List<FormField>>(entity.formFields))
            put("extractedInfo", JSONObject(entity.extractedInfo))
            put("tags", Json.decodeFromString<List<String>>(entity.tags))
            put("uploadDate", entity.uploadDate)
            put("relatedFileIds", Json.decodeFromString<List<String>>(entity.relatedFileIds))
            put("sha256", entity.sha256)
            put("isProcessed", entity.isProcessed)
        }
    }

    suspend fun updateForm(formId: String, updates: JSONObject) {
        val entity = formDao.getById(formId) ?: return
        val updated = entity.copy(
            name = updates.optString("name", entity.name),
            description = updates.optString("description", entity.description),
            imageUris = updates.optJSONArray("imageUris")?.toString() ?: entity.imageUris,
            formFields = updates.optJSONArray("formFields")?.toString() ?: entity.formFields,
            extractedInfo = updates.optJSONObject("extractedInfo")?.toString() ?: entity.extractedInfo,
            tags = updates.optJSONArray("tags")?.toString() ?: entity.tags,
            uploadDate = updates.optString("uploadDate", entity.uploadDate),
            relatedFileIds = updates.optJSONArray("relatedFileIds")?.toString() ?: entity.relatedFileIds,
            sha256 = updates.optString("sha256", entity.sha256),
            isProcessed = updates.optBoolean("isProcessed", entity.isProcessed)
        )
        formDao.update(updated)
    }

    suspend fun exportForms(formIds: List<String>): List<JSONObject> {
        return formDao.getByIds(formIds).map { entity ->
            JSONObject().apply {
                put("id", entity.id)
                put("name", entity.name)
                put("description", entity.description)
                put("imageUris", Json.decodeFromString<List<String>>(entity.imageUris))
                put("formFields", Json.decodeFromString<List<FormField>>(entity.formFields))
                put("extractedInfo", JSONObject(entity.extractedInfo))
                put("tags", Json.decodeFromString<List<String>>(entity.tags))
                put("uploadDate", entity.uploadDate)
                put("relatedFileIds", Json.decodeFromString<List<String>>(entity.relatedFileIds))
                put("sha256", entity.sha256)
                put("isProcessed", entity.isProcessed)
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
                put("imageUris", Json.decodeFromString<List<String>>(entity.imageUris))
                put("formFields", Json.decodeFromString<List<FormField>>(entity.formFields))
                put("extractedInfo", JSONObject(entity.extractedInfo))
                put("tags", Json.decodeFromString<List<String>>(entity.tags))
                put("uploadDate", entity.uploadDate)
                put("relatedFileIds", Json.decodeFromString<List<String>>(entity.relatedFileIds))
                put("sha256", entity.sha256)
                put("isProcessed", entity.isProcessed)
            }
        }
    }

    // match query with related docs / forms / textual info
    suspend fun searchByQuery(query: String): List<JSONObject> {
        val docResults = documentDao.searchByQuery(query)
        val formResults = formDao.searchByQuery(query)

        val docJsons = docResults.map { entity ->
            android.util.Log.d("SERVICE_DEBUG", "Mapping a DOC with ID: ${entity.id}")
            JSONObject().apply {
                put("type", "document")
                put("id", entity.id)
                put("name", entity.name)
                put("description", entity.description)
                put("imageUris", Json.decodeFromString<List<String>>(entity.imageUris))
                put("extractedInfo", JSONObject(entity.extractedInfo))
                put("tags", entity.tags)
                put("uploadDate", entity.uploadDate)
                put("relatedFileIds", Json.decodeFromString<List<String>>(entity.relatedFileIds))
                put("sha256", entity.sha256)
                put("isProcessed", entity.isProcessed)
            }
        }
        val formJsons = formResults.map { entity ->
            android.util.Log.d("SERVICE_DEBUG", "Mapping a FORM with ID: ${entity.id}")
            JSONObject().apply {
                put("type", "form")
                put("id", entity.id)
                put("name", entity.name)
                put("description", entity.description)
                put("imageUris", Json.decodeFromString<List<String>>(entity.imageUris))
                put("formFields", Json.decodeFromString<List<FormField>>(entity.formFields))
                put("extractedInfo", JSONObject(entity.extractedInfo))
                put("tags", entity.tags)
                put("uploadDate", entity.uploadDate)
                put("relatedFileIds", Json.decodeFromString<List<String>>(entity.relatedFileIds))
                put("sha256", entity.sha256)
                put("isProcessed", entity.isProcessed)
            }
        }
        return docJsons + formJsons
    }

    // fetch text info by usage frequency (stub, needs actual logic)
    suspend fun getFrequentTextInfo(): List<JSONObject> {
        // This would require a separate table or usage tracking, so here we return empty for now
        return emptyList()
    }
} 