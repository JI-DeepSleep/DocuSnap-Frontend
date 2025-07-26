package cn.edu.sjtu.deepsleep.docusnap.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.PointF
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.edu.sjtu.deepsleep.docusnap.AppModule
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// --- 1. REDEFINE THE UI STATE ---
// It's simpler now. It mainly holds the "canvas" (editingBitmap).
data class ImageProcessingUiState(
    val isLoading: Boolean = false,
    val originalImageUris: List<String> = emptyList(),
    val currentImageIndex: Int = 0,
    // This is the "canvas". It holds the current state of the image being edited.
    val editingBitmap: Bitmap? = null,
    val isFilterToolbarVisible: Boolean = false,
    val isPerspectiveToolbarVisible: Boolean = false,
    val appliedFilter: String? = null, // Changed from appliedFilters to appliedFilter (single filter only)
    // Corner adjustment state
    val isCornerAdjustmentMode: Boolean = false,
    val detectedCorners: Array<PointF>? = null,
    val adjustedCorners: Array<PointF>? = null
)

class ImageProcessingViewModel(
    private val context: Context,
    private val imageProcService: ImageProcService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageProcessingUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

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
    private fun applyFilter(filterName: String, filter: suspend (Bitmap) -> Bitmap) {
        val currentCanvas = _uiState.value.editingBitmap

        if (currentCanvas == null || _uiState.value.appliedFilter == filterName) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val newCanvas = filter(currentCanvas)


            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    editingBitmap = newCanvas,
                    appliedFilter = filterName
                )
            }
        }
    }
    // --- 4. CREATE PUBLIC FUNCTIONS that call the generic applyFilter ---
    // These are what the UI buttons will call.

    fun applyBinarizationFilter() {
        applyFilter("Black & White") { bitmap ->
            imageProcService.applyThresholdFilter(bitmap, 4)
        }
    }

    fun applyHighContrastFilter() {
        applyFilter("High Contrast") { bitmap ->
            imageProcService.applyHighContrast(bitmap)
        }
    }

    fun applyAutoFilter() {
        applyFilter("Auto") { bitmap ->
            imageProcService.autoProcessing(bitmap)
        }
    }

    fun applyColorEnhancementFilter() {
        applyFilter("Color Enhancement") { bitmap ->
            imageProcService.enhanceColors(bitmap)
        }
    }

    fun applyPerspectiveCorrectionFilter() {
        val currentBitmap = _uiState.value.editingBitmap
        if (currentBitmap == null) return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // First, try to detect corners automatically
                val detectedCorners = imageProcService.findDocumentCorners(currentBitmap)
                
                if (detectedCorners != null) {
                    // Show corner adjustment mode with detected corners
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isCornerAdjustmentMode = true,
                            detectedCorners = detectedCorners,
                            adjustedCorners = Array(4) { index -> 
                                PointF(detectedCorners[index].x, detectedCorners[index].y) 
                            } // Copy detected corners
                        )
                    }
                } else {
                    // No corners detected, apply filter directly as fallback
                    val correctedBitmap = imageProcService.correctPerspective(currentBitmap)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            editingBitmap = correctedBitmap,
                            appliedFilter = "Perspective Correction"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
        fun updateCornerPosition(cornerIndex: Int, newPosition: PointF) {
        val currentCorners = _uiState.value.adjustedCorners ?: return
        
        // Reuse existing PointF objects where possible
        currentCorners[cornerIndex].set(newPosition.x, newPosition.y)
        
        _uiState.update { 
            it.copy(adjustedCorners = currentCorners) 
        }
    }
    
    fun applyCornerAdjustment() {
        val currentBitmap = _uiState.value.editingBitmap
        val adjustedCorners = _uiState.value.adjustedCorners
        
        if (currentBitmap == null || adjustedCorners == null) return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // Apply perspective correction with the user-adjusted corners
                val correctedBitmap = imageProcService.performPerspectiveCorrectionWithCorners(currentBitmap, adjustedCorners)
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        editingBitmap = correctedBitmap,
                        appliedFilter = "Perspective Correction",
                        isCornerAdjustmentMode = false,
                        detectedCorners = null,
                        adjustedCorners = null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun cancelCornerAdjustment() {
        _uiState.update { 
            it.copy(
                isCornerAdjustmentMode = false,
                detectedCorners = null,
                adjustedCorners = null
            )
        }
    }
    fun resetToOriginal() {
        // Simply put the stored original bitmap back onto the canvas. No I/O needed.
        _uiState.update { it.copy(
            editingBitmap = originalBitmap,
            appliedFilter = null
        )}
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
                    it.copy(isLoading = false, editingBitmap = loadedBitmap,  appliedFilter = null)
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
        _uiState.update { currentState ->
            currentState.copy(
                isFilterToolbarVisible = !currentState.isFilterToolbarVisible,
                isPerspectiveToolbarVisible = false // Close perspective toolbar when opening filter toolbar
            )
        }
    }

    fun togglePerspectiveToolbar() {
        _uiState.update { currentState ->
            currentState.copy(
                isPerspectiveToolbarVisible = !currentState.isPerspectiveToolbarVisible,
                isFilterToolbarVisible = false // Close filter toolbar when opening perspective toolbar
            )
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
            viewModelScope.launch { _events.emit("Failed to load image") }
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