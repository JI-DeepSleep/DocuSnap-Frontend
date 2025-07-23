package cn.edu.sjtu.deepsleep.docusnap.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val imageBase64s: String,
    val extractedInfo: String,
    val tags: String,
    val uploadDate: String,
    val relatedFileIds: String,
    val sha256: String? = null,
    val isProcessed: Boolean = false,
    @ColumnInfo(name = "job_id")
    val jobId: Long? = null,
    @ColumnInfo(name = "usage_count")
    val usageCount: Int = 0,
    @ColumnInfo(name = "last_used")
    val lastUsed: String = "2024-01-15"
)

@Entity(tableName = "forms")
data class FormEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val imageBase64s: String,
    val formFields: String,
    val extractedInfo: String,
    val tags: String,
    val uploadDate: String,
    val relatedFileIds: String,
    val sha256: String? = null,
    val isProcessed: Boolean = false,
    @ColumnInfo(name = "job_id")
    val jobId: Long? = null,
    @ColumnInfo(name = "usage_count")
    val usageCount: Int = 0,
    @ColumnInfo(name = "last_used")
    val lastUsed: String = "2024-01-15"
)