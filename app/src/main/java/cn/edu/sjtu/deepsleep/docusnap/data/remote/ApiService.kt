import cn.edu.sjtu.deepsleep.docusnap.data.remote.CacheQueryResponse
import cn.edu.sjtu.deepsleep.docusnap.data.remote.CacheStoreRequest
import cn.edu.sjtu.deepsleep.docusnap.data.remote.ClearRequest
import cn.edu.sjtu.deepsleep.docusnap.data.remote.ClearResponse
import cn.edu.sjtu.deepsleep.docusnap.data.remote.OcrExtractRequest
import cn.edu.sjtu.deepsleep.docusnap.data.remote.OcrExtractResponse
import cn.edu.sjtu.deepsleep.docusnap.data.remote.ProcessRequest
import cn.edu.sjtu.deepsleep.docusnap.data.remote.ProcessResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.*
import retrofit2.Response

interface ApiService {
    // Unified Processing Endpoint
    @POST("/api/process")
    suspend fun processDocument(@Body request: ProcessRequest): ProcessResponse

    // Clears processing results from the system
    @POST("/api/clear")
    suspend fun clearProcessingResult(@Body request: ClearRequest): ClearResponse

    // ----- Cache Server -----

    // Query cache for a processing result (GET with query params)
    @GET("/api/cache/query")
    suspend fun queryCache(
        @Query("client_id") clientId: String,
        @Query("SHA256") sha256: String,
        @Query("type") type: String
    ): Response<CacheQueryResponse> // Use Response<> to handle 404

    // Store a processing result in cache
    @POST("/api/cache/store")
    suspend fun storeCache(@Body request: CacheStoreRequest): Response<Unit> // 201 Created, empty body

    // Clear cache entries
    @POST("/api/cache/clear")
    suspend fun clearCache(@Body request: ClearRequest): ClearResponse

    // ----- OCR Server -----

    // OCR extraction
    @POST("/api/ocr/extract")
    suspend fun extractOcr(@Body request: OcrExtractRequest): OcrExtractResponse
}