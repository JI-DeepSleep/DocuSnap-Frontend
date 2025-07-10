package cn.edu.sjtu.deepsleep.docusnap.data

import androidx.compose.ui.graphics.vector.ImageVector

data class Document(
    val id: String,
    val name: String,
    val type: DocumentType,
    val imageUri: String? = null,
    val extractedInfo: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList(),
    val uploadDate: String = "2024-01-15"
)

data class Form(
    val id: String,
    val name: String,
    val imageUri: String? = null,
    val formFields: List<FormField> = emptyList(),
    val extractedInfo: Map<String, String> = emptyMap(),
    val uploadDate: String = "2024-01-15"
)

data class FormField(
    val name: String,
    val value: String? = null,
    val isRetrieved: Boolean = false
)

enum class DocumentType {
    RECEIPT, INVOICE, CONTRACT, ID_CARD, MEDICAL_RECORD, OTHER
}

// Unified search entity that can represent text, document, or form
sealed class SearchEntity {
    data class TextEntity(
        val id: String,
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

// Legacy SearchResult for backward compatibility
data class LegacySearchResult(
    val documents: List<Document> = emptyList(),
    val forms: List<Form> = emptyList(),
    val textualInfo: List<String> = emptyList()
)

data class NavigationAction(
    val route: String,
    val data: Any? = null
) 