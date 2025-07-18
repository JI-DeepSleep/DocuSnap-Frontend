package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.edu.sjtu.deepsleep.docusnap.di.AppModule
import cn.edu.sjtu.deepsleep.docusnap.service.ImageProcService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// This data class represents everything the UI needs to know for now.
data class ImageProcessingUiState(
    val isProcessing: Boolean = false,
    val processedImageForPreview: Bitmap? = null,
)

class ImageProcessingViewModel(
    private val context: Context, // We need context for file operations.
    private val imageProcService: ImageProcService
) : ViewModel() {

    // Private mutable state for the ViewModel to modify.
    private val _uiState = MutableStateFlow(ImageProcessingUiState())
    // Public read-only state for the UI to observe.
    val uiState = _uiState.asStateFlow()

    // We will store the final Uri internally in the ViewModel.
    var finalProcessedUri: Uri? = null
        private set

    /**
     * The function the UI will call to apply the filter.
     */
    fun applyBinarization(imageUri: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processedImageForPreview = null) }

            // Perform all logic in background threads.
            withContext(Dispatchers.IO) {
                // 1. Decode bitmap with downsampling for performance.
                val inputBitmap = uriToBitmap(imageUri) ?: run {
                    _uiState.update { it.copy(isProcessing = false) }
                    return@withContext
                }

                // 2. Call the service to perform the binarization.
                val outputBitmap = imageProcService.applyThresholdFilter(inputBitmap,4)

                // 3. Save the processed bitmap and get its new URI.
                finalProcessedUri = saveBitmapToCache(outputBitmap)

                // 4. Update the UI with the bitmap for preview.
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processedImageForPreview = outputBitmap
                    )
                }
            }
        }
    }

    /**
     * Decodes a bitmap from a URI string, with downsampling.
     */
    private fun uriToBitmap(uriString: String?): Bitmap? {
        if (uriString == null) return null
        return try {
            val imageUri = Uri.parse(uriString)
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            val listener = ImageDecoder.OnHeaderDecodedListener { decoder, info, _ ->
                val targetSize = 1080
                val width = info.size.width
                val height = info.size.height
                if (width > targetSize || height > targetSize) {
                    val scale = if (width > height) targetSize.toFloat() / width else targetSize.toFloat() / height
                    decoder.setTargetSize((width * scale).toInt(), (height * scale).toInt())
                }
            }
            ImageDecoder.decodeBitmap(source, listener).copy(Bitmap.Config.ARGB_8888, true)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves a bitmap to the app's cache directory and returns its content URI.
     */
    private fun saveBitmapToCache(bitmap: Bitmap): Uri? {
        return try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "processed_image_${System.currentTimeMillis()}.jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            stream.close()
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}


// The factory now also needs to provide the context to the ViewModel.
class ImageProcessingViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImageProcessingViewModel::class.java)) {
            val service = AppModule.provideImageProcService(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            // Provide both context and service to the ViewModel.
            return ImageProcessingViewModel(context.applicationContext, service) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}