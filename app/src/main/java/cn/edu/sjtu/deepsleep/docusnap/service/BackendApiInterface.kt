package cn.edu.sjtu.deepsleep.docusnap.service

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class ProcessRequest(
    val client_id: String,
    val type: String,
    val SHA256: String,
    val has_content: Boolean,
    val content: String? = null,
    val aes_key: String? = null
)

data class ProcessResponse(
    val status: String,
    val error_detail: String? = null,
    val result: String? = null
)

data class StatusResponse(
    val status: String
)

interface BackendApiInterface {
    @POST("process")
    suspend fun processDocument(@Body request: ProcessRequest): ProcessResponse
    
    @GET("check_status")
    suspend fun checkStatus(): StatusResponse
}

class BackendApiClient {
    companion object {
        fun create(baseUrl: String): BackendApiInterface {
            // Ensure URL ends with trailing slash for Retrofit compatibility
            val adjustedUrl = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"

            return Retrofit.Builder()
                .baseUrl(adjustedUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BackendApiInterface::class.java)
        }
    }
} 