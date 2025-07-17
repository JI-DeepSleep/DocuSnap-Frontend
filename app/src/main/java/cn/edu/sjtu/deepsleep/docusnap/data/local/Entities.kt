package cn.edu.sjtu.deepsleep.docusnap.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val name: String, // was title
    val description: String,
    val imageUris: String, // JSON string
    val extractedInfo: String, // JSON string
    val tags: String, // JSON string
    val uploadDate: String,
    val relatedFileIds: String, // JSON string
    val sha256: String? = null,
    val isProcessed: Boolean = false
)

@Entity(tableName = "forms")
data class FormEntity(
    @PrimaryKey val id: String,
    val name: String, // was title
    val description: String,
    val imageUris: String, // JSON string
    val formFields: String, // JSON string
    val extractedInfo: String, // JSON string
    val tags: String, // JSON string
    val uploadDate: String,
    val relatedFileIds: String, // JSON string
    val sha256: String? = null,
    val isProcessed: Boolean = false
)

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey val id: String,
    val clientId: String,
    val type: String, // "doc", "form", "fill"
    val sha256: String,
    val status: String, // "pending", "processing", "completed", "error"
    val createdAt: Long,
    val updatedAt: Long,
    val result: String?, // JSON string of result when completed
    val errorDetail: String?, // Error message if failed
    val excludeType: String?, // FileType to exclude from file_lib
    val excludeId: String? // ID to exclude from file_lib
)