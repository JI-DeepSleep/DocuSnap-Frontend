package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Filter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import cn.edu.sjtu.deepsleep.docusnap.data.Document
import cn.edu.sjtu.deepsleep.docusnap.data.Form
import java.util.UUID
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.FileProvider
import android.util.Log
import android.widget.Toast
import androidx.compose.material.icons.outlined.PhotoFilter
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap


@Composable
fun ImageProcessingScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit,
    photoUris: String? = null, // Changed from originalImageUri to photoUris
    source: String = "document"
) {
    val context = LocalContext.current
    val viewModel: ImageProcessingViewModel = viewModel(
        factory = ImageProcessingViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(key1 = true) {
        viewModel.events.collect { eventMessage ->
            Toast.makeText(context, eventMessage, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(key1 = photoUris) {
        viewModel.setInitialPhotosAndLoadFirst(photoUris)
    }


    val scope = rememberCoroutineScope()

    // Function to create new document or form and navigate to it
    fun createAndNavigateToDetail() {
        scope.launch{
            val finalUris = viewModel.getFinalUris()

            // [1. PREPARE DATA] Convert the list of Uris into a single, URL-safe string.
            // 第一步：准备数据。将 URI 列表转换成一个 URL 安全的字符串。
            val urisString = finalUris.joinToString(",")
            val encodedUris = java.net.URLEncoder.encode(urisString, "UTF-8")

            when (source) {
                "document" -> {
                    // Create a new document object (this part is unchanged)
                    val newDocument = Document(
                        id = UUID.randomUUID().toString(),
                        name = "New Document ${System.currentTimeMillis()}",
                        description = "A new document created by user",
                        imageUris = finalUris,
                        extractedInfo = emptyMap(),
                        tags = listOf("New", "Document"),
                        uploadDate = "2024-01-15"
                    )
                    // TODO: Save the document to the database/storage

                    // [2. MODIFY NAVIGATION] Add the encodedUris to the navigation route.
                    onNavigate("document_detail?documentId=${newDocument.id}&fromImageProcessing=true&photoUris=$encodedUris")
                }
                "form" -> {
                    // Create a new form object (this part is unchanged)
                    val newForm = Form(
                        id = UUID.randomUUID().toString(),
                        name = "New Form ${System.currentTimeMillis()}",
                        description = "A new form uploaded by user",
                        imageUris = finalUris,
                        formFields = emptyList(),
                        extractedInfo = emptyMap(),
                        uploadDate = "2024-01-15"
                    )
                    // TODO: Save the form to the database/storage

                    // [3. MODIFY NAVIGATION] Add the encodedUris to the navigation route.
                    onNavigate("form_detail?formId=${newForm.id}&fromImageProcessing=true&photoUris=$encodedUris")
                }
                else -> onNavigate("home")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar with Done button
        TopAppBar(
            title = { Text("Image Processing") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                Button(
                    onClick = { createAndNavigateToDetail() }
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Done")
                }
            }
        )

        // Main Image Display with Navigation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .background(
                    Color.Gray.copy(alpha = 0.1f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // --- 1. Get the current display URI from the ViewModel state ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- 2. CONNECT Image index display to ViewModel state ---
                if (uiState.originalImageUris.isNotEmpty()) {
                    Text(
                        text = "${uiState.currentImageIndex + 1}/${uiState.originalImageUris.size}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // --- 3. REBUILD Image placeholder/loaded image logic with ViewModel state ---
                Box(
                    modifier = Modifier
                        .size(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Priority 1: Show a loading spinner if processing.
                    if (uiState.isLoading) {
                        CircularProgressIndicator()
                        Text(
                            text = "Processing image...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Priority 2: If we have a live preview Bitmap, display it.
                    else if (uiState.editingBitmap != null) {
                        uiState.editingBitmap?.let { bitmapToShow ->
                            Image(
                                bitmap = bitmapToShow.asImageBitmap(),
                                contentDescription = "Editing Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    // Priority 3: If there is no image at all, show a placeholder.
                    else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "📄", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Document Image", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // --- 4. CONNECT Navigation arrows to ViewModel functions ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous arrow
                IconButton(
                    onClick = { viewModel.goToPreviousImage() },
                    enabled = uiState.currentImageIndex > 0,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (uiState.currentImageIndex > 0) Color.White.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous image",
                        tint = if (uiState.currentImageIndex > 0) Color.Black else Color.Gray
                    )
                }

                // Next arrow
                IconButton(
                    onClick = { viewModel.goToNextImage() },
                    enabled = uiState.currentImageIndex < uiState.originalImageUris.size - 1,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (uiState.currentImageIndex < uiState.originalImageUris.size - 1) Color.White.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next image",
                        tint = if (uiState.currentImageIndex < uiState.originalImageUris.size - 1) Color.Black else Color.Gray
                    )
                }
            }
        }

        // Secondary Filter Toolbar (appears when Filter button is clicked)
        if (uiState.isFilterToolbarVisible) { // showFilterToolbar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterButton(
                                text = "Original",
                                icon = Icons.Default.Refresh,
                                onClick = {
                                     viewModel.resetToOriginal()
                                },
                                isSelected = uiState.appliedFilters.isEmpty(),
                                enabled = true
                            )
                        }
                        item {
                            FilterButton(
                                text = "Black & White",
                                icon = Icons.Default.Tonality,
                                onClick = {
                                    viewModel.applyBinarizationFilter()
                                },
                                isSelected = uiState.appliedFilters.contains("Black & White"),
                                enabled = !uiState.appliedFilters.contains("Black & White")
                            )
                        }
                        item {
                            FilterButton(
                                text = "High Contrast",
                                icon = Icons.Default.Contrast,
                                onClick = {
                                    viewModel.applyHighContrastFilter()
                                },
                                isSelected = uiState.appliedFilters.contains("High Contrast"),
                                enabled = !uiState.appliedFilters.contains("High Contrast")
                            )
                        }
                        item {
                            FilterButton(
                                text = "Color Enhancement",
                                icon = Icons.Outlined.ColorLens,
                                onClick = {
                                    viewModel.applyColorEnhancementFilter()
                                },
                                isSelected = uiState.appliedFilters.contains("Color Enhancement"),
                                enabled = !uiState.appliedFilters.contains("Color Enhancement")
                            )
                        }
                    }
                }
            }
        }

        // Primary Bottom Tool Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                // Primary tool buttons
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterButton(
                            text = "Auto",
                            icon = Icons.Default.AutoFixHigh,
                            onClick = {
                                viewModel.applyAutoFilter()
                            },
                            isSelected = uiState.appliedFilters.contains("Auto"),
                            enabled = !uiState.appliedFilters.contains("Auto")
                        )
                    }
                    item {
                        FilterButton(
                            text = "Filter",
                            icon = Icons.Outlined.PhotoFilter,
                            onClick = { viewModel.toggleFilterToolbar() }, // showFilterToolbar = !showFilterToolbar
                            isSelected = uiState.isFilterToolbarVisible // isSelected = false // showFilterToolbar
                        )
                    }
                    item {
                        FilterButton(
                            text = "Perspective",
                            icon = Icons.Default.Transform,
                            onClick = {
                                // TODO: complete the function
                            }
                        )
                    }
                    item {
                        FilterButton(
                            text = "Reset",
                            icon = Icons.Default.Refresh,
                            onClick = {
                                viewModel.resetToOriginal()
                            },
                            isSelected = false,
                            enabled = true
                        )
                    }
                }
            }
        }
    }
}

// Placeholder functions for image processing (to be implemented later)
/*
private fun performAutoProcessing(imageUri: String?): String? {
    // TODO: Implement binary thresholding + 4 point perspective correction
    return imageUri
}

private fun performPerspectiveCorrection(imageUri: String?): String? {
    // TODO: Implement 4 point perspective correction
    return imageUri
}

private fun applyOriginalFilter(imageUri: String?): String? {
    // TODO: Apply original filter
    return imageUri
}


private fun applyBinaryThresholdingFilter(imageUri: String?): String? {
    // TODO: Apply binary thresholding filter
    return imageUri
}

private fun applyColorEnhancementFilter(imageUri: String?): String? {
    // TODO: Apply color enhancement filter
    return imageUri
}
*/

@Composable
private fun FilterButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(text, fontSize = 12.sp)
    }
}

// Helper function to decode a Uri string into a Bitmap.
private fun uriToBitmap(context: Context, uriString: String): Bitmap? {
    return try {
        val imageUri = Uri.parse(uriString)
        val source = ImageDecoder.createSource(context.contentResolver, imageUri)
        // Decode the image. Add memory optimization later if needed.
        ImageDecoder.decodeBitmap(source)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun applyBlackAndWhiteFilter(bitmap: Bitmap): Bitmap {
    val newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val width = newBitmap.width
    val height = newBitmap.height
    val pixels = IntArray(width * height)

    newBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    for (i in pixels.indices) {
        val pixel = pixels[i]
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        val gray = (r * 0.3 + g * 0.59 + b * 0.11).toInt()
        pixels[i] = 0xFF000000.toInt() or (gray shl 16) or (gray shl 8) or gray
    }

    newBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return newBitmap
}

private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs() // Create the cache directory if it doesn't exist.

        // Create a new file for the bitmap.
        val file = File(cachePath, "processed_image_${System.currentTimeMillis()}.jpg")
        val stream = FileOutputStream(file)

        // Compress the bitmap to the file.
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        stream.close()

        // Get a content Uri for the file using FileProvider.
        // NOTE: Your app's applicationId must match the authority string.
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
