package cn.edu.sjtu.deepsleep.docusnap.util

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class JsonUtil {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Generic serialization
    fun <T> toJson(obj: T, clazz: Class<T>): String {
        val adapter = moshi.adapter(clazz)
        return adapter.toJson(obj)
    }

    // Generic deserialization
    fun <T> fromJson(json: String, clazz: Class<T>): T? {
        val adapter = moshi.adapter(clazz)
        return adapter.fromJson(json)
    }
}