package cn.edu.sjtu.deepsleep.docusnap.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clientId: String,
    val type: String, // "doc", "form", or "fill"
    val sha256: String,
    val hasContent: Boolean,
    val content: String?, // base64(AES(actual_json_string))
    val aesKey: String?, // RSA(real_aes_key)
    val status: String = "pending", // "pending", "processing", "completed", "error"
    val result: String? = null, // base64(AES(actual json string)) from backend
    val errorDetail: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) 