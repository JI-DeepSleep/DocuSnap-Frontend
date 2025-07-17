package cn.edu.sjtu.deepsleep.docusnap.data.model

import androidx.room.TypeConverter
import cn.edu.sjtu.deepsleep.docusnap.data.Document
import cn.edu.sjtu.deepsleep.docusnap.data.local.DocumentEntity
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return Json.decodeFromString(value)
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        return Json.decodeFromString(value)
    }
}

// Extension functions for model conversion
fun DocumentEntity.toModel(): Document = Document(
    id = id,
    name = title, // Map title to name
    description = description,
    imageUris = emptyList(), // Default empty list for now
    extractedInfo = Json.decodeFromString(kv), // Map kv to extractedInfo
    tags = Json.decodeFromString(tags),
    uploadDate = "2024-01-15", // Default date
    relatedFileIds = emptyList() // Default empty list for now
)

fun Document.toEntity(): DocumentEntity = DocumentEntity(
    id = id,
    title = name, // Map name to title
    tags = Json.encodeToString(tags),
    description = description,
    kv = Json.encodeToString(extractedInfo), // Map extractedInfo to kv
    related = "[]", // Default empty JSON array
    sha256 = "", // Default empty string
    isProcessed = false
)