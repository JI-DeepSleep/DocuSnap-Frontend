package cn.edu.sjtu.deepsleep.docusnap.data.model

import Document
import RelatedResource
import androidx.room.TypeConverter
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

    @TypeConverter
    fun fromRelatedResourceList(value: List<RelatedResource>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toRelatedResourceList(value: String): List<RelatedResource> {
        return Json.decodeFromString(value)
    }
}

// Extension functions for model conversion
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