package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

@Composable
fun ImageProcessingScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit,
    originalImageUri: String? = null,
    source: String = "document"
) {
    var isProcessing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Original") }
    var showFilterToolbar by remember { mutableStateOf(false) }
    var currentImageUri by remember { mutableStateOf(originalImageUri) }
    val scope = rememberCoroutineScope()

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
                    onClick = {
                        // Navigate based on source
                        val destination = when (source) {
                            "document" -> "document_detail"
                            "form" -> "form_detail"
                            else -> "home"
                        }
                        onNavigate(destination)
                        // TODO: background parsing document or form
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Done")
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
                        .size(300.dp),
                    contentAlignment = Alignment.Center
                ) {
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
                    } else if (currentImageUri != null) {
                        AsyncImage(
                            model = currentImageUri,
                            contentDescription = "Processed Image",
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
                                    // TODO: Apply original filter
                                    // currentImageUri = applyOriginalFilter(currentImageUri)
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
                                    // TODO: Apply black and white filter
                                    // currentImageUri = applyBlackAndWhiteFilter(currentImageUri)
                                },
                                isSelected = selectedFilter == "Black & White"
                            )
                        }
                        item {
                            FilterButton(
                                text = "B&W High Contrast",
                                icon = Icons.Default.Contrast,
                                onClick = {
                                    selectedFilter = "B&W High Contrast"
                                    // TODO: Apply binary thresholding filter
                                    // currentImageUri = applyBinaryThresholdingFilter(currentImageUri)
                                },
                                isSelected = selectedFilter == "B&W High Contrast"
                            )
                        }
                        item {
                            FilterButton(
                                text = "Color Enhancement",
                                icon = Icons.Default.AutoFixHigh,
                                onClick = {
                                    selectedFilter = "Color Enhancement"
                                    // TODO: Apply color enhancement filter
                                    // currentImageUri = applyColorEnhancementFilter(currentImageUri)
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
                                isProcessing = true
                                scope.launch {
                                    // TODO: Implement auto-processing with binary thresholding + 4 point perspective correction
                                    // currentImageUri = performAutoProcessing(currentImageUri)
                                    kotlinx.coroutines.delay(1000)
                                    isProcessing = false
                                    selectedFilter = "Auto Processed"
                                }
                            }
                        )
                    }
                    item {
                        FilterButton(
                            text = "Filter",
                            icon = Icons.Default.FilterAlt,
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
                                isProcessing = true
                                scope.launch {
                                    // TODO: Implement 4 point perspective correction
                                    // currentImageUri = performPerspectiveCorrection(currentImageUri)
                                    kotlinx.coroutines.delay(1000)
                                    isProcessing = false
                                    selectedFilter = "Perspective Corrected"
                                }
                            }
                        )
                    }
                    item {
                        FilterButton(
                            text = "Reset",
                            icon = Icons.Default.Refresh,
                            onClick = {
                                selectedFilter = "Original"
                                currentImageUri = originalImageUri
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

private fun applyBlackAndWhiteFilter(imageUri: String?): String? {
    // TODO: Apply black and white filter
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