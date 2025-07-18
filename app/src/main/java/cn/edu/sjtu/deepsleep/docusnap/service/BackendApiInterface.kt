package cn.edu.sjtu.deepsleep.docusnap.service

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
        private const val BASE_URL = "https://docusnap.zjyang.dev/api/v1/"
        
        fun create(): BackendApiInterface {
            return retrofit2.Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build()
                .create(BackendApiInterface::class.java)
        }
    }
} 