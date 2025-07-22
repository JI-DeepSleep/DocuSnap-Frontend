package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import cn.edu.sjtu.deepsleep.docusnap.data.Document
import cn.edu.sjtu.deepsleep.docusnap.data.Form
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.FileProvider
import android.widget.Toast
import androidx.compose.material.icons.outlined.PhotoFilter
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import cn.edu.sjtu.deepsleep.docusnap.ui.viewmodels.ImageProcessingViewModel
import cn.edu.sjtu.deepsleep.docusnap.ui.viewmodels.ImageProcessingViewModelFactory
import cn.edu.sjtu.deepsleep.docusnap.ui.components.CornerAdjustmentOverlay
import cn.edu.sjtu.deepsleep.docusnap.ui.viewmodels.DocumentViewModel
import cn.edu.sjtu.deepsleep.docusnap.ui.viewmodels.DocumentViewModelFactory
import cn.edu.sjtu.deepsleep.docusnap.di.AppModule

private const val TAG = "ImageProcessingScreen"


@Composable
fun ImageProcessingScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit,
    photoUris: String? = null, // Changed from originalImageUri to photoUris
    source: String = "document"
) {
    val context = LocalContext.current
    val imageProcessingViewModel: ImageProcessingViewModel = viewModel(
        factory = ImageProcessingViewModelFactory(context)
    )
    val documentViewModel: DocumentViewModel = viewModel(
        factory = DocumentViewModelFactory(AppModule.provideDocumentRepository(context))
    )
    val uiState by imageProcessingViewModel.uiState.collectAsState()

    LaunchedEffect(key1 = true) {
        imageProcessingViewModel.events.collect { eventMessage ->
            Toast.makeText(context, eventMessage, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(key1 = photoUris) {
        Log.d("ImageProcessingScreen","launching photoUris for display: $photoUris")
        imageProcessingViewModel.setInitialPhotosAndLoadFirst(photoUris)
    }


    val scope = rememberCoroutineScope()

    fun uriToBase64(uri: String): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(Uri.parse(uri))
            val bytes = inputStream?.readBytes() ?: byteArrayOf()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    // Function to create new document or form and navigate to it
    fun createAndNavigateToDetail() {
        scope.launch {
            val finalUris = imageProcessingViewModel.getFinalUris()
            // Convert URIs to Base64
            val base64Images = finalUris.map { uriToBase64(it) }

            // Get current date in the required format
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            base64Images.forEachIndexed { index, base64 ->
                val preview = if (base64.length >= 20) {
                    "${base64.take(10)}...${base64.takeLast(10)}"
                } else {
                    base64 // Show full string if too short
                }
                Log.e(TAG, "Image $index (${base64.length} chars): $preview")
            }

            when (source) {
                "document" -> {
                    // Create a new document object and save to database
                    val newDocument = Document(
                        id = UUID.randomUUID().toString(),
                        name = "New Document ${System.currentTimeMillis()}",
                        description = "A new document created by user",
                        imageBase64s = base64Images,
                        extractedInfo = emptyMap(),
                        tags = listOf("New", "Document"),
                        uploadDate = currentDate
                    )
                    // Save the document to the database
                    documentViewModel.saveDocument(newDocument)

                    // Navigate with just the document ID
                    onNavigate("document_detail?documentId=${newDocument.id}&fromImageProcessing=true")
                }
                "form" -> {
                    // Create a new form object and save to database
                    val newForm = Form(
                        id = UUID.randomUUID().toString(),
                        name = "New Form ${System.currentTimeMillis()}",
                        description = "A new form uploaded by user",
                        imageBase64s = base64Images,
                        formFields = emptyList(),
                        extractedInfo = emptyMap(),
                        uploadDate = currentDate
                    )
                    // Save the form to the database
                    documentViewModel.saveForm(newForm)

                    // Navigate with just the form ID
                    onNavigate("form_detail?formId=${newForm.id}&fromImageProcessing=true")
                }
                else -> onNavigate("home")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar with Done/Apply button
        TopAppBar(
            title = { Text("Image Processing") },
            navigationIcon = {
                IconButton(onClick = {
                    if (uiState.isCornerAdjustmentMode) {
                        imageProcessingViewModel.cancelCornerAdjustment()
                    } else {
                        onBackClick()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (uiState.isCornerAdjustmentMode) {
                    // Show Apply button when in corner adjustment mode
                    Button(
                        onClick = { imageProcessingViewModel.applyCornerAdjustment() }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply")
                    }
                } else {
                    // Show Done button in normal mode
                    Button(
                        onClick = { createAndNavigateToDetail() }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Done")
                    }
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
                        .fillMaxWidth()
                        .aspectRatio(
                            // Calculate aspect ratio from the bitmap if available, otherwise use a default
                            ratio = uiState.editingBitmap?.let { bitmap ->
                                bitmap.width.toFloat() / bitmap.height.toFloat()
                            } ?: 1f, // Default to square if no bitmap
                            matchHeightConstraintsFirst = false
                        )
                        .padding(horizontal = 16.dp), // Add some horizontal padding
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
                                contentScale = ContentScale.Fit // Changed from Crop to Fit to show entire image
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

            // Corner Adjustment Overlay
            if (uiState.isCornerAdjustmentMode && uiState.editingBitmap != null && uiState.adjustedCorners != null) {
                CornerAdjustmentOverlay(
                    bitmap = uiState.editingBitmap!!,
                    corners = uiState.adjustedCorners!!,
                    onCornerMoved = { cornerIndex, newPosition ->
                        imageProcessingViewModel.updateCornerPosition(cornerIndex, newPosition)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // --- 4. CONNECT Navigation arrows to ViewModel functions ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous arrow
                IconButton(
                    onClick = { imageProcessingViewModel.goToPreviousImage() },
                    enabled = uiState.currentImageIndex > 0,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (uiState.currentImageIndex > 0) Color.White.copy(alpha = 0.8f) else Color.Gray.copy(
                                alpha = 0.5f
                            ),
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
                    onClick = { imageProcessingViewModel.goToNextImage() },
                    enabled = uiState.currentImageIndex < uiState.originalImageUris.size - 1,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (uiState.currentImageIndex < uiState.originalImageUris.size - 1) Color.White.copy(
                                alpha = 0.8f
                            ) else Color.Gray.copy(alpha = 0.5f),
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

        // Secondary Perspective Toolbar (appears when Perspective button is clicked)
        if (uiState.isPerspectiveToolbarVisible) {
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
                                text = "Straighten",
                                icon = Icons.Default.AutoFixHigh,
                                onClick = {
                                    imageProcessingViewModel.applyPerspectiveCorrectionFilter()
                                },
                                isSelected = uiState.appliedFilter == "Perspective Correction",
                                enabled = uiState.appliedFilter != "Perspective Correction"
                            )
                        }
//                        item {
//                            FilterButton(
//                                text = "Rotate 90°",
//                                icon = Icons.Default.RotateRight,
//                                onClick = {
//                                    viewModel.applyRotation(90)
//                                },
//                                isSelected = false,
//                                enabled = true
//                            )
//                        }
                    }
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
                                     imageProcessingViewModel.resetToOriginal()
                                },
                                isSelected = uiState.appliedFilter == null,
                                enabled = true
                            )
                        }
                        item {
                            FilterButton(
                                text = "Monochrome",
                                icon = Icons.Default.Tonality,
                                onClick = {
                                    imageProcessingViewModel.applyBinarizationFilter()
                                },
                                isSelected = uiState.appliedFilter == "Black & White",
                                enabled = uiState.appliedFilter != "Black & White"
                            )
                        }
                        item {
                            FilterButton(
                                text = "High Contrast",
                                icon = Icons.Default.Contrast,
                                onClick = {
                                    imageProcessingViewModel.applyHighContrastFilter()
                                },
                                isSelected = uiState.appliedFilter == "High Contrast",
                                enabled = uiState.appliedFilter != "High Contrast"
                            )
                        }
                        item {
                            FilterButton(
                                text = "Color Enhancement",
                                icon = Icons.Outlined.ColorLens,
                                onClick = {
                                    imageProcessingViewModel.applyColorEnhancementFilter()
                                },
                                isSelected = uiState.appliedFilter == "Color Enhancement",
                                enabled = uiState.appliedFilter != "Color Enhancement"
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
                                imageProcessingViewModel.applyAutoFilter()
                            },
                            isSelected = uiState.appliedFilter == "Auto",
                            enabled = uiState.appliedFilter != "Auto"
                        )
                    }
                    item {
                        FilterButton(
                            text = "Filter",
                            icon = Icons.Outlined.PhotoFilter,
                            onClick = { imageProcessingViewModel.toggleFilterToolbar() },
                            isSelected = uiState.isFilterToolbarVisible
                        )
                    }
                    item {
                        FilterButton(
                            text = "Perspective",
                            icon = Icons.Default.Transform,
                            onClick = { imageProcessingViewModel.togglePerspectiveToolbar() },
                            isSelected = uiState.isPerspectiveToolbarVisible
                        )
                    }
                    item {
                        FilterButton(
                            text = "Reset",
                            icon = Icons.Default.Refresh,
                            onClick = {
                                imageProcessingViewModel.resetToOriginal()
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