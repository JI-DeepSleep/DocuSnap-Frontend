package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.edu.sjtu.deepsleep.docusnap.data.Form
import cn.edu.sjtu.deepsleep.docusnap.data.FormField
import cn.edu.sjtu.deepsleep.docusnap.data.local.AppDatabase
import cn.edu.sjtu.deepsleep.docusnap.data.local.JobEntity
import cn.edu.sjtu.deepsleep.docusnap.di.AppModule
import cn.edu.sjtu.deepsleep.docusnap.service.DeviceDBService
import cn.edu.sjtu.deepsleep.docusnap.service.JobPollingService
import cn.edu.sjtu.deepsleep.docusnap.ui.viewmodels.DocumentViewModel
import cn.edu.sjtu.deepsleep.docusnap.ui.viewmodels.DocumentViewModelFactory
import cn.edu.sjtu.deepsleep.docusnap.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.util.*
import kotlin.math.max
import kotlin.math.min

private const val TAG = "FormDetailScreen"

@Composable
private fun ZoomableImage(
    bitmap: Bitmap,
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
fun FormDetailScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit,
    formId: String? = null,
    fromImageProcessing: Boolean = false
) {
    val context = LocalContext.current
    val viewModel: DocumentViewModel = viewModel(
        factory = DocumentViewModelFactory(AppModule.provideDocumentRepository(context))
    )
    val jobPollingService = remember { JobPollingService(context) }
    val deviceDBService = remember { DeviceDBService(context) }
    val database = remember { AppDatabase.getInstance(context) }
    val jobDao = remember { database.jobDao() }
    val coroutineScope = rememberCoroutineScope()

    // State for loaded form
    var form by remember { mutableStateOf<Form?>(null) }
    var loading by remember { mutableStateOf(true) }
    var job by remember { mutableStateOf<JobEntity?>(null) }
    var jobStatus by remember { mutableStateOf<String?>(null) }
    var jobError by remember { mutableStateOf<String?>(null) }
    var processing by remember { mutableStateOf(false) }

    // Observe job status changes
    LaunchedEffect(form?.jobId) {
        form?.jobId?.let { jobId ->
            jobDao.getJobById(jobId).collectLatest { jobEntity: JobEntity? ->
                job = jobEntity
                jobEntity?.let {
                    jobStatus = it.status
                    jobError = it.errorDetail
                    processing = it.status == "pending" || it.status == "processing"

                    if (it.status == "completed" && it.result != null) {
                        try {
                            val decryptedResult = jobPollingService.decryptJobResult(it.result, it)
                            decryptedResult?.let { resultJson ->
                                val result = JSONObject(resultJson)

                                // Extract ALL fields from the result
                                val title = result.optString("title", form?.name ?: "")
                                val tagsArray = result.optJSONArray("tags")
                                val tags = if (tagsArray != null) {
                                    mutableListOf<String>().apply {
                                        for (i in 0 until tagsArray.length()) {
                                            add(tagsArray.getString(i))
                                        }
                                    }
                                } else {
                                    form?.tags ?: emptyList()
                                }
                                val description = result.optString("description", form?.description ?: "")

                                val extractedInfoJson = result.optJSONObject("extractedInfo") ?: result.optJSONObject("kv")
                                val extractedInfo = mutableMapOf<String, String>()
                                extractedInfoJson?.keys()?.forEach { key ->
                                    extractedInfo[key] = extractedInfoJson.getString(key)
                                }

                                val formFieldsJson = result.optJSONArray("formFields") ?: result.optJSONArray("fields")
                                val formFields = if (formFieldsJson != null) {
                                    val fieldNames = mutableListOf<String>()
                                    for (i in 0 until formFieldsJson.length()) {
                                        fieldNames.add(formFieldsJson.getString(i))
                                    }
                                    fieldNames.map { name -> FormField(name, null, false) }
                                } else {
                                    emptyList()
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
                                            deviceDBService.addRelatedFile(form?.id ?: "", resourceId)
                                        }
                                    }
                                }

                                // Update form with ALL fields
                                form = form?.copy(
                                    name = title,
                                    tags = tags,
                                    description = description,
                                    formFields = formFields,
                                    extractedInfo = extractedInfo,
                                    isProcessed = true,
                                    relatedFileIds = relatedIds
                                )

                                // Save to DB
                                form?.let { updatedForm ->
                                    viewModel.updateForm(updatedForm)
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

    // Load form from DB with retry mechanism
    LaunchedEffect(formId) {
        loading = true
        if (formId != null) {
            val startTime = System.currentTimeMillis()
            val timeout = 3000L // 3 seconds timeout
            while (System.currentTimeMillis() - startTime < timeout) {
                val loadedForm = viewModel.getForm(formId)
                if (loadedForm != null) {
                    form = loadedForm
                    loading = false
                    return@LaunchedEffect
                }
                delay(200) // Wait 200ms before retrying
            }
            // Timeout reached without finding form
            Toast.makeText(context, "Form Not Found", Toast.LENGTH_SHORT).show()
            delay(1000)
            onBackClick()
        }
        loading = false
    }

    // Save/update form on exit
    DisposableEffect(form, fromImageProcessing) {
        onDispose {
            form?.let { frm ->
                if (fromImageProcessing) {
                    coroutineScope.launch { viewModel.saveForm(frm) }
                } else {
                    coroutineScope.launch { viewModel.updateForm(frm) }
                }
            }
        }
    }

    if (loading || form == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentForm = form!!

    // Convert base64 images to bitmaps
    val imagesToShow = remember(currentForm) {
        currentForm.imageBase64s.mapNotNull { base64 ->
            try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                Log.e(TAG, "Bitmap decoding failed", e)
                null
            }
        }
    }

    var isEditing by remember { mutableStateOf(false) }
    var autoFilling by remember { mutableStateOf(false) }
    var autoFillingJob by remember { mutableStateOf<Job?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var currentImageIndex by remember { mutableStateOf(0) }

    // For editing, keep separate state
    var editedFormFields by remember { mutableStateOf(currentForm.formFields) }
    var editedExtractedInfo by remember { mutableStateOf(currentForm.extractedInfo) }

    // Related files
    var relatedFiles by remember { mutableStateOf<List<Pair<String, String?>>>(emptyList()) }
    LaunchedEffect(currentForm.relatedFileIds) {
        val files = mutableListOf<Pair<String, String?>>()
        currentForm.relatedFileIds.forEach { id ->
            val name = withContext(Dispatchers.IO) {
                deviceDBService.getFileNameById(id)
            }
            files.add(Pair(id, name))
        }
        relatedFiles = files
    }

    // Navigation functions
    fun goToPreviousImage() {
        if (currentImageIndex > 0) currentImageIndex--
    }

    fun goToNextImage() {
        if (currentImageIndex < imagesToShow.size - 1) currentImageIndex++
    }

    // Helper to persist form changes
    fun persistFormUpdate(updatedForm: Form) {
        form = updatedForm
        coroutineScope.launch { viewModel.updateForm(updatedForm) }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar
        TopAppBar(
            title = { Text(currentForm.name) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        viewModel.exportForms(listOf(currentForm.id))
                        Toast.makeText(context, "Form images saved to local media", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Export")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Form Images with Navigation
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
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

                        // Current image with zoom
                        ZoomableImage(
                            bitmap = imagesToShow[currentImageIndex],
                            contentDescription = "Form image ${currentImageIndex + 1}",
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
                                    text = "📋",
                                    fontSize = 48.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentForm.name,
                                    fontSize = 16.sp,
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

            // Form Summary
            Text(
                text = currentForm.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Form Tags
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentForm.tags.forEach { tag ->
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
                    text= "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Tool buttons row
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
                                    val base64Images = currentForm.imageBase64s
                                    val job = jobPollingService.createJob(
                                        type = "form",
                                        id = currentForm.id,
                                        payload = base64Images
                                    )
                                    form = currentForm.copy(jobId = job.id)
                                    viewModel.updateForm(form!!)
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

                // Autofill button
                if (!autoFilling) {
                    IconButton(
                        onClick = {
                            autoFilling = true
                            autoFillingJob = coroutineScope.launch {
                                try {
                                    // Create autofill payload according to backend spec
                                    val autofillPayload = mapOf(
                                        "id" to currentForm.id,
                                        "fields" to currentForm.formFields.map { it.name }
                                    )

                                    // Create autofill job
                                    val job = jobPollingService.createJob(
                                        type = "fill",
                                        id = currentForm.id,
                                        payload = autofillPayload
                                    )

                                    // Store job ID to monitor its status
                                    val jobId = job.id

                                    // Observe job status changes
                                    jobDao.getJobById(jobId).collect { jobEntity: JobEntity? ->
                                        jobEntity?.let {
                                            when (it.status) {
                                                "completed" -> {
                                                    if (it.result != null) {
                                                        try {
                                                            // Decrypt and process result
                                                            val decryptedResult = jobPollingService.decryptJobResult(it.result, it)
                                                            decryptedResult?.let { resultJson ->
                                                                val result = JSONObject(resultJson)

                                                                // Update form fields with autofilled values
                                                                val updatedFormFields = currentForm.formFields.map { field ->
                                                                    if (result.has(field.name)) {
                                                                        val fieldData = result.getJSONObject(field.name)
                                                                        field.copy(
                                                                            value = fieldData.getString("value"),
                                                                            isRetrieved = true,
                                                                            srcFileId = fieldData.getJSONObject("source").getString("resource_id")
                                                                        )
                                                                    } else {
                                                                        field
                                                                    }
                                                                }

                                                                // Update form
                                                                editedFormFields = updatedFormFields
                                                                persistFormUpdate(currentForm.copy(formFields = updatedFormFields))
                                                            }
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Autofill result parsing failed", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                    autoFilling = false
                                                    autoFillingJob = null
                                                }
                                                "error" -> {
                                                    jobError = it.errorDetail ?: "Autofill failed"
                                                    autoFilling = false
                                                    autoFillingJob = null
                                                }
                                                // Continue processing for other statuses
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    jobError = "Autofill job creation failed: ${e.message}"
                                    autoFilling = false
                                    autoFillingJob = null
                                }
                            }
                        },
                        enabled = currentForm.formFields.isNotEmpty() && !processing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Autofill")
                    }
                } else {
                    IconButton(
                        onClick = {
                            autoFillingJob?.cancel()
                            autoFilling = false
                            autoFillingJob = null
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
                            // Save changes
                            persistFormUpdate(currentForm.copy(
                                extractedInfo = editedExtractedInfo,
                                formFields = editedFormFields
                            ))
                        } else {
                            // Enter edit mode
                            editedExtractedInfo = currentForm.extractedInfo
                            editedFormFields = currentForm.formFields
                        }
                        isEditing = !isEditing
                    },
                    enabled = (currentForm.extractedInfo.isNotEmpty() || currentForm.formFields.isNotEmpty()) && !processing && !autoFilling,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditing) "Save" else "Edit"
                    )
                }

                // Clear Form button
                IconButton(
                    onClick = {
                        val clearedFormFields = currentForm.formFields.map { field ->
                            field.copy(value = null, isRetrieved = false)
                        }
                        editedFormFields = clearedFormFields
                        persistFormUpdate(currentForm.copy(formFields = clearedFormFields))
                    },
                    enabled = currentForm.formFields.isNotEmpty() && !processing && !autoFilling,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ClearAll, contentDescription = "Clear Form")
                }

                // Clear All button
                IconButton(
                    onClick = {
                        editedFormFields = emptyList()
                        editedExtractedInfo = emptyMap()
                        persistFormUpdate(currentForm.copy(
                            extractedInfo = emptyMap(),
                            formFields = emptyList()
                        ))
                    },
                    enabled = (currentForm.extractedInfo.isNotEmpty() || currentForm.formFields.isNotEmpty()) && !processing && !autoFilling,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear All")
                }

                // Copy button
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val extractedText = currentForm.extractedInfo.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                        val formFieldsText = currentForm.formFields.joinToString("\n") { "${it.name}: ${it.value ?: ""}" }
                        val allText = if (extractedText.isNotEmpty() && formFieldsText.isNotEmpty()) {
                            "Extracted Information:\n$extractedText\n\nForm Fields:\n$formFieldsText"
                        } else if (extractedText.isNotEmpty()) {
                            "Extracted Information:\n$extractedText"
                        } else if (formFieldsText.isNotEmpty()) {
                            "Form Fields:\n$formFieldsText"
                        } else {
                            "No information available"
                        }
                        val clip = android.content.ClipData.newPlainText("Form Information", allText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "All information copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    enabled = (currentForm.extractedInfo.isNotEmpty() || currentForm.formFields.isNotEmpty()) && !processing && !autoFilling,
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

            // Show processing indicators
//            if (processing) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(60.dp),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text("Processing form...", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
//                }
//                Spacer(modifier = Modifier.height(8.dp))
//            }

            if (autoFilling) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Auto-filling form...", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Extracted Information Section
            if ((if (isEditing) editedExtractedInfo else currentForm.extractedInfo).isNotEmpty()) {
                Text(
                    text = "Extracted Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        (if (isEditing) editedExtractedInfo else currentForm.extractedInfo).forEach { (key, value) ->
                            ExtractedInfoItem(
                                key = key,
                                value = value,
                                isEditing = isEditing,
                                onValueChange = { newValue ->
                                    editedExtractedInfo = editedExtractedInfo.toMutableMap().apply { put(key, newValue) }
                                }
                            )
                            if (key != (if (isEditing) editedExtractedInfo else currentForm.extractedInfo).keys.last()) {
                                Divider(modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Form Fields Section
            if ((if (isEditing) editedFormFields else currentForm.formFields).isNotEmpty() && !autoFilling) {
                Text(
                    text = "Form Fields",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        (if (isEditing) editedFormFields else currentForm.formFields).forEach { field ->
                            FormFieldDisplayItem(
                                field = field,
                                isEditing = isEditing,
                                onNavigate = onNavigate,
                                onValueChange = { newValue ->
                                    editedFormFields = editedFormFields.map {
                                        if (it.name == field.name) it.copy(value = newValue) else it
                                    }
                                }
                            )
                            if (field != (if (isEditing) editedFormFields else currentForm.formFields).last()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            } else if (!processing && !autoFilling && currentForm.extractedInfo.isEmpty() && currentForm.formFields.isEmpty()) {
                // Empty state
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No information available",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Use Parse to extract information and form fields from the document",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
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
                    if (relatedFiles.isEmpty()) {
                        Text(
                            "No related files found",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        relatedFiles.forEachIndexed { index, (id, name) ->
                            if (name != null) {
                                RelatedFileItem(
                                    id = id,
                                    name = name,
                                    onNavigate = { fileId ->
                                        FileUtils.navigateToFileDetail(context, onNavigate, fileId)
                                    }
                                )
                                if (index < relatedFiles.lastIndex && relatedFiles[index + 1].second != null) {
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Upload Date
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Upload Date: ${currentForm.uploadDate}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Delete button
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
                Text("Delete Form", color = Color.White)
            }
        }
    }

    // Help dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Form Actions") },
            text = {
                Column {
                    HelpItem(
                        icon = Icons.Default.DocumentScanner,
                        title = "Parse",
                        description = "Extract both information and field names from the form image."
                    )
                    HelpItem(
                        icon = Icons.Default.AutoAwesome,
                        title = "Autofill",
                        description = "Automatically fill form fields using extracted information or related documents."
                    )
                    HelpItem(
                        icon = Icons.Default.Edit,
                        title = "Edit",
                        description = "Toggle edit mode to manually modify extracted information and field values."
                    )
                    HelpItem(
                        icon = Icons.Default.ClearAll,
                        title = "Clear Form",
                        description = "Clear all form field values while keeping the field names intact."
                    )
                    HelpItem(
                        icon = Icons.Default.Delete,
                        title = "Clear All",
                        description = "Remove all extracted information and form fields completely."
                    )
                    HelpItem(
                        icon = Icons.Default.ContentCopy,
                        title = "Copy All",
                        description = "Copy all extracted information and form fields to clipboard."
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

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to permanently delete this form?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.deleteForms(listOf(currentForm.id))
                        }
                        onNavigate("form_gallery")
                        showDeleteDialog = false
                    }
                ) {
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

@Composable
private fun FormFieldDisplayItem(
    field: FormField,
    isEditing: Boolean,
    onNavigate: (String) -> Unit,
    onValueChange: ((String) -> Unit)? = null
) {
    var value by remember { mutableStateOf(field.value ?: "") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Keep value in sync with field.value prop
    LaunchedEffect(field.value) { if (!isEditing) value = field.value ?: "" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = field.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (field.value == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                // Show link to source file if srcFileId is present
                if (field.srcFileId != null) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                FileUtils.navigateToFileDetail(context, onNavigate, field.srcFileId)
                            }
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "Go to source file",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Copy icon for retrieved fields
                if (field.isRetrieved && field.value != null) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Field Value", field.value)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Value copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(20.dp)
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

            if (isEditing) {
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        onValueChange?.invoke(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else if (field.value != null) {
                Text(
                    text = field.value,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "No value available",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
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
    if (name == null) return
    val context = LocalContext.current
    var uploadTime by remember { mutableStateOf("Loading...") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(id) {
        uploadTime = FileUtils.getFormattedTimeForFile(context, id) ?: "Unknown"
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
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
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