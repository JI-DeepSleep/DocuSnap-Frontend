package cn.edu.sjtu.deepsleep.docusnap.data.model

import androidx.room.TypeConverter
import cn.edu.sjtu.deepsleep.docusnap.data.Document
import cn.edu.sjtu.deepsleep.docusnap.data.Form
import cn.edu.sjtu.deepsleep.docusnap.data.local.DocumentEntity
import cn.edu.sjtu.deepsleep.docusnap.data.FormField
import cn.edu.sjtu.deepsleep.docusnap.data.local.FormEntity
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
    fun fromFormFieldList(value: List<FormField>): String = Json.encodeToString(value)

    @TypeConverter
    fun toFormFieldList(value: String): List<FormField> = Json.decodeFromString(value)
}

// Extension functions for model conversion
fun DocumentEntity.toModel(): Document = Document(
    id = id,
    name = name,
    description = description,
    imageBase64s = Json.decodeFromString(imageBase64s), // Changed from imageUris to imageBase64s
    extractedInfo = Json.decodeFromString(extractedInfo),
    tags = Json.decodeFromString(tags),
    uploadDate = uploadDate,
    relatedFileIds = Json.decodeFromString(relatedFileIds)
)

fun Document.toEntity(): DocumentEntity = DocumentEntity(
    id = id,
    name = name,
    description = description,
    imageBase64s = Json.encodeToString(imageBase64s), // Changed from imageUris to imageBase64s
    extractedInfo = Json.encodeToString(extractedInfo),
    tags = Json.encodeToString(tags),
    uploadDate = uploadDate,
    relatedFileIds = Json.encodeToString(relatedFileIds),
    sha256 = null,
    isProcessed = false
)

fun FormEntity.toModel(): Form = Form(
    id = id,
    name = name,
    description = description,
    imageBase64s = Json.decodeFromString(imageBase64s), // Changed from imageUris to imageBase64s
    formFields = Json.decodeFromString(formFields),
    extractedInfo = Json.decodeFromString(extractedInfo),
    tags = Json.decodeFromString(tags),
    uploadDate = uploadDate,
    relatedFileIds = Json.decodeFromString(relatedFileIds)
)

fun Form.toEntity(): FormEntity = FormEntity(
    id = id,
    name = name,
    description = description,
    imageBase64s = Json.encodeToString(imageBase64s), // Changed from imageUris to imageBase64s
    formFields = Json.encodeToString(formFields),
    extractedInfo = Json.encodeToString(extractedInfo),
    tags = Json.encodeToString(tags),
    uploadDate = uploadDate,
    relatedFileIds = Json.encodeToString(relatedFileIds),
    sha256 = null,
    isProcessed = false
)