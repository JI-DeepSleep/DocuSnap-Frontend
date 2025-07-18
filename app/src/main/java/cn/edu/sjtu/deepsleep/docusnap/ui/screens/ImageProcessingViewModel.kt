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

// This data class now represents ALL state for the screen.
data class ImageProcessingUiState(
    val isProcessing: Boolean = false,
    val originalImageUris: List<String> = emptyList(),
    val currentImageIndex: Int = 0,
    // This map now stores the URI of the PROCESSED image file.
    val processedImageUris: Map<Int, String> = emptyMap(),
    // This is only for the live preview bitmap, which doesn't need to be saved.
    val livePreviewBitmap: Bitmap? = null,
    val isFilterToolbarVisible: Boolean = false
) {
    // A helper property to get the URI that should be displayed now.
    // It prioritizes the processed URI if it exists for the current index.
    val currentDisplayUri: String?
        get() = processedImageUris[currentImageIndex] ?: originalImageUris.getOrNull(currentImageIndex)
}

class ImageProcessingViewModel(
    private val context: Context,
    private val imageProcService: ImageProcService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageProcessingUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Call this ONCE when the screen is first created to set the initial images.
     */
    fun setInitialPhotos(photoUris: String?) {
        val uris = photoUris?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        _uiState.update { it.copy(originalImageUris = uris) }
    }
    /**
     * Applies the posterization filter to the given image URI.
     */
    fun applyBinarization(imageUri: String?) {
        // If the URI is null, do nothing.
        if (imageUri == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, livePreviewBitmap = null) }
            withContext(Dispatchers.IO) {
                val inputBitmap = uriToBitmap(imageUri) ?: return@withContext
                // You mentioned you are using 4 levels, so I'll use 4. Change to 8 if you wish.
                val outputBitmap = imageProcService.applyThresholdFilter(inputBitmap, 4)
                val newUri = saveBitmapToCache(outputBitmap)

                newUri?.let { savedUri ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            livePreviewBitmap = outputBitmap,
                            // This is the key fix: update the map with the new URI.
                            processedImageUris = it.processedImageUris + (it.currentImageIndex to savedUri.toString())
                        )
                    }
                } ?: _uiState.update { it.copy(isProcessing = false) } // Handle save failure
            }
        }
    }
    /**
     * Resets the CURRENT image to its original state.
     */
    fun resetToOriginal() {
        _uiState.update {
            it.copy(
                // Remove the live preview.
                livePreviewBitmap = null,
                // Remove the processed URI for the current index from the map.
                processedImageUris = it.processedImageUris - it.currentImageIndex
            )
        }
    }

    /**
     * Navigates to the next image.
     */
    fun goToNextImage() {
        val currentState = _uiState.value
        if (currentState.currentImageIndex < currentState.originalImageUris.size - 1) {
            _uiState.update {
                it.copy(
                    currentImageIndex = it.currentImageIndex + 1,
                    livePreviewBitmap = null // Clear preview when switching images.
                )
            }
        }
    }

    /**
     * Navigates to the previous image.
     */
    fun goToPreviousImage() {
        val currentState = _uiState.value
        if (currentState.currentImageIndex > 0) {
            _uiState.update {
                it.copy(
                    currentImageIndex = it.currentImageIndex - 1,
                    livePreviewBitmap = null // Clear preview when switching images.
                )
            }
        }
    }

    // --- Private Helper Functions (uriToBitmap, saveBitmapToCache) remain the same ---
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
    fun toggleFilterToolbar() {
        _uiState.update {
            it.copy(isFilterToolbarVisible = !it.isFilterToolbarVisible)
        }
    }
}


// The Factory class remains exactly the same as before.
class ImageProcessingViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImageProcessingViewModel::class.java)) {
            val service = AppModule.provideImageProcService(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return ImageProcessingViewModel(context.applicationContext, service) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}