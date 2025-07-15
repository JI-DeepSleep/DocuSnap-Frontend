import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun DocumentEntity.toModel(): Document = Document(
    id = id,
    title = title,
    tags = Json.decodeFromString(tags),
    description = description,
    kv = Json.decodeFromString(kv),
    related = Json.decodeFromString(related),
    sha256 = sha256,
    isProcessed = isProcessed
)

fun Document.toEntity(): DocumentEntity = DocumentEntity(
    id = id,
    title = title,
    tags = Json.encodeToString(tags),
    description = description,
    kv = Json.encodeToString(kv),
    related = Json.encodeToString(related),
    sha256 = sha256,
    isProcessed = isProcessed
)