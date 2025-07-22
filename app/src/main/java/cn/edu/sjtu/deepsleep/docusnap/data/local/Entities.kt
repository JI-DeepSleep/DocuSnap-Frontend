package cn.edu.sjtu.deepsleep.docusnap.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val imageUris: String,
    val extractedInfo: String,
    val tags: String,
    val uploadDate: String,
    val relatedFileIds: String,
    val sha256: String? = null,
    val isProcessed: Boolean = false,
    @ColumnInfo(name = "job_id")
    val jobId: Long? = null
)

@Entity(tableName = "forms")
data class FormEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val imageUris: String,
    val formFields: String,
    val extractedInfo: String,
    val tags: String,
    val uploadDate: String,
    val relatedFileIds: String,
    val sha256: String? = null,
    val isProcessed: Boolean = false,
    @ColumnInfo(name = "job_id")
    val jobId: Long? = null
)