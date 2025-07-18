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
import android.util.Log
import kotlinx.coroutines.Job

// --- 1. REDEFINE THE UI STATE ---
// It's simpler now. It mainly holds the "canvas" (editingBitmap).
data class ImageProcessingUiState(
    val isLoading: Boolean = false,
    val originalImageUris: List<String> = emptyList(),
    val currentImageIndex: Int = 0,
    // This is the "canvas". It holds the current state of the image being edited.
    val editingBitmap: Bitmap? = null,
    val isFilterToolbarVisible: Boolean = false
)

class ImageProcessingViewModel(
    private val context: Context,
    private val imageProcService: ImageProcService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageProcessingUiState())
    val uiState = _uiState.asStateFlow()

    // --- 2. Keep track of the original bitmap and the final saved URIs ---
    private var originalBitmap: Bitmap? = null
    private var processedImageUris = mutableMapOf<Int, String>()

    // This function will be called by the UI's 'Done' button.
    suspend fun saveCurrentCanvasToFile(): Uri? {
        val bitmapToSave = _uiState.value.editingBitmap ?: return null
        return saveBitmapToCache(bitmapToSave)
    }

    // --- 3. REWORK THE LOGIC ---
    // Instead of one function per filter, we have one generic "apply" function.

    /**
     * A generic function that applies any service filter to the current "canvas".
     * @param filter The service function to apply, e.g., imageProcService::applyHighContrast
     */
    private fun applyFilter(filter: suspend (Bitmap) -> Bitmap) {
        val currentCanvas = _uiState.value.editingBitmap
        if (currentCanvas == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Give the current canvas to the service.
            val newCanvas = filter(currentCanvas)
            // Update the canvas with the new result.
            _uiState.update { it.copy(isLoading = false, editingBitmap = newCanvas) }

            Log.d(
                "DebugDocuSnap",
                "Filter Applied on index: ${_uiState.value.currentImageIndex}. " +
                        "Canvas updated. Current processedImageUris map: ${processedImageUris}"
            )
        }
    }

    // --- 4. CREATE PUBLIC FUNCTIONS that call the generic applyFilter ---
    // These are what the UI buttons will call.

    fun applyBinarizationFilter() {
        // We wrap the service call in a simple lambda.
        // The lambda matches the type `suspend (Bitmap) -> Bitmap`.
        applyFilter { bitmap -> imageProcService.applyThresholdFilter(bitmap, 4) }
    }

    fun applyHighContrastFilter() {
        // For functions with one parameter, the syntax is even simpler.
        applyFilter { bitmap -> imageProcService.applyHighContrast(bitmap) }
        // Or using the shorter 'it' syntax:
        // applyFilter { imageProcService.applyHighContrast(it) }
    }

    fun applyAutoFilter() {
        applyFilter { bitmap -> imageProcService.autoProcessing(bitmap) }
    }

    fun applyColorEnhancementFilter() {
        applyFilter { bitmap -> imageProcService.enhanceColors(bitmap) }
    }

    fun resetToOriginal() {
        // Simply put the stored original bitmap back onto the canvas. No I/O needed.
        _uiState.update { it.copy(editingBitmap = originalBitmap) }
    }

    // --- 5. REWORK NAVIGATION and INITIALIZATION ---

    fun setInitialPhotosAndLoadFirst(photoUris: String?) {
        val uris = photoUris?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        _uiState.update { it.copy(originalImageUris = uris, currentImageIndex = 0) }
        // After setting the list, immediately load the first image.
        loadImageForCurrentIndex()
    }

    fun goToNextImage() {
        val currentState = _uiState.value
        if (currentState.currentImageIndex < currentState.originalImageUris.size - 1) {
            // Save the current state before switching
            saveCurrentEdit()
            val newIndex = currentState.currentImageIndex + 1
            _uiState.update { it.copy(currentImageIndex = newIndex) }
            loadImageForCurrentIndex()
        }
    }

    fun goToPreviousImage() {
        val currentState = _uiState.value
        if (currentState.currentImageIndex > 0) {
            saveCurrentEdit()
            val newIndex = currentState.currentImageIndex - 1
            _uiState.update { it.copy(currentImageIndex = newIndex) }
            loadImageForCurrentIndex()
        }
    }

    /**
     * Loads the image for the current index onto the canvas.
     */
    private fun loadImageForCurrentIndex() {
        val currentState = _uiState.value
        val uriToLoad = processedImageUris[currentState.currentImageIndex] ?: currentState.originalImageUris.getOrNull(currentState.currentImageIndex)

        if (uriToLoad == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withContext(Dispatchers.IO) {
                val loadedBitmap = uriToBitmap(uriToLoad)
                originalBitmap = loadedBitmap // Always store the first loaded version for reset.
                _uiState.update {
                    it.copy(isLoading = false, editingBitmap = loadedBitmap)
                }
            }
        }
    }

    /**
     * Saves the current edited bitmap to a file and stores its URI.
     */
    private fun saveCurrentEdit(): Job { // 1. Add Job as the return type
        val bitmapToSave = _uiState.value.editingBitmap ?: return Job().apply { complete() }
        val currentIndex = _uiState.value.currentImageIndex

        return viewModelScope.launch(Dispatchers.IO) { // 2. Return the result of launch
            val savedUri = saveBitmapToCache(bitmapToSave)
            if (savedUri != null) {
                processedImageUris[currentIndex] = savedUri.toString()
                Log.d(
                    "DebugDocuSnap",
                    "saveCurrentEdit (on switch/done) for index: ${currentIndex}. " +
                            "URI saved. Map is now: ${processedImageUris}"
                )
            }
        }
    }

    suspend fun getFinalUris(): List<String> {
        // Before navigating away, ensure the very last edit is also saved.
        saveCurrentEdit().join()
        Log.d(
            "DebugDocuSnap",
            "getFinalUris called. Final map state before creating list: ${processedImageUris}"
        )
        return _uiState.value.originalImageUris.mapIndexed { index, originalUri ->
            processedImageUris[index] ?: originalUri
        }
    }

    fun toggleFilterToolbar() {
        _uiState.update { it.copy(isFilterToolbarVisible = !it.isFilterToolbarVisible) }
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