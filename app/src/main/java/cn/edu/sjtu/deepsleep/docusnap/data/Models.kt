package cn.edu.sjtu.deepsleep.docusnap.data

data class Document(
    val id: String,
    val name: String,
    val imageUris: List<String> = emptyList(), // Changed from imageUri to imageUris
    val extractedInfo: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList(),
    val uploadDate: String = "2024-01-15",
    val relatedDocumentIds: List<String> = emptyList(), // IDs of related documents
    val relatedFormIds: List<String> = emptyList() // IDs of related forms
)

data class Form(
    val id: String,
    val name: String,
    val imageUris: List<String> = emptyList(), // Changed from imageUri to imageUris
    val formFields: List<FormField> = emptyList(),
    val extractedInfo: Map<String, String> = emptyMap(),
    val uploadDate: String = "2024-01-15",
    val relatedDocumentIds: List<String> = emptyList(), // IDs of related documents
    val relatedFormIds: List<String> = emptyList() // IDs of related forms
)

data class FormField(
    val name: String,
    val value: String? = null,
    val isRetrieved: Boolean = false,
    val srcDocId: String? = null // Source document ID for this field
)

// Unified search entity that can represent text, document, or form
sealed class SearchEntity {
    data class TextEntity(
        val text: String,
        val sourceDocument: String? = null,
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