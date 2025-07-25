package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.edu.sjtu.deepsleep.docusnap.viewmodels.DocumentViewModel
import cn.edu.sjtu.deepsleep.docusnap.viewmodels.DocumentViewModelFactory
import cn.edu.sjtu.deepsleep.docusnap.AppModule
import cn.edu.sjtu.deepsleep.docusnap.service.JobPollingService
import cn.edu.sjtu.deepsleep.docusnap.data.local.AppDatabase
import cn.edu.sjtu.deepsleep.docusnap.util.FileUtils
import android.util.Base64
import cn.edu.sjtu.deepsleep.docusnap.data.local.JobEntity
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONObject
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import cn.edu.sjtu.deepsleep.docusnap.data.model.Document
import cn.edu.sjtu.deepsleep.docusnap.data.model.ExtractedInfoItem
import cn.edu.sjtu.deepsleep.docusnap.data.model.FileType
import cn.edu.sjtu.deepsleep.docusnap.service.DeviceDBService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "DocumentDetailScreen"

@Composable
private fun ZoomableImage(
    bitmap: android.graphics.Bitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f..3f)
        offset += offsetChange
    }

    Box(
        modifier = modifier
            .onSizeChanged { size = it }
            .transformable(state = state)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = contentScale
        )
    }
}

