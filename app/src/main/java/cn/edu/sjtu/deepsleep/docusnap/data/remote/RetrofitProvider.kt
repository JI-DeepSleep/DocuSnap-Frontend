import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

val retrofit = Retrofit.Builder()
    .baseUrl("http://localhost/") // Fake base URL
    .client(OkHttpClient.Builder().addInterceptor(ApiMockInterceptor()).build())
    .addConverterFactory(MoshiConverterFactory.create())
    .build()

val apiService: ApiService = retrofit.create(ApiService::class.java)