package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

@Composable
fun ImageProcessingScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit,
    photoUri: String? = null,
    source: String = "document"
) {
    var isProcessing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Original") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Image Processing") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // Main Image Display
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
                // Image placeholder or loaded image
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing || isSaving) {
                        CircularProgressIndicator()
                    } else if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Captured Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
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

                Spacer(modifier = Modifier.height(16.dp))

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
        }

        // Bottom Tool Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Editing Tools",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Tool buttons
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterButton(
                            text = "Auto Process",
                            icon = Icons.Default.AutoFixHigh,
                            onClick = {
                                isProcessing = true
                                // Simulate processing
                                scope.launch {
                                    kotlinx.coroutines.delay(2000)
                                    isProcessing = false
                                    selectedFilter = "Processed"
                                }
                            }
                        )
                    }
                    item {
                        FilterButton(
                            text = "Crop",
                            icon = Icons.Default.Crop,
                            onClick = { /* Crop functionality */ }
                        )
                    }
                    item {
                        FilterButton(
                            text = "Grayscale",
                            icon = Icons.Default.Tonality,
                            onClick = { selectedFilter = "Grayscale" },
                            isSelected = selectedFilter == "Grayscale"
                        )
                    }
                    item {
                        FilterButton(
                            text = "Perspective",
                            icon = Icons.Default.Transform,
                            onClick = { /* Perspective correction */ }
                        )
                    }
                    item {
                        FilterButton(
                            text = "High Contrast",
                            icon = Icons.Default.Contrast,
                            onClick = { selectedFilter = "High Contrast" },
                            isSelected = selectedFilter == "High Contrast"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { selectedFilter = "Original" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset")
                    }
                    Button(
                        onClick = {
                            // Navigate based on source
                            val destination = when (source) {
                                "document" -> "document_gallery"
                                "form" -> "form_overview"
                                else -> "home"
                            }
                            onNavigate(destination)
//                            if (photoUri != null) {
//                                scope.launch {
//                                    isSaving = true
//                                    saveImageToGallery(context, photoUri, selectedFilter)
//                                    isSaving = false
//                                }
//                            } else {
//                                Toast.makeText(context, "No image to save", Toast.LENGTH_SHORT).show()
//                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving && photoUri != null
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Done Editing")
                    }
                }
            }
        }
    }
}

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
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 12.sp)
    }
}

private suspend fun saveImageToGallery(context: Context, imageUri: String, filter: String) {
    try {
        val uri = Uri.parse(imageUri)
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (bitmap != null) {
            // Apply filter effect (simplified - in real app you'd apply actual image processing)
            val processedBitmap = applyFilter(bitmap, filter)
            
            // Save to gallery
            val savedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveImageToGalleryAPI29Plus(context, processedBitmap, filter)
            } else {
                saveImageToGalleryLegacy(context, processedBitmap, filter)
            }
            
            if (savedUri != null) {
                Toast.makeText(context, "Image saved to gallery successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun applyFilter(bitmap: Bitmap, filter: String): Bitmap {
    // This is a simplified filter application
    // In a real app, you would apply actual image processing algorithms
    return when (filter) {
        "Grayscale" -> {
            // Convert to grayscale
            val width = bitmap.width
            val height = bitmap.height
            val grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    val pixel = bitmap.getPixel(x, y)
                    val gray = (android.graphics.Color.red(pixel) * 0.299 + 
                               android.graphics.Color.green(pixel) * 0.587 + 
                               android.graphics.Color.blue(pixel) * 0.114).toInt()
                    grayBitmap.setPixel(x, y, android.graphics.Color.rgb(gray, gray, gray))
                }
            }
            grayBitmap
        }
        "High Contrast" -> {
            // Apply high contrast effect
            val width = bitmap.width
            val height = bitmap.height
            val contrastBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    val pixel = bitmap.getPixel(x, y)
                    val factor = 1.5f // Contrast factor
                    val red = ((android.graphics.Color.red(pixel) - 128) * factor + 128).toInt().coerceIn(0, 255)
                    val green = ((android.graphics.Color.green(pixel) - 128) * factor + 128).toInt().coerceIn(0, 255)
                    val blue = ((android.graphics.Color.blue(pixel) - 128) * factor + 128).toInt().coerceIn(0, 255)
                    contrastBitmap.setPixel(x, y, android.graphics.Color.rgb(red, green, blue))
                }
            }
            contrastBitmap
        }
        "Processed" -> {
            // Apply auto-processing effect (enhanced brightness and contrast)
            val width = bitmap.width
            val height = bitmap.height
            val processedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    val pixel = bitmap.getPixel(x, y)
                    val factor = 1.3f // Enhancement factor
                    val red = ((android.graphics.Color.red(pixel) - 128) * factor + 128).toInt().coerceIn(0, 255)
                    val green = ((android.graphics.Color.green(pixel) - 128) * factor + 128).toInt().coerceIn(0, 255)
                    val blue = ((android.graphics.Color.blue(pixel) - 128) * factor + 128).toInt().coerceIn(0, 255)
                    processedBitmap.setPixel(x, y, android.graphics.Color.rgb(red, green, blue))
                }
            }
            processedBitmap
        }
        else -> bitmap // Return original for other filters
    }
}

private fun saveImageToGalleryAPI29Plus(context: Context, bitmap: Bitmap, filter: String): Uri? {
    val filename = "DocuSnap_${filter}_${System.currentTimeMillis()}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DocuSnap")
    }

    val contentResolver = context.contentResolver
    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let { imageUri ->
        contentResolver.openOutputStream(imageUri)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        }
    }

    return uri
}

private fun saveImageToGalleryLegacy(context: Context, bitmap: Bitmap, filter: String): Uri? {
    val filename = "DocuSnap_${filter}_${System.currentTimeMillis()}.jpg"
    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    val docuSnapDir = File(picturesDir, "DocuSnap")
    
    if (!docuSnapDir.exists()) {
        docuSnapDir.mkdirs()
    }
    
    val imageFile = File(docuSnapDir, filename)
    
    return try {
        FileOutputStream(imageFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        }
        
        // Notify gallery about the new image
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    } catch (e: IOException) {
        null
    }
}