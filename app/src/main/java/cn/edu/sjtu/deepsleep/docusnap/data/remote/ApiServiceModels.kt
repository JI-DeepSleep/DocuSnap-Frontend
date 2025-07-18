package cn.edu.sjtu.deepsleep.docusnap.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// For /api/process
@JsonClass(generateAdapter = true)
data class ProcessRequest(
    @Json(name = "client_id") val clientId: String,
    val type: String,
    @Json(name = "SHA256") val sha256: String,
    @Json(name = "has_content") val hasContent: Boolean,
    val content: String? = null,
    @Json(name = "aes_key") val aesKey: String? = null
)

@JsonClass(generateAdapter = true)
data class ProcessResponse(
    val status: String,
    @Json(name = "error_detail") val errorDetail: String? = null,
    val result: String? = null
)

// For /api/clear and /api/cache/clear
@JsonClass(generateAdapter = true)
data class ClearRequest(
    @Json(name = "client_id") val clientId: String,
    val type: String? = null,
    @Json(name = "SHA256") val sha256: String? = null
)

@JsonClass(generateAdapter = true)
data class ClearResponse(
    val status: String
)

// For /api/cache/query
@JsonClass(generateAdapter = true)
data class CacheQueryResponse(
    val data: String? = null,
    val error: String? = null // Only present on 404
)

// For /api/cache/store
@JsonClass(generateAdapter = true)
data class CacheStoreRequest(
    @Json(name = "client_id") val clientId: String,
    val type: String,
    @Json(name = "SHA256") val sha256: String,
    val data: String
)

// For /api/ocr/extract
@JsonClass(generateAdapter = true)
data class OcrExtractRequest(
    @Json(name = "image_data") val imageData: String
)

@JsonClass(generateAdapter = true)
data class OcrExtractResponse(
    val text: String
)