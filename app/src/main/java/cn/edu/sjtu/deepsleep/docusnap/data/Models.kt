package cn.edu.sjtu.deepsleep.docusnap.data

// UID structure for unique identification
data class UID(
    val fileType: FileType,
    val id: String
) {
    override fun toString(): String = "${fileType.name}_$id"
}

enum class FileType {
    DOCUMENT,
    FORM
}

data class Document(
    val id: String,
    val name: String,
    val description: String,
    val imageUris: List<String> = emptyList(), // Changed from imageUri to imageUris
    val extractedInfo: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList(),
    val uploadDate: String = "2024-01-15",
    val relatedFileIds: List<String> = emptyList() // Combined relatedDocumentIds and relatedFormIds
)

data class Form(
    val id: String,
    val name: String,
    val description: String,
    val imageUris: List<String> = emptyList(), // Changed from imageUri to imageUris
    val formFields: List<FormField> = emptyList(),
    val extractedInfo: Map<String, String> = emptyMap(), // Added extractedInfo to Form
    val tags: List<String> = emptyList(), // Added tags to Form
    val uploadDate: String = "2024-01-15",
    val relatedFileIds: List<String> = emptyList() // Combined relatedDocumentIds and relatedFormIds
)

data class FormField(
    val name: String,
    val value: String? = null,
    val isRetrieved: Boolean = false,
    val srcFileId: String? = null // Changed from srcDocId to srcFileId to support both docs and forms
)

// Unified search entity that can represent text, document, or form
sealed class SearchEntity {
    data class TextEntity(
        val text: String,
        val srcFileId: String? = null, // Changed from sourceDocument to srcFileId
        val relevanceScore: Float = 0.0f
    ) : SearchEntity()
    
    data class DocumentEntity(
        val document: Document,
        val relevanceScore: Float = 0.0f
    ) : SearchEntity()
    
    data class FormEntity(
        val form: Form,
        val relevanceScore: Float = 0.0f
    ) : SearchEntity()
}

data class SearchResult(
    val entities: List<SearchEntity> = emptyList()
)

// Data class for frequently used text info extracted from documents and forms
data class TextInfo(
    val key: String, // The key/label of the information
    val value: String, // The actual value extracted from the document or form
    val category: String, // e.g., "Recent Expenses", "Important Contacts", "Travel Information"
    val srcFileId: String, // Changed from sourceDocumentId to srcFileId to support both docs and forms
    val usageCount: Int = 0, // How frequently this text is used/searched
    val lastUsed: String = "2024-01-15" // Last time this text was accessed
)