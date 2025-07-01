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

data class SearchResult(
    val documents: List<Document> = emptyList(),
    val forms: List<Form> = emptyList(),
    val textualInfo: List<String> = emptyList()
)

data class NavigationAction(
    val route: String,
    val data: Any? = null
) 