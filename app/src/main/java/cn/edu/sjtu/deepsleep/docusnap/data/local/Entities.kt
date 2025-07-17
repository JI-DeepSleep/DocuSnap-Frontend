package cn.edu.sjtu.deepsleep.docusnap.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val tags: String, // JSON string, or use TypeConverters
    val description: String,
    val kv: String,   // JSON string
    val related: String, // JSON string
    val sha256: String,
    val isProcessed: Boolean = false
)

@Entity(tableName = "forms")
data class FormEntity(
    @PrimaryKey val id: String,
    val title: String,
    val tags: String,
    val description: String,
    val kv: String,
    val fields: String, // JSON string
    val related: String,
    val sha256: String,
    val isProcessed: Boolean = false
)