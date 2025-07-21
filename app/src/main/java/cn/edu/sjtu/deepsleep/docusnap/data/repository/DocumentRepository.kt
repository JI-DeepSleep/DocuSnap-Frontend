

package cn.edu.sjtu.deepsleep.docusnap.data.repository

import cn.edu.sjtu.deepsleep.docusnap.data.Document
import cn.edu.sjtu.deepsleep.docusnap.data.FileType
import cn.edu.sjtu.deepsleep.docusnap.data.Form
import cn.edu.sjtu.deepsleep.docusnap.data.FormField
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
        android.util.Log.d("DocumentRepository", "Getting all documents...")
        try {
            val jsonList = deviceDBService.getDocumentGallery()
            android.util.Log.d("DocumentRepository", "Got ${jsonList.size} documents from service")
            val documents = jsonList.mapNotNull { json ->
                try {
                    Document(
                        id = json.getString("id"),
                        name = json.getString("name"),
                        description = json.getString("description"),
                        imageUris = Json.decodeFromString(json.getString("imageUris")),
                        extractedInfo = json.getJSONObject("extractedInfo").toMap(),
                        tags = Json.decodeFromString(json.getString("tags")),
                        uploadDate = json.getString("uploadDate"),
                        relatedFileIds = Json.decodeFromString(json.getString("relatedFileIds"))
                    )
                } catch (e: Exception) {
                    android.util.Log.e("DocumentRepository", "Error converting JSON to Document: ${e.message}", e)
                    null
                }
            }
            android.util.Log.d("DocumentRepository", "Converted ${documents.size} documents successfully")
            return documents
        } catch (e: Exception) {
            android.util.Log.e("DocumentRepository", "Error getting all documents: ${e.message}", e)
            throw e
        }
    }

    suspend fun getDocument(id: String): Document? {
        val json = deviceDBService.getDocument(id) ?: return null
        return try {
            Document(
                id = json.getString("id"),
                name = json.getString("name"),
                description = json.getString("description"),
                imageUris = Json.decodeFromString(json.getString("imageUris")),
                extractedInfo = json.getJSONObject("extractedInfo").toMap(),
                tags = Json.decodeFromString(json.getString("tags")),
                uploadDate = json.getString("uploadDate"),
                relatedFileIds = Json.decodeFromString(json.getString("relatedFileIds"))
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveDocument(document: Document) {
        val json = JSONObject().apply {
            put("name", document.name)
            put("description", document.description)
            put("imageUris", Json.encodeToString(document.imageUris))
            put("extractedInfo", JSONObject(document.extractedInfo))
            put("tags", Json.encodeToString(document.tags))
            put("uploadDate", document.uploadDate)
            put("relatedFileIds", Json.encodeToString(document.relatedFileIds))
            put("sha256",  "")
            put("isProcessed", document.extractedInfo.isNotEmpty())
        }
        deviceDBService.saveDocument(document.id, json)
    }

    suspend fun updateDocument(document: Document) {
        val json = JSONObject().apply {
            put("name", document.name)
            put("description", document.description)
            put("imageUris", Json.encodeToString(document.imageUris))
            put("extractedInfo", JSONObject(document.extractedInfo))
            put("tags", Json.encodeToString(document.tags))
            put("uploadDate", document.uploadDate)
            put("relatedFileIds", Json.encodeToString(document.relatedFileIds))
            put("sha256",  "")
            put("isProcessed", document.extractedInfo.isNotEmpty())
        }
        deviceDBService.updateDocument(document.id, json)
    }

    suspend fun deleteDocuments(documentIds: List<String>) {
        deviceDBService.deleteDocuments(documentIds)
    }

    suspend fun exportDocuments(documentIds: List<String>) {
        deviceDBService.exportDocuments(documentIds)
    }

    suspend fun exportForms(formIds: List<String>) {
        deviceDBService.exportForms(formIds)
    }

    // Form operations
    suspend fun getAllForms(): List<Form> {
        val jsonList = deviceDBService.getFormGallery()
        return jsonList.mapNotNull { json ->
            try {
                Form(
                    id = json.getString("id"),
                    name = json.getString("name"),
                    description = json.getString("description"),
                    imageUris = Json.decodeFromString(json.getString("imageUris")),
                    formFields = Json.decodeFromString(json.getString("formFields")),
                    extractedInfo = json.getJSONObject("extractedInfo").toMap(),
                    tags = Json.decodeFromString(json.getString("tags")),
                    uploadDate = json.getString("uploadDate"),
                    relatedFileIds = Json.decodeFromString(json.getString("relatedFileIds"))
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
                name = json.getString("name"),
                description = json.getString("description"),
                imageUris = Json.decodeFromString(json.getString("imageUris")),
                formFields = Json.decodeFromString(json.getString("formFields")),
                extractedInfo = json.getJSONObject("extractedInfo").toMap(),
                tags = Json.decodeFromString(json.getString("tags")),
                uploadDate = json.getString("uploadDate"),
                relatedFileIds = Json.decodeFromString(json.getString("relatedFileIds"))
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveForm(form: Form) {
        val json = JSONObject().apply {
            put("name", form.name)
            put("description", form.description)
            put("imageUris", Json.encodeToString(form.imageUris))
            put("formFields", Json.encodeToString(form.formFields))
            put("extractedInfo", JSONObject(form.extractedInfo))
            put("tags", Json.encodeToString(form.tags))
            put("uploadDate", form.uploadDate)
            put("relatedFileIds", Json.encodeToString(form.relatedFileIds))
            put("sha256", "")
            put("isProcessed", form.extractedInfo.isNotEmpty())
        }
        deviceDBService.saveForm(form.id, json)
    }

    suspend fun updateForm(form: Form) {
        val json = JSONObject().apply {
            put("name", form.name)
            put("description", form.description)
            put("imageUris", Json.encodeToString(form.imageUris))
            put("formFields", Json.encodeToString(form.formFields))
            put("extractedInfo", JSONObject(form.extractedInfo))
            put("tags", Json.encodeToString(form.tags))
            put("uploadDate", form.uploadDate)
            put("relatedFileIds", Json.encodeToString(form.relatedFileIds))
            put("sha256", "")
            put("isProcessed", form.extractedInfo.isNotEmpty())
        }
        deviceDBService.updateForm(form.id, json)
    }

    suspend fun deleteForms(formIds: List<String>) {
        deviceDBService.deleteForms(formIds)
    }

    // Search operations
    suspend fun searchByQuery(query: String): List<SearchEntity> {
        // Return early if query is blank to avoid unnecessary work
        if (query.isBlank()) {
            return emptyList()
        }

        val jsonList = deviceDBService.searchByQuery(query)

        // v-- PROBE 2: Check what the Repository received --v
        val docCount = jsonList.count { it.optString("type") == "document" }
        val formCount = jsonList.count { it.optString("type") == "form" }

        return jsonList.mapNotNull { json ->
            try {
                // CORE CHANGE: Instead of assuming everything is a Document,
                // we now check the 'type' field added in the service layer.
                when (json.optString("type")) {
                    "document" -> {
                        // This is the corrected parsing logic for a Document
                        val document = Document(
                            id = json.getString("id"),
                            name = json.getString("name"),
                            description = json.getString("description"),
                            imageUris = Json.decodeFromString(json.getString("imageUris")),
                            extractedInfo = JSONObject(json.getString("extractedInfo")).toMap(),
                            tags = Json.decodeFromString(json.getString("tags")),
                            uploadDate = json.getString("uploadDate"),
                            relatedFileIds = Json.decodeFromString(json.getString("relatedFileIds"))
                        )
                        SearchEntity.DocumentEntity(document, 10.0F)
                    }
                    "form" -> {
                        // This is the new parsing logic for a Form
                        val form = Form(
                            id = json.getString("id"),
                            name = json.getString("name"),
                            description = json.getString("description"),
                            imageUris = Json.decodeFromString(json.getString("imageUris")),
                            formFields = Json.decodeFromString(json.getString("formFields")),
                            extractedInfo = JSONObject(json.getString("extractedInfo")).toMap(),
                            tags = Json.decodeFromString(json.getString("tags")),
                            uploadDate = json.getString("uploadDate"),
                            relatedFileIds = Json.decodeFromString(json.getString("relatedFileIds"))
                        )
                        SearchEntity.FormEntity(form, 10.0F)
                    }
                    // If type is unknown, ignore this entry
                    else -> null
                }
            } catch (e: Exception) {
                // If any error occurs during parsing, log it and safely skip this item
                android.util.Log.e("DocumentRepository", "Failed to parse search result: $json", e)
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

    suspend fun exportDatabaseToJson(excludeType: FileType, excludeId: String): String {
        val docs = getAllDocuments().filterNot { excludeType == FileType.DOCUMENT && it.id == excludeId }
        val forms = getAllForms().filterNot { excludeType == FileType.FORM && it.id == excludeId }

        val docJsonList = docs.map { doc ->
            mapOf(
                "id" to doc.id,
                "title" to doc.name,
                "tag" to doc.tags,
                "description" to doc.description,
                "kv" to doc.extractedInfo,
                "import date" to doc.uploadDate
            )
        }
        val formJsonList = forms.map { form ->
            mapOf(
                "id" to form.id,
                "title" to form.name,
                "tag" to form.tags,
                "description" to form.description,
                "kv" to form.extractedInfo,
                "fields" to form.formFields,
                "import date" to form.uploadDate
            )
        }
        val result = mapOf(
            "doc" to docJsonList,
            "form" to formJsonList
        )
        return Json.encodeToString(result)
    }
    
    // Development helper: Add test data to database
    suspend fun addTestData() {
        android.util.Log.d("DocumentRepository", "Adding test data...")
        try {
            val timestamp = System.currentTimeMillis()
            val testDocument = Document(
                id = "test-doc-$timestamp",
                name = "Test Document $timestamp",
                description = "A test document for development (created at $timestamp)",
                imageUris = emptyList(),
                extractedInfo = mapOf(
                    "Vendor" to "Test Company",
                    "Date" to "2024-01-15",
                    "Amount" to "$25.00",
                    "Created" to timestamp.toString()
                ),
                tags = listOf("Test", "Development"),
                uploadDate = "2024-01-15",
                relatedFileIds = emptyList()
            )
            
            val testForm = Form(
                id = "test-form-$timestamp",
                name = "Test Form $timestamp",
                description = "A test form for development (created at $timestamp)",
                imageUris = emptyList(),
                formFields = listOf(
                    FormField("Name", "John Doe", true),
                    FormField("Email", "john@example.com", true),
                    FormField("Created", timestamp.toString(), true)
                ),
                extractedInfo = mapOf(
                    "Form Type" to "Test Form",
                    "Status" to "Completed",
                    "Created" to timestamp.toString()
                ),
                tags = listOf("Test", "Form"),
                uploadDate = "2024-01-15",
                relatedFileIds = emptyList()
            )
            
            android.util.Log.d("DocumentRepository", "Saving test document with ID: ${testDocument.id}")
            saveDocument(testDocument)
            android.util.Log.d("DocumentRepository", "Saving test form with ID: ${testForm.id}")
            saveForm(testForm)
            android.util.Log.d("DocumentRepository", "Test data added successfully!")
        } catch (e: Exception) {
            android.util.Log.e("DocumentRepository", "Error adding test data: ${e.message}", e)
            throw e
        }
    }
}