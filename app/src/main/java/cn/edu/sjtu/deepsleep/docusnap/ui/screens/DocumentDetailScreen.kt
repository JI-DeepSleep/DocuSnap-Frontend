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
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.edu.sjtu.deepsleep.docusnap.ui.viewmodels.DocumentViewModel
import cn.edu.sjtu.deepsleep.docusnap.ui.viewmodels.DocumentViewModelFactory
import cn.edu.sjtu.deepsleep.docusnap.di.AppModule

@Composable
fun DocumentDetailScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit,
    documentId: String? = null,
    photoUris: String? = null,
    fromImageProcessing: Boolean = false // This was in MainActivity, let's add it here for consistency
) {
    val viewModel: DocumentViewModel = viewModel(
        factory = DocumentViewModelFactory(AppModule.provideDocumentRepository(LocalContext.current))
    )

    // State for loaded document
    var document by remember { mutableStateOf<cn.edu.sjtu.deepsleep.docusnap.data.Document?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Helper to persist extractedInfo changes to DB
    fun persistExtractedInfoUpdate(newExtractedInfo: Map<String, String>) {
        document = document?.copy(extractedInfo = newExtractedInfo)
        document?.let { updatedDoc ->
            scope.launch { viewModel.updateDocument(updatedDoc) }
        }
    }

    // Load document from DB (or fallback to MockData if not found)
    LaunchedEffect(documentId) {
        loading = true
        document = if (documentId != null) {
            viewModel.getDocument(documentId) ?: MockData.mockDocuments.find { it.id == documentId } ?: MockData.mockDocuments.first()
        } else {
            MockData.mockDocuments.first()
        }
        loading = false
    }

    // Save/update document on exit
    DisposableEffect(document, fromImageProcessing) {
        onDispose {
            document?.let { doc ->
                if (fromImageProcessing) {
                    scope.launch { viewModel.saveDocument(doc) }
                } else {
                    scope.launch { viewModel.updateDocument(doc) }
                }
            }
        }
    }

    if (loading || document == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val doc = document!!
    // Restore image navigation state and imagesToShow after doc is loaded
    var currentImageIndex by remember { mutableStateOf(0) }
    val imagesToShow = remember(photoUris, doc) {
        if (photoUris != null) {
            try {
                java.net.URLDecoder.decode(photoUris, "UTF-8").split(",").filter { it.isNotEmpty() }
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            doc.imageUris
        }
    }
    var isEditing by remember { mutableStateOf(false) }
    val originalExtractedInfo = remember(doc) { doc.extractedInfo.toMap() }
    var extractedInfo by remember { mutableStateOf(originalExtractedInfo) }
    // For editing, keep a separate map to hold edits until saved
    var editedExtractedInfo by remember { mutableStateOf(originalExtractedInfo) }
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var parsing by remember { mutableStateOf(false) }
    var parsingJob by remember { mutableStateOf<Job?>(null) }
    var previousExtractedInfo by remember { mutableStateOf(originalExtractedInfo) }
    // IMPORTANT: The imageUris variable is now replaced by imagesToShow
    // 重要：旧的 imageUris 变量现在被 imagesToShow 取代
    
    // Get related files using MockData helper functions
    val relatedFiles = remember(doc) {
        MockData.getRelatedFiles(doc.id)
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
        if (currentImageIndex < imagesToShow.size - 1) {
            currentImageIndex++
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar
        TopAppBar(
            title = { Text(doc.name) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        viewModel.exportDocuments(listOf(doc.id))
                        Toast.makeText(context, "Document images saved to local media", Toast.LENGTH_SHORT).show()
                    }) {
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
                    if (imagesToShow.isNotEmpty()) {
                        // Image index display
                        Text(
                            text = "${currentImageIndex + 1}/${imagesToShow.size}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(8.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        
                        // Current image
                        AsyncImage(
                            model = imagesToShow[currentImageIndex],
                            contentDescription = "Document image ${currentImageIndex + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Navigation arrows
                        if (imagesToShow.size > 1) {
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
                                    enabled = currentImageIndex < imagesToShow.size - 1,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            if (currentImageIndex < imagesToShow.size - 1) Color.White.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.5f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Next image",
                                        tint = if (currentImageIndex < imagesToShow.size - 1) Color.Black else Color.Gray
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
                                    text = doc.name,
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

            // Document Summary
            Text(
                text = doc.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Document Type and Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                doc.tags.forEach { tag ->
                    AssistChip(
                        onClick = { },
                        label = { Text(tag) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tool buttons row: Parse/Edit/Clear/Copy/Help
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Parse button
                if (!parsing) {
                    IconButton(
                        onClick = {
                            // Save current info for possible restoration
                            previousExtractedInfo = extractedInfo
                            parsing = true
                            // Hide info list
                            extractedInfo = emptyMap()
                            // Start mock parsing job
                            // TODO: BackendApiService.processDocument()
                            parsingJob = scope.launch {
                                delay(2000)
                                // After delay, show parsed info
                                extractedInfo = originalExtractedInfo
                                editedExtractedInfo = originalExtractedInfo
                                parsing = false
                                parsingJob = null
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = "Parse")
                    }
                } else {
                    IconButton(
                        onClick = {
                            // Stop parsing, restore previous info
                            parsingJob?.cancel()
                            extractedInfo = previousExtractedInfo
                            editedExtractedInfo = previousExtractedInfo
                            parsing = false
                            parsingJob = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                    }
                }
                
                // Edit button
                IconButton(
                    onClick = {
                        if (isEditing) {
                            // Save changes to database when finishing editing
                            extractedInfo = editedExtractedInfo
                            persistExtractedInfoUpdate(editedExtractedInfo)
                        } else {
                            // Entering edit mode, copy current extractedInfo
                            editedExtractedInfo = extractedInfo
                        }
                        isEditing = !isEditing
                    },
                    enabled = extractedInfo.isNotEmpty() && !parsing,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditing) "Save" else "Edit"
                    )
                }
                
                // Clear button
                IconButton(
                    onClick = {
                        extractedInfo = emptyMap()
                        editedExtractedInfo = emptyMap()
                        persistExtractedInfoUpdate(emptyMap())
                    },
                    enabled = extractedInfo.isNotEmpty() && !parsing,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear")
                }
                
                // Copy all button
                IconButton(
                    onClick = { copyAllExtractedInfo() },
                    enabled = extractedInfo.isNotEmpty() && !parsing,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy All")
                }

                // Help button
                IconButton(
                    onClick = { showHelpDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Help, contentDescription = "Help")
                }
            }

            // Show parsing message if parsing
            if (parsing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Parsing document...", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Extracted Information Section
            Text(
                text = "Extracted Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Show info list or empty state
            if ((if (isEditing) editedExtractedInfo else extractedInfo).isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        (if (isEditing) editedExtractedInfo else extractedInfo).forEach { (key, value) ->
                            ExtractedInfoItem(
                                key = key,
                                value = value,
                                isEditing = isEditing,
                                onValueChange = { newValue ->
                                    editedExtractedInfo = editedExtractedInfo.toMutableMap().apply { put(key, newValue) }
                                }
                            )
                            if (key != (if (isEditing) editedExtractedInfo else extractedInfo).keys.last()) {
                                Divider(modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else if (!parsing) {
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
                    relatedFiles.forEach { relatedFile ->
                        RelatedFileItem(
                            file = relatedFile,
                            onNavigate = onNavigate
                        )
                        if (relatedFile != relatedFiles.last()) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

            // Upload Date at the bottom
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Upload Date: ${doc.uploadDate}",
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
                                icon = Icons.Default.DocumentScanner,
                                title = "Parse",
                                description = "Extract information from the document image using AI-powered text recognition."
                            )
                            HelpItem(
                                icon = Icons.Default.Edit,
                                title = "Edit",
                                description = "Toggle edit mode to manually modify extracted information values."
                            )
                            HelpItem(
                                icon = Icons.Default.Delete,
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
                            scope.launch {
                                viewModel.deleteDocuments(listOf(doc.id))
                            }
                            showDeleteDialog = false
                            onNavigate("document_gallery")
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
    isEditing: Boolean,
    onValueChange: ((String) -> Unit)? = null
) {
    var editedValue by remember { mutableStateOf(value) }
    val context = LocalContext.current
    // Keep editedValue in sync with value prop
    LaunchedEffect(value) { if (!isEditing) editedValue = value }
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
                    onValueChange = {
                        editedValue = it
                        onValueChange?.invoke(it)
                    },
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
    file: Any,
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
                text = when (file) {
                    is cn.edu.sjtu.deepsleep.docusnap.data.Document -> file.name
                    is cn.edu.sjtu.deepsleep.docusnap.data.Form -> file.name
                    else -> "Unknown file"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = when (file) {
                    is cn.edu.sjtu.deepsleep.docusnap.data.Document -> file.uploadDate
                    is cn.edu.sjtu.deepsleep.docusnap.data.Form -> file.uploadDate
                    else -> ""
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Light
            )
        }
        
        IconButton(
            onClick = { 
                when (file) {
                    is cn.edu.sjtu.deepsleep.docusnap.data.Document -> onNavigate("document_detail?documentId=${file.id}&fromImageProcessing=false")
                    is cn.edu.sjtu.deepsleep.docusnap.data.Form -> onNavigate("form_detail?formId=${file.id}&fromImageProcessing=false")
                }
            }
        ) {
            Icon(
                Icons.Default.Link,
                contentDescription = "Open file",
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