@Composable
fun DocumentDetailScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit,
    documentId: String? = null,
    fromImageProcessing: Boolean = false,
    documentViewModel: DocumentViewModel? = null
) {
    val context = LocalContext.current
    val viewModel: DocumentViewModel = viewModel(
        factory = DocumentViewModelFactory(AppModule.provideDocumentRepository(context))
    )
    val jobPollingService = remember { JobPollingService(context) }
    val database = remember { AppDatabase.getInstance(context) }
    val deviceDBService = remember { DeviceDBService(context) }
    val jobDao = remember { database.jobDao() }
    val coroutineScope = rememberCoroutineScope()

    // State for loaded document
    var document by remember { mutableStateOf<Document?>(null) }
    var loading by remember { mutableStateOf(true) }
    var job by remember { mutableStateOf<JobEntity?>(null) }
    var jobStatus by remember { mutableStateOf<String?>(null) }
    var jobError by remember { mutableStateOf<String?>(null) }
    var processing by remember { mutableStateOf(false) }

    // Observe job status changes
    LaunchedEffect(document?.jobId) {
        document?.jobId?.let { jobId ->
            jobDao.getJobById(jobId).collectLatest { jobEntity: JobEntity? ->
                job = jobEntity
                jobEntity?.let {
                    jobStatus = it.status
                    jobError = it.errorDetail

                    // Sync processing state with actual job status
                    processing = it.status == "pending" || it.status == "processing"

                    if (it.status == "completed" && it.result != null) {
                        try {
                            val decryptedResult = jobPollingService.decryptJobResult(it.result, it)
                            decryptedResult?.let { resultJson ->
                                val result = JSONObject(resultJson)

                                // Extract ALL fields from the result
                                val title = result.optString("title", document?.name ?: "")
                                val tagsArray = result.optJSONArray("tags")
                                val tags = if (tagsArray != null) {
                                    mutableListOf<String>().apply {
                                        for (i in 0 until tagsArray.length()) {
                                            add(tagsArray.getString(i))
                                        }
                                    }
                                } else {
                                    document?.tags ?: emptyList()
                                }
                                val description = result.optString("description", document?.description ?: "")
                                val kv = result.optJSONObject("kv") ?: JSONObject()

                                // Convert kv to list of ExtractedInfoItem
                                val extractedInfoList = mutableListOf<ExtractedInfoItem>()
                                kv.keys().forEach { key ->
                                    extractedInfoList.add(
                                        ExtractedInfoItem(
                                            key = key,
                                            value = kv.getString(key),
                                            usageCount = 0,
                                            lastUsed = System.currentTimeMillis().toString()
                                        )
                                    )
                                }

                                // Handle related files
                                val relatedArray = result.optJSONArray("related")
                                val relatedIds = mutableListOf<String>()
                                if (relatedArray != null) {
                                    for (i in 0 until relatedArray.length()) {
                                        val relatedObj = relatedArray.getJSONObject(i)
                                        val resourceId = relatedObj.getString("resource_id")
                                        relatedIds.add(resourceId)
                                        // Add to both sides of relationship
                                        withContext(Dispatchers.IO) {
                                            deviceDBService.addRelatedFile(document?.id ?: "", resourceId)
                                        }
                                    }
                                }
                                // Update document with ALL fields
                                document = document?.copy(
                                    name = title,
                                    tags = tags,
                                    description = description,
                                    extractedInfo = extractedInfoList,
                                    isProcessed = true,
                                    relatedFileIds = relatedIds // Also update the relatedFileIds
                                )

                                // Save to DB
                                document?.let { updatedDoc ->
                                    coroutineScope.launch { viewModel.updateDocument(updatedDoc) }
                                }
                            }
                        } catch (e: Exception) {
                            jobError = "Result parsing failed: ${e.message}"
                        }
                    }
                }
            }
        }
    }

    // Load document from DB with retry mechanism and 3-second timeout
    LaunchedEffect(documentId) {
        loading = true
        if (documentId != null) {
            val startTime = System.currentTimeMillis()
            val timeout = 3000L // 3 seconds timeout

            while (System.currentTimeMillis() - startTime < timeout) {
                val loadedDoc = viewModel.getDocument(documentId)
                if (loadedDoc != null) {
                    document = loadedDoc
                    loading = false
                    return@LaunchedEffect
                }
                delay(200) // Wait 200ms before retrying
            }

            // If we get here, the timeout was reached without finding the document
            Toast.makeText(context, "File Not Found", Toast.LENGTH_SHORT).show()
            delay(1000) // Show the toast for 1 second
            onBackClick() // Navigate back
        }
        loading = false
    }

    // In DocumentDetailScreen's LaunchedEffect
    LaunchedEffect(document) {
        document?.let { doc ->
            // Only update usage if not already updated
                documentViewModel?.updateDocumentUsage(doc.id)
        }
    }

    // Save/update document on exit
    DisposableEffect(fromImageProcessing) {
        onDispose {
            document?.let { doc ->
                if (fromImageProcessing) {
                    coroutineScope.launch { viewModel.saveDocument(doc) }
                } else {
                    coroutineScope.launch { viewModel.updateDocument(doc) }
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
    // In DocumentDetailScreen
    val imagesToShow = remember(doc) {
        doc.imageBase64s.mapNotNull { base64 ->
            try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                Log.e(TAG, "Bitmap decoding failed", e)
                null
            }
        }.also {
            Log.d(TAG, "Decoded ${it.size} bitmaps from document ${doc.id}")
        }
    }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // For editing, keep a separate list to hold edits until saved
    var editedExtractedInfo by remember { mutableStateOf(doc.extractedInfo) }

    // Helper to persist extractedInfo changes to DB
    fun persistExtractedInfoUpdate(newExtractedInfo: List<ExtractedInfoItem>) {
        document = document?.copy(extractedInfo = newExtractedInfo)
        document?.let { updatedDoc ->
            coroutineScope.launch { viewModel.updateDocument(updatedDoc) }
        }
    }

    // Related Files Section
    var relatedFiles by remember { mutableStateOf<List<Pair<String, String?>>>(emptyList()) }
    LaunchedEffect(doc.relatedFileIds) {
        val files = mutableListOf<Pair<String, String?>>()
        doc.relatedFileIds.forEach { id ->
            val name = withContext(Dispatchers.IO) {
                FileUtils.getFileNameById(context, id) // Now FileUtils is available
            }
            files.add(Pair(id, name))
        }
        relatedFiles = files
    }


    fun copyAllExtractedInfo() {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val text = doc.extractedInfo.joinToString("\n") { "${it.key}: ${it.value}" }
        val clip = android.content.ClipData.newPlainText("Extracted Information", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "All information copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    // Navigation functions
    fun goToPreviousImage() {
        if (currentImageIndex > 0) currentImageIndex--
    }

    fun goToNextImage() {
        if (currentImageIndex < imagesToShow.size - 1) currentImageIndex++
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

                        // Current image with zoom functionality
                        ZoomableImage(
                            bitmap = imagesToShow[currentImageIndex],
                            contentDescription = "Document image ${currentImageIndex + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
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
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                doc.tags.forEach { tag ->
                    AssistChip(
                        onClick = { },
                        label = { Text(tag) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Job status display
            jobStatus?.let { status ->
                val statusColor = when (status) {
                    "processing" -> MaterialTheme.colorScheme.primary
                    "completed" -> MaterialTheme.colorScheme.secondary
                    "error" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Text(
                    text = "Processing status: ${status.uppercase()}",
                    color = statusColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            jobError?.let { error ->
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Tool buttons row: Parse/Edit/Clear/Copy/Help
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Parse button
                if (!processing) {
                    IconButton(
                        onClick = {
                            processing = true
                            jobStatus = null
                            jobError = null
                            coroutineScope.launch {
                                try {
                                    // Use the original Base64 strings from the document
                                    val base64Images = doc.imageBase64s

                                    // Create processing job
                                    val job = jobPollingService.createJob(
                                        type = "doc",
                                        id=doc.id,
                                        payload = base64Images
                                    )

                                    // Update document with job ID
                                    document = doc.copy(jobId = job.id)
                                    viewModel.updateDocument(document!!)
                                } catch (e: Exception) {
                                    jobError = "Job creation failed: ${e.message}"
                                    processing = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = "Parse")
                    }
                } else {
                    IconButton(
                        onClick = { processing = false },
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
                            persistExtractedInfoUpdate(editedExtractedInfo)
                        } else {
                            // Entering edit mode, copy current extractedInfo
                            editedExtractedInfo = doc.extractedInfo
                        }
                        isEditing = !isEditing
                    },
                    enabled = doc.extractedInfo.isNotEmpty() && !processing,
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
                        editedExtractedInfo = emptyList()
                        persistExtractedInfoUpdate(emptyList())
                    },
                    enabled = doc.extractedInfo.isNotEmpty() && !processing,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear")
                }

                // Copy all button
                IconButton(
                    onClick = { copyAllExtractedInfo() },
                    enabled = doc.extractedInfo.isNotEmpty() && !processing,
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

            // Show processing message if processing
//            if (processing) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(60.dp),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text("Processing document...", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
//                }
//                Spacer(modifier = Modifier.height(8.dp))
//            }

            // Extracted Information Section
            Text(
                text = "Extracted Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Show info list or empty state
            if (doc.extractedInfo.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        (if (isEditing) editedExtractedInfo else doc.extractedInfo).forEach { item ->
                            ExtractedInfoItem(
                                key = item.key,
                                value = if (isEditing) editedExtractedInfo.find { it.key == item.key }?.value ?: item.value else item.value,
                                isEditing = isEditing,
                                onValueChange = { newValue ->
                                    editedExtractedInfo = editedExtractedInfo.map { 
                                        if (it.key == item.key) it.copy(value = newValue) else it 
                                    }
                                },
                                onCopyText = {
                                    documentViewModel?.updateExtractedInfoUsage(
                                        fileId = doc.id,
                                        fileType = FileType.DOCUMENT,
                                        key = item.key
                                    )
                                }
                            )
                            if (item != (if (isEditing) editedExtractedInfo else doc.extractedInfo).last()) {
                                Divider(modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else if (!processing) {
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
                    // In the Related Files Section of DocumentDetailScreen
                    if (relatedFiles.isEmpty()) {
                        Text(
                            "No related files found",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        relatedFiles.forEachIndexed { index, (id, name) ->
                            if (name != null) { // Only show files with known names
                                RelatedFileItem(
                                    id = id,
                                    name = name,
                                    onNavigate = { fileId ->
                                        FileUtils.navigateToFileDetail(context, onNavigate, fileId)
                                    }
                                )
                                // Only add divider if:
                                // 1. Not the last item AND
                                // 2. The next item is not null
                                if (index < relatedFiles.lastIndex &&
                                    relatedFiles[index + 1].second != null) {
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
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
                            coroutineScope.launch {
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
    onValueChange: ((String) -> Unit)? = null,
    onCopyText: (() -> Unit)? = null
) {
    var editedValue by remember { mutableStateOf(value) }
    val context = LocalContext.current

    // Keep editedValue in sync with value prop
    LaunchedEffect(value) {
        if (!isEditing) editedValue = value
    }

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
                    // Call the update function if provided
                    onCopyText?.invoke()
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
    id: String,
    name: String?,
    onNavigate: suspend (String) -> Unit
) {
    if (name == null) return // Skip rendering if name is null

    val context = LocalContext.current
    var uploadTime by remember { mutableStateOf("Loading...") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(id) {
        uploadTime = FileUtils.getFormattedTimeForFile(context, id)
    }

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
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Uploaded: $uploadTime",
                fontSize = 10.sp,
                fontWeight = FontWeight.Light
            )
        }
        IconButton(onClick = {
            coroutineScope.launch {
                onNavigate(id)
            }
        }) {
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