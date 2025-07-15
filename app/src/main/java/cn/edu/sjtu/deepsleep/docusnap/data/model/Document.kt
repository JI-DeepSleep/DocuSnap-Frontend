data class Document(
    val id: String,
    val title: String,
    val tags: List<String>,
    val description: String,
    val kv: Map<String, String>,
    val related: List<RelatedResource>,
    val sha256: String,
    val isProcessed: Boolean = false
)

data class RelatedResource(val type: String, val resource_id: String)