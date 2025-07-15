package cn.edu.sjtu.deepsleep.docusnap.service

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import cn.edu.sjtu.deepsleep.docusnap.data.SettingsManager
import kotlinx.coroutines.flow.first
import okhttp3.*
import java.io.ByteArrayOutputStream


// Manager of all interactions with backend server

class BackendApiService(private val context: Context) {
    private val settingsManager = SettingsManager(context)
    private val client = OkHttpClient()
    
    // Get the current backend URL from settings
    private suspend fun getBackendUrl(): String {
        return settingsManager.settings.first().backendUrl
    }
    
    // Convert Bitmap to Base64 string
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    
    // Document Handler
    // TODO: invoke backend process document API
    suspend fun processDocument(enhancedImage: Bitmap) {
        // should change bit map to a list of bitmap or some other ways to support multi images
        // upon upload, compress quality to 20
    }
    
    // Form Handler
    // TODO: invoke backend process form API
    suspend fun processForm(enhancedImage: Bitmap, formType: String) {
        // should change bit map to a list of bitmap or some other ways to support multi images
        // upon upload, compress quality to 20
    }

    // TODO: invoke backend fill form API
    suspend fun fillForm(formId: String) {
    }
} 