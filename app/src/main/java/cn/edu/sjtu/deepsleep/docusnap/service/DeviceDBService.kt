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
            ).build().also {
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
                title = data.optString("title"),
                tags = data.optJSONArray("tags")?.toString() ?: "[]",
                description = data.optString("description"),
                kv = data.optJSONObject("kv")?.toString() ?: "{}",
                related = data.optJSONArray("related")?.toString() ?: "[]",
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
            put("title", entity.title)
            put("tags", Json.decodeFromString<List<String>>(entity.tags))
            put("description", entity.description)
            put("kv", JSONObject(entity.kv))
            put("related", Json.decodeFromString<List<String>>(entity.related))
            put("sha256", entity.sha256)
            put("isProcessed", entity.isProcessed)
        }
    }

    suspend fun updateDocument(documentId: String, updates: JSONObject) {
        val entity = documentDao.getById(documentId) ?: return
        val updated = entity.copy(
            title = updates.optString("title", entity.title),
            tags = updates.optJSONArray("tags")?.toString() ?: entity.tags,
            description = updates.optString("description", entity.description),
            kv = updates.optJSONObject("kv")?.toString() ?: entity.kv,
            related = updates.optJSONArray("related")?.toString() ?: entity.related,
            sha256 = updates.optString("sha256", entity.sha256),
            isProcessed = updates.optBoolean("isProcessed", entity.isProcessed)
        )
        documentDao.update(updated)
    }

    suspend fun exportDocuments(documentIds: List<String>): List<JSONObject> {
        return documentDao.getByIds(documentIds).map { entity ->
            JSONObject().apply {
                put("id", entity.id)
                put("title", entity.title)
                put("tags", Json.decodeFromString<List<String>>(entity.tags))
                put("description", entity.description)
                put("kv", JSONObject(entity.kv))
                put("related", Json.decodeFromString<List<String>>(entity.related))
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
                    put("title", entity.title)
                    put("tags", Json.decodeFromString<List<String>>(entity.tags))
                    put("description", entity.description)
                    put("kv", JSONObject(entity.kv))
                    put("related", Json.decodeFromString<List<String>>(entity.related))
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
                title = data.optString("title"),
                tags = data.optJSONArray("tags")?.toString() ?: "[]",
                description = data.optString("description"),
                kv = data.optJSONObject("kv")?.toString() ?: "{}",
                fields = data.optJSONArray("fields")?.toString() ?: "[]",
                related = data.optJSONArray("related")?.toString() ?: "[]",
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
            put("title", entity.title)
            put("tags", Json.decodeFromString<List<String>>(entity.tags))
            put("description", entity.description)
            put("kv", JSONObject(entity.kv))
            put("fields", Json.decodeFromString<List<FormField>>(entity.fields))
            put("related", Json.decodeFromString<List<String>>(entity.related))
            put("sha256", entity.sha256)
            put("isProcessed", entity.isProcessed)
        }
    }

    suspend fun updateForm(formId: String, updates: JSONObject) {
        val entity = formDao.getById(formId) ?: return
        val updated = entity.copy(
            title = updates.optString("title", entity.title),
            tags = updates.optJSONArray("tags")?.toString() ?: entity.tags,
            description = updates.optString("description", entity.description),
            kv = updates.optJSONObject("kv")?.toString() ?: entity.kv,
            fields = updates.optJSONArray("fields")?.toString() ?: entity.fields,
            related = updates.optJSONArray("related")?.toString() ?: entity.related,
            sha256 = updates.optString("sha256", entity.sha256),
            isProcessed = updates.optBoolean("isProcessed", entity.isProcessed)
        )
        formDao.update(updated)
    }

    suspend fun exportForms(formIds: List<String>): List<JSONObject> {
        return formDao.getByIds(formIds).map { entity ->
            JSONObject().apply {
                put("id", entity.id)
                put("title", entity.title)
                put("tags", Json.decodeFromString<List<String>>(entity.tags))
                put("description", entity.description)
                put("kv", JSONObject(entity.kv))
                put("fields", Json.decodeFromString<List<FormField>>(entity.fields))
                put("related", Json.decodeFromString<List<String>>(entity.related))
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
                put("title", entity.title)
                put("tags", Json.decodeFromString<List<String>>(entity.tags))
                put("description", entity.description)
                put("kv", JSONObject(entity.kv))
                put("fields", Json.decodeFromString<List<FormField>>(entity.fields))
                put("related", Json.decodeFromString<List<String>>(entity.related))
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
            JSONObject().apply {
                put("id", entity.id)
                put("title", entity.title)
                put("tags", Json.decodeFromString<List<String>>(entity.tags))
                put("description", entity.description)
                put("kv", JSONObject(entity.kv))
                put("related", Json.decodeFromString<List<String>>(entity.related))
                put("sha256", entity.sha256)
                put("isProcessed", entity.isProcessed)
            }
        }
        val formJsons = formResults.map { entity ->
            JSONObject().apply {
                put("id", entity.id)
                put("title", entity.title)
                put("tags", Json.decodeFromString<List<String>>(entity.tags))
                put("description", entity.description)
                put("kv", JSONObject(entity.kv))
                put("fields", Json.decodeFromString<List<FormField>>(entity.fields))
                put("related", Json.decodeFromString<List<String>>(entity.related))
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