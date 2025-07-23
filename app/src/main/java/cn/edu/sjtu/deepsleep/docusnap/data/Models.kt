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

// New structure for extracted info with usage tracking
@Serializable
data class ExtractedInfoItem(
    val key: String,
    val value: String,
    val usageCount: Int = 0,
    val lastUsed: String = "2024-01-15"
)

@Serializable
data class Document(
    val id: String,
    val name: String,
    val description: String,
    val imageBase64s: List<String> = emptyList(),
    val extractedInfo: List<ExtractedInfoItem> = emptyList(),
    val tags: List<String> = emptyList(),
    val uploadDate: String = "2024-01-15",
    val relatedFileIds: List<String> = emptyList(),
    val sha256: String? = null,
    val isProcessed: Boolean = false,
    val jobId: Long? = null,
    val usageCount: Int = 0,
    val lastUsed: String = "2024-01-15"
)

@Serializable
data class Form(
    val id: String,
    val name: String,
    val description: String,
    val imageBase64s: List<String> = emptyList(),
    val formFields: List<FormField> = emptyList(),
    val extractedInfo: List<ExtractedInfoItem> = emptyList(),
    val tags: List<String> = emptyList(),
    val uploadDate: String = "2024-01-15",
    val relatedFileIds: List<String> = emptyList(),
    val sha256: String? = null,
    val isProcessed: Boolean = false,
    val jobId: Long? = null,
    val usageCount: Int = 0,
    val lastUsed: String = "2024-01-15"
)

@Serializable
data class FormField(
    val name: String,
    val value: String? = null,
    val isRetrieved: Boolean = false,
    val srcFileId: String? = null
)

// Unified search entity that can represent text, document, or form
@Serializable
sealed class SearchEntity {
    @Serializable
    data class TextEntity(
        val text: String,
        val srcFileId: String? = null,
        val srcFileType: FileType? = null,
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

// Data class for frequently used text info extracted from documents and forms
@Serializable
data class TextInfo(
    val key: String,
    val value: String,
    val srcFileId: String,
    val srcFileType: FileType,
    val usageCount: Int = 0,
    val lastUsed: String = "2024-01-15"
)