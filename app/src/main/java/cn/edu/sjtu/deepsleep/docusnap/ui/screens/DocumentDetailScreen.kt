package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.edu.sjtu.deepsleep.docusnap.data.MockData
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@Composable
fun DocumentDetailScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit,
    documentId: String? = null
) {
    // Find the specific document by ID, or use the first one as fallback
    val document = remember(documentId) {
        if (documentId != null) {
            MockData.mockDocuments.find { it.id == documentId } ?: MockData.mockDocuments.first()
        } else {
            MockData.mockDocuments.first()
        }
    }
    var isEditing by remember { mutableStateOf(false) }
    val originalExtractedInfo = remember { document.extractedInfo.toMap() }
    var extractedInfo by remember { mutableStateOf(originalExtractedInfo) }
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var parsing by remember { mutableStateOf(false) }
    var parsingJob by remember { mutableStateOf<Job?>(null) }
    var previousExtractedInfo by remember { mutableStateOf(originalExtractedInfo) }
    val scope = rememberCoroutineScope()
    
    // Image navigation state
    var currentImageIndex by remember { mutableStateOf(0) }
    val imageUris = remember { document.imageUris }
    
    // Get related files using MockData helper functions
    val relatedFiles = remember(document) {
        MockData.getRelatedDocuments(document.id)
    }
    
    // Placeholder for export and delete actions
    fun exportDocument() {
        // TODO: Implement export logic (save image(s) to gallery)
        Toast.makeText(context, "Document saved to local media", Toast.LENGTH_SHORT).show()
    }
    
    fun deleteDocument() {
        // TODO: Erase this file from the local file system
        // Always go back to gallery when deleting, regardless of source
        onNavigate("document_gallery")
    }
    
    fun copyAllExtractedInfo() {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val text = extractedInfo.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        val clip = android.content.ClipData.newPlainText("Extracted Information", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "All information copied to clipboard", Toast.LENGTH_SHORT).show()
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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar
        TopAppBar(
            title = { Text(document.name) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { exportDocument() }) {
                    Icon(Icons.Default.Download, contentDescription = "Export/Download")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Document Images with Navigation
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Main image display
                    if (imageUris.isNotEmpty()) {
                        // Image index display
                        Text(
                            text = "${currentImageIndex + 1}/${imageUris.size}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(8.dp)
                                .background(
                                    Color.White.copy(alpha = 0.8f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        
                        // Current image
                        AsyncImage(
                            model = imageUris[currentImageIndex],
                            contentDescription = "Document image ${currentImageIndex + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Navigation arrows
                        if (imageUris.size > 1) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
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
                    } else {
                        // Fallback when no images
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Gray.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "📄",
                                    fontSize = 64.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = document.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "No images available",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Document Type and Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                document.tags.forEach { tag ->
                    AssistChip(
                        onClick = { },
                        label = { Text(tag) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Document Summary
            Text(
                text = document.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Extracted Information Section with Help Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Extracted Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { showHelpDialog = true }
                ) {
                    Icon(
                        Icons.Default.Help,
                        contentDescription = "Help",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Row of action buttons as icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!parsing) {
                    IconButton(
                        onClick = {
                            // Save current info for possible restoration
                            previousExtractedInfo = extractedInfo
                            parsing = true
                            // Hide info list
                            extractedInfo = emptyMap()
                            // Start mock parsing job
                            // TODO: call doc parsing API
                            parsingJob = scope.launch {
                                delay(2000)
                                // After delay, show parsed info
                                extractedInfo = originalExtractedInfo
                                parsing = false
                                parsingJob = null
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Animation, contentDescription = "Parse")
                    }
                } else {
                    IconButton(
                        onClick = {
                            // Stop parsing, restore previous info
                            parsingJob?.cancel()
                            extractedInfo = previousExtractedInfo
                            parsing = false
                            parsingJob = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                    }
                }
                
                IconButton(
                    onClick = { isEditing = !isEditing },
                    enabled = extractedInfo.isNotEmpty() && !parsing,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditing) "Save" else "Edit"
                    )
                }
                
                IconButton(
                    onClick = { extractedInfo = emptyMap() },
                    enabled = extractedInfo.isNotEmpty() && !parsing,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
                
                IconButton(
                    onClick = { copyAllExtractedInfo() },
                    enabled = extractedInfo.isNotEmpty() && !parsing,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy All")
                }
            }
            
            // Show parsing message or info list
            if (parsing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("parsing document...", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
            } else if (extractedInfo.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        extractedInfo.forEach { (key, value) ->
                            ExtractedInfoItem(
                                key = key,
                                value = value,
                                isEditing = isEditing
                            )
                            if (key != extractedInfo.keys.last()) {
                                Divider(modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                // Show empty state when no extracted info
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No extracted information available. Use Parse to extract information from the document.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Related Files Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Related Files",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    relatedFiles.forEach { relatedDoc ->
                        RelatedFileItem(
                            document = relatedDoc,
                            onNavigate = onNavigate
                        )
                        if (relatedDoc != relatedFiles.last()) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

            // Upload Date at the bottom
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Upload Date: ${document.uploadDate}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Red delete button at the bottom
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Document", color = Color.White)
            }
            
            // Help dialog
            if (showHelpDialog) {
                AlertDialog(
                    onDismissRequest = { showHelpDialog = false },
                    title = { Text("Document Actions") },
                    text = {
                        Column {
                            HelpItem(
                                icon = Icons.Default.Animation,
                                title = "Parse",
                                description = "Extract information from the document image using AI-powered text recognition."
                            )
                            HelpItem(
                                icon = Icons.Default.Edit,
                                title = "Edit",
                                description = "Toggle edit mode to manually modify extracted information values."
                            )
                            HelpItem(
                                icon = Icons.Default.Clear,
                                title = "Clear",
                                description = "Remove all extracted information from the document."
                            )
                            HelpItem(
                                icon = Icons.Default.ContentCopy,
                                title = "Copy All",
                                description = "Copy all extracted information to clipboard in key-value format."
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showHelpDialog = false }) {
                            Text("Got it")
                        }
                    }
                )
            }
            
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Confirm Deletion") },
                    text = { Text("Are you sure you want to permanently delete this document?") },
                    confirmButton = {
                        TextButton(onClick = {
                            deleteDocument()
                            showDeleteDialog = false
                        }) {
                            Text("Delete", color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ExtractedInfoItem(
    key: String,
    value: String,
    isEditing: Boolean
) {
    var editedValue by remember { mutableStateOf(value) }
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Key column
        Text(
            text = key,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Value and copy icon column
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (isEditing) {
                OutlinedTextField(
                    value = editedValue,
                    onValueChange = { editedValue = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            } else {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Copy icon for individual values
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Value", value)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Value copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy value",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RelatedFileItem(
    document: cn.edu.sjtu.deepsleep.docusnap.data.Document,
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = document.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = document.uploadDate,
                fontSize = 10.sp,
                fontWeight = FontWeight.Light
            )
        }
        
        IconButton(
            onClick = { onNavigate("document_detail?documentId=${document.id}&fromImageProcessing=false") }
        ) {
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = "Open document",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun HelpItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
} 