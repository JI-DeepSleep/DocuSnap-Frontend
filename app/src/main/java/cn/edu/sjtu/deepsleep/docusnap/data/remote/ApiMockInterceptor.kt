import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class ApiMockInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val path = chain.request().url.encodedPath
        val mockJson = when (path) {
            "/api/process" -> """
            {
                "status": "completed",
                "result": {
                  "title": "Lease Agreement",
                  "tags": ["legal", "contract"],
                  "description": "Standard residential lease agreement for 12 months",
                  "kv": {
                    "landlord": "Jane Smith",
                    "tenant": "John Doe",
                    "term": "12 months"
                  },
                  "related": [
                    {"type": "form", "resource_id": "uuid-123"}
                  ]
                }
            }
            """
            else -> """{"status": "error", "error_detail": "Unknown endpoint"}"""
        }
        return Response.Builder()
            .code(200)
            .message(mockJson)
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .body(mockJson.toByteArray().toResponseBody("application/json".toMediaTypeOrNull()))
            .addHeader("content-type", "application/json")
            .build()
    }
}