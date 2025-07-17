
package cn.edu.sjtu.deepsleep.docusnap.data.repository

import cn.edu.sjtu.deepsleep.docusnap.data.Document
import cn.edu.sjtu.deepsleep.docusnap.data.Form
import cn.edu.sjtu.deepsleep.docusnap.data.SearchEntity
import cn.edu.sjtu.deepsleep.docusnap.data.TextInfo
import cn.edu.sjtu.deepsleep.docusnap.service.DeviceDBService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DocumentRepository(
    private val deviceDBService: DeviceDBService
) {
    // Document operations
    suspend fun getAllDocuments(): List<Document> {
        val jsonList = deviceDBService.getDocumentGallery()
        return jsonList.mapNotNull { json ->
            try {
                Document(
                    id = json.getString("id"),
                    name = json.getString("title"),
                    description = json.getString("description"),
                    imageUris = emptyList(), // TODO: Add imageUris to database
                    extractedInfo = json.getJSONObject("kv").toMap(),
                    tags = Json.decodeFromString(json.getString("tags")),
                    uploadDate = "2024-01-15", // TODO: Add uploadDate to database
                    relatedFileIds = emptyList() // TODO: Add relatedFileIds to database
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getDocument(id: String): Document? {
        val json = deviceDBService.getDocument(id) ?: return null
        return try {
            Document(
                id = json.getString("id"),
                name = json.getString("title"),
                description = json.getString("description"),
                imageUris = emptyList(), // TODO: Add imageUris to database
                extractedInfo = json.getJSONObject("kv").toMap(),
                tags = Json.decodeFromString(json.getString("tags")),
                uploadDate = "2024-01-15", // TODO: Add uploadDate to database
                relatedFileIds = emptyList() // TODO: Add relatedFileIds to database
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveDocument(document: Document) {
        val json = JSONObject().apply {
            put("title", document.name)
            put("description", document.description)
            put("tags", Json.encodeToString(document.tags))
            put("kv", JSONObject(document.extractedInfo))
            put("related", org.json.JSONArray())              // Empty JSON array
            put("sha256",  "")              // Avoid null ambiguity
            put("isProcessed", document.extractedInfo.isNotEmpty())
        }
        deviceDBService.saveDocument(document.id, json)
    }

    suspend fun updateDocument(document: Document) {
        val json = JSONObject().apply {
            put("title", document.name)
            put("description", document.description)
            put("tags", Json.encodeToString(document.tags))  // Assuming tags is List<String>
            put("kv", JSONObject(document.extractedInfo))     // Assuming extractedInfo is JSON string
            put("related", org.json.JSONArray())              // Empty JSON array
            put("sha256",  "")              // Avoid null ambiguity
            put("isProcessed", document.extractedInfo.isNotEmpty())
        }
        deviceDBService.updateDocument(document.id, json)
    }

    suspend fun deleteDocuments(documentIds: List<String>) {
        deviceDBService.deleteDocuments(documentIds)
    }

    // Form operations
    suspend fun getAllForms(): List<Form> {
        val jsonList = deviceDBService.getFormGallery()
        return jsonList.mapNotNull { json ->
            try {
                Form(
                    id = json.getString("id"),
                    name = json.getString("title"),
                    description = json.getString("description"),
                    imageUris = emptyList(), // TODO: Add imageUris to database
                    formFields = emptyList(), // TODO: Add formFields to database
                    extractedInfo = json.getJSONObject("kv").toMap(),
                    tags = Json.decodeFromString(json.getString("tags")),
                    uploadDate = "2024-01-15", // TODO: Add uploadDate to database
                    relatedFileIds = emptyList() // TODO: Add relatedFileIds to database
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getForm(id: String): Form? {
        val json = deviceDBService.getForm(id) ?: return null
        return try {
            Form(
                id = json.getString("id"),
                name = json.getString("title"),
                description = json.getString("description"),
                imageUris = emptyList(), // TODO: Add imageUris to database
                formFields = emptyList(), // TODO: Add formFields to database
                extractedInfo = json.getJSONObject("kv").toMap(),
                tags = Json.decodeFromString(json.getString("tags")),
                uploadDate = "2024-01-15", // TODO: Add uploadDate to database
                relatedFileIds = emptyList() // TODO: Add relatedFileIds to database
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveForm(form: Form) {
        val json = JSONObject().apply {
            put("title", form.name)
            put("description", form.description)
            put("tags", Json.encodeToString(form.tags))
            put("kv", JSONObject(form.extractedInfo))
            put("fields", Json.encodeToString(form.formFields))
            put("related", org.json.JSONArray())              // Empty JSON array
            put("sha256", "")              // Avoid null ambiguity
            put("isProcessed", form.extractedInfo.isNotEmpty())
        }
        deviceDBService.saveForm(form.id, json)
    }

    suspend fun updateForm(form: Form) {
        val json = JSONObject().apply {
            put("title", form.name)
            put("description", form.description)
            put("tags", Json.encodeToString(form.tags))
            put("kv", JSONObject(form.extractedInfo))
            put("fields", Json.encodeToString(form.formFields))
            put("related", org.json.JSONArray())              // Empty JSON array
            put("sha256",  "")              // Avoid null ambiguity
            put("isProcessed", form.extractedInfo.isNotEmpty())
        }
        deviceDBService.updateForm(form.id, json)
    }

    suspend fun deleteForms(formIds: List<String>) {
        deviceDBService.deleteForms(formIds)
    }

    // Search operations
    suspend fun searchByQuery(query: String): List<SearchEntity> {
        val jsonList = deviceDBService.searchByQuery(query)
        return jsonList.mapNotNull { json ->
            try {
                // For now, return DocumentEntity - you can enhance this to distinguish between docs and forms
                val document = Document(
                    id = json.getString("id"),
                    name = json.getString("title"),
                    description = json.getString("description"),
                    imageUris = emptyList(),
                    extractedInfo = json.getJSONObject("kv").toMap(),
                    tags = Json.decodeFromString(json.getString("tags")),
                    uploadDate = "2024-01-15",
                    relatedFileIds = emptyList()
                )
                SearchEntity.DocumentEntity(document, 10.0F)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getFrequentTextInfo(): List<TextInfo> {
        val jsonList = deviceDBService.getFrequentTextInfo()
        return jsonList.mapNotNull { json ->
            try {
                TextInfo(
                    key = json.getString("key"),
                    value = json.getString("value"),
                    category = json.getString("category"),
                    srcFileId = json.getString("srcFileId"),
                    usageCount = json.getInt("usageCount"),
                    lastUsed = json.getString("lastUsed")
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    // Helper function to convert JSONObject to Map
    private fun JSONObject.toMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (key in keys()) {
            map[key] = getString(key)
        }
        return map
    }
}