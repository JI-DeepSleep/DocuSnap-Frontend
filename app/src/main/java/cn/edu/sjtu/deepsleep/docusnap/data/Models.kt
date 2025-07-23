package cn.edu.sjtu.deepsleep.docusnap.data

import kotlinx.serialization.Serializable

// UID structure for unique identification
@Serializable
data class UID(
    val fileType: FileType,
    val id: String
) {
    override fun toString(): String = "${fileType.name}_$id"
}

@Serializable
enum class FileType {
    DOCUMENT,
    FORM
}

@Serializable
data class Document(
    val id: String,
    val name: String,
    val description: String,
    val imageBase64s: List<String> = emptyList(),
    val extractedInfo: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList(),
    val uploadDate: String = "2024-01-15",
    val relatedFileIds: List<String> = emptyList(),
    val sha256: String? = null,
    val isProcessed: Boolean = false,
    val jobId: Long? = null
)

@Serializable
data class Form(
    val id: String,
    val name: String,
    val description: String,
    val imageBase64s: List<String> = emptyList(),
    val formFields: List<FormField> = emptyList(),
    val extractedInfo: Map<String, String> = emptyMap(), // Added extractedInfo to Form
    val tags: List<String> = emptyList(), // Added tags to Form
    val uploadDate: String = "2024-01-15",
    val relatedFileIds: List<String> = emptyList(), // Combined relatedDocumentIds and relatedFormIds
    val sha256: String? = null,
    val isProcessed: Boolean = false,
    val jobId: Long? = null
)

@Serializable
data class FormField(
    val name: String,
    val value: String? = null,
    val isRetrieved: Boolean = false,
    val srcFileId: String? = null // Changed from srcDocId to srcFileId to support both docs and forms
)

// Unified search entity that can represent text, document, or form
@Serializable
sealed class SearchEntity {
    @Serializable
    data class TextEntity(
        val text: String,
        val srcFileId: String? = null, // Changed from sourceDocument to srcFileId
        val relevanceScore: Float = 0.0f
    ) : SearchEntity()

    @Serializable
    data class DocumentEntity(
        val document: Document,
        val relevanceScore: Float = 0.0f
    ) : SearchEntity()

    @Serializable
    data class FormEntity(
        val form: Form,
        val relevanceScore: Float = 0.0f
    ) : SearchEntity()
}

@Serializable
data class SearchResult(
    val entities: List<SearchEntity> = emptyList()
)

// Data class for frequently used text info extracted from documents and forms
@Serializable
data class TextInfo(
    val key: String, // The key/label of the information
    val value: String, // The actual value extracted from the document or form
    val srcFileId: String, // Changed from sourceDocumentId to srcFileId to support both docs and forms
    val srcFileType: FileType, // Type of the source file (document or form)
    val usageCount: Int = 0, // How frequently this text is used/searched
    val lastUsed: String = "2024-01-15" // Last time this text was accessed
)