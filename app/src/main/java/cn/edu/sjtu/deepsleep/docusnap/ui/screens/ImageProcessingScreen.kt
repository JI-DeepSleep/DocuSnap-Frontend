package cn.edu.sjtu.deepsleep.docusnap.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun ImageProcessingScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Original") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Auto Processing") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                TextButton(
                    onClick = { onNavigate("home") }
                ) {
                    Text("Done Editing")
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
                // Image placeholder
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .background(
                            when (selectedFilter) {
                                "Grayscale" -> Color.Gray
                                "High Contrast" -> Color.Black
                                else -> Color.White
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator()
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
                        onClick = { onNavigate("home") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
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