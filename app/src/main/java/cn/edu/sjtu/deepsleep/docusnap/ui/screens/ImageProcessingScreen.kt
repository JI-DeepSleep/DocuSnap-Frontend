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

@Composable
fun ImageProcessingScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit,
    photoUris: String? = null, // Changed from originalImageUri to photoUris
    source: String = "document"
) {
    var isProcessing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Original") }
    var showFilterToolbar by remember { mutableStateOf(false) }
    var currentImageIndex by remember { mutableStateOf(0) }
    var processedImages by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Parse photoUris string into list
    val imageUris = remember(photoUris) {
        photoUris?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    // Current image URI
    val currentImageUri = remember(currentImageIndex, imageUris, processedImages) {
        processedImages[currentImageIndex] ?: imageUris.getOrNull(currentImageIndex)
    }

    // Navigation functions
    fun goToPreviousImage() {
        if (currentImageIndex > 0) {
            currentImageIndex--
        }
    }

    fun goToNextImage() {
        if (currentImageIndex < imageUris.size - 1) {
            currentImageIndex++
        }
    }

    // Image processing functions
    // [ 用这个新函数替换旧的 processCurrentImage ]
    fun processCurrentImage(filterType: String) {
        if (currentImageUri == null) return

        isProcessing = true
        // [1. CHANGE] Use IO dispatcher for file operations as it can be slow.
        scope.launch(Dispatchers.IO) {
            val inputBitmap = uriToBitmap(context, currentImageUri)

            if (inputBitmap != null) {
                val outputBitmap = when (filterType) {
                    "Black & White" -> applyBlackAndWhiteFilter(inputBitmap)
                    else -> inputBitmap
                }

                // [2. ADD] Save the processed bitmap and get the new Uri.
                val newUri = saveBitmapToCache(context, outputBitmap)

                // [3. CHANGE] Switch back to the Main thread for UI and state updates.
                launch(Dispatchers.Main) {
                    if (newUri != null) {
                        // [KEY CHANGE] Store the URI of the NEW, saved file.
                        processedImages = processedImages + (currentImageIndex to newUri.toString())
                        Log.d("ImageSaveDebug", "Map updated for index $currentImageIndex. New URI: $newUri. Map now has ${processedImages.size} items.")
                    } else {
                    // [ADD THIS LOG] Log an error if saving failed.
                    // 【添加这行日志】如果保存失败，就打印一条错误日志。
                    Log.e("ImageSaveDebug", "saveBitmapToCache failed, newUri is null!")
                }

                    // We still use processedBitmap for live preview on this screen.
                    processedBitmap = outputBitmap
                    selectedFilter = filterType
                    isProcessing = false
                }
            } else {
                launch(Dispatchers.Main) {
                    isProcessing = false
                    Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Function to create new document or form and navigate to it
    fun createAndNavigateToDetail() {
        Log.d("ImageSaveDebug", "Create/Navigate called. Current processedImages map: $processedImages")
        val processedImageUris = if (processedImages.isNotEmpty()) {
            processedImages.values.toList()
        } else {
            imageUris
        }

        // [1. PREPARE DATA] Convert the list of Uris into a single, URL-safe string.
        // 第一步：准备数据。将 URI 列表转换成一个 URL 安全的字符串。
        val urisString = processedImageUris.joinToString(",")
        val encodedUris = java.net.URLEncoder.encode(urisString, "UTF-8")

        when (source) {
            "document" -> {
                // Create a new document object (this part is unchanged)
                val newDocument = Document(
                    id = UUID.randomUUID().toString(),
                    name = "New Document ${System.currentTimeMillis()}",
                    description = "A new document created by user",
                    imageUris = processedImageUris,
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
                    imageUris = processedImageUris,
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Image index display
                if (imageUris.isNotEmpty()) {
                    Text(
                        text = "${currentImageIndex + 1}/${imageUris.size}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Image placeholder or loaded image
                Box(
                    modifier = Modifier
                        .size(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Priority 1: Show a loading spinner if processing.
                    if (isProcessing || isSaving) {
                        CircularProgressIndicator()
                        // Processing status
                        if (isProcessing) {
                            Text(
                                text = "Processing image...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (isSaving) {
                            Text(
                                text = "Saving to gallery...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    // Priority 2: If we have a processed Bitmap, display it.
                    else if (processedBitmap != null) {
                        Image(
                            bitmap = processedBitmap!!.asImageBitmap(),
                            contentDescription = "Processed Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Priority 3: Otherwise, display the original image from its Uri.
                    else if (currentImageUri != null) {
                        AsyncImage(
                            model = currentImageUri,
                            contentDescription = "Original Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Priority 4: If there is no image at all, show a placeholder.
                    else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📄",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Document Image",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Filter: $selectedFilter",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Navigation arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous arrow
                IconButton(
                    onClick = { goToPreviousImage() },
                    enabled = currentImageIndex > 0,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (currentImageIndex > 0) Color.White.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous image",
                        tint = if (currentImageIndex > 0) Color.Black else Color.Gray
                    )
                }

                // Next arrow
                IconButton(
                    onClick = { goToNextImage() },
                    enabled = currentImageIndex < imageUris.size - 1,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (currentImageIndex < imageUris.size - 1) Color.White.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next image",
                        tint = if (currentImageIndex < imageUris.size - 1) Color.Black else Color.Gray
                    )
                }
            }
        }

        // Secondary Filter Toolbar (appears when Filter button is clicked)
        if (showFilterToolbar) {
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
                                    selectedFilter = "Original"
                                    processCurrentImage("Original")
                                },
                                isSelected = selectedFilter == "Original"
                            )
                        }
                        item {
                            FilterButton(
                                text = "Black & White",
                                icon = Icons.Default.Tonality,
                                onClick = {
                                    selectedFilter = "Black & White"
                                    processCurrentImage("Black & White")
                                },
                                isSelected = selectedFilter == "Black & White"
                            )
                        }
                        item {
                            FilterButton(
                                text = "High Contrast",
                                icon = Icons.Default.Contrast,
                                onClick = {
                                    selectedFilter = "High Contrast"
                                    processCurrentImage("High Contrast")
                                },
                                isSelected = selectedFilter == "High Contrast"
                            )
                        }
                        item {
                            FilterButton(
                                text = "Color Enhancement",
                                icon = Icons.Outlined.ColorLens,
                                onClick = {
                                    selectedFilter = "Color Enhancement"
                                    processCurrentImage("Color Enhancement")
                                },
                                isSelected = selectedFilter == "Color Enhancement"
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
                                showFilterToolbar = false
                                processCurrentImage("Auto Processed")
                            }
                        )
                    }
                    item {
                        FilterButton(
                            text = "Filter",
                            icon = Icons.Outlined.PhotoFilter,
                            onClick = { showFilterToolbar = !showFilterToolbar },
                            isSelected = showFilterToolbar
                        )
                    }
                    item {
                        FilterButton(
                            text = "Perspective",
                            icon = Icons.Default.Transform,
                            onClick = {
                                showFilterToolbar = false
                                processCurrentImage("Perspective Corrected")
                            }
                        )
                    }
                    item {
                        FilterButton(
                            text = "Reset",
                            icon = Icons.Default.Refresh,
                            onClick = {
                                // [ADD THIS] Clear the processed bitmap to revert to the original.
                                processedBitmap = null
                                selectedFilter = "Original"
                                // The line below is now less relevant but we can keep it for now.
                                processedImages = processedImages - currentImageIndex
                                showFilterToolbar = false
                            }
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
    isSelected: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
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
    for (x in 0 until newBitmap.width) {
        for (y in 0 until newBitmap.height) {
            val pixel = newBitmap.getPixel(x, y)
            val gray = (AndroidColor.red(pixel) * 0.3 + AndroidColor.green(pixel) * 0.59 + AndroidColor.blue(pixel) * 0.11).toInt()
            newBitmap.setPixel(x, y, AndroidColor.rgb(gray, gray, gray))
        }
    }
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
