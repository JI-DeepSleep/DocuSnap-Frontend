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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.edu.sjtu.deepsleep.docusnap.data.MockData
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import android.widget.Toast
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.edu.sjtu.deepsleep.docusnap.ui.viewmodels.DocumentViewModel
import cn.edu.sjtu.deepsleep.docusnap.ui.viewmodels.DocumentViewModelFactory
import cn.edu.sjtu.deepsleep.docusnap.di.AppModule
import cn.edu.sjtu.deepsleep.docusnap.service.JobPollingService
import cn.edu.sjtu.deepsleep.docusnap.data.local.AppDatabase
import android.util.Base64
import cn.edu.sjtu.deepsleep.docusnap.data.local.JobEntity
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONObject
import android.util.Log
import cn.edu.sjtu.deepsleep.docusnap.data.FormField
import kotlinx.serialization.json.Json

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
    val database = remember { AppDatabase.getInstance(context) }
    val jobDao = remember { database.jobDao() }
    val coroutineScope = rememberCoroutineScope()

    // State for loaded form
    var form by remember { mutableStateOf<cn.edu.sjtu.deepsleep.docusnap.data.Form?>(null) }
    var loading by remember { mutableStateOf(true) }
    var job by remember { mutableStateOf<JobEntity?>(null) }
    var jobStatus by remember { mutableStateOf<String?>(null) }
    var jobError by remember { mutableStateOf<String?>(null) }

    // Observe job status changes
    LaunchedEffect(form?.jobId) {
        form?.jobId?.let { jobId ->
            jobDao.getJobById(jobId).collectLatest { jobEntity: JobEntity? ->
                job = jobEntity
                jobEntity?.let {
                    jobStatus = it.status
                    jobError = it.errorDetail

                    if (it.status == "completed" && it.result != null) {
                        try {
                            // Decrypt result
                            val decryptedResult = jobPollingService.decryptJobResult(it.result, it)
                            decryptedResult?.let { resultJson ->
                                val result = JSONObject(resultJson)
                                val formFieldsJson = result.optJSONArray("formFields") ?: result.optJSONArray("fields")
                                val extractedInfoJson = result.optJSONObject("extractedInfo") ?: result.optJSONObject("kv")

                                // Convert form fields
                                val formFields = if (formFieldsJson != null) {
                                    Json.decodeFromString<List<FormField>>(formFieldsJson.toString())
                                } else {
                                    emptyList()
                                }

                                // Convert extracted info
                                val extractedInfo = mutableMapOf<String, String>()
                                extractedInfoJson?.keys()?.forEach { key ->
                                    extractedInfo[key] = extractedInfoJson.getString(key)
                                }

                                // Update form
                                form = form?.copy(
                                    formFields = formFields,
                                    extractedInfo = extractedInfo,
                                    isProcessed = true
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

    // Load form from DB
    LaunchedEffect(formId) {
        loading = true
        form = if (formId != null) {
            viewModel.getForm(formId) ?: MockData.mockForms.find { it.id == formId } ?: MockData.mockForms.first()
        } else {
            MockData.mockForms.first()
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

    // Images to show from Base64 strings in the form
    val imagesToShow = remember(currentForm) {
        currentForm.imageBase64s.map { base64 ->
            "data:image/jpeg;base64,$base64"
        }
    }

    var isEditing by remember { mutableStateOf(false) }
    var parsing by remember { mutableStateOf(false) }
    var parsingJob by remember { mutableStateOf<Job?>(null) }
    var autoFilling by remember { mutableStateOf(false) }
    var autoFillingJob by remember { mutableStateOf<Job?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    var currentImageIndex by remember { mutableStateOf(0) }

    // Store original form fields for restoration during parsing
    val originalFormFields = remember { currentForm.formFields }

    // Create mutable state for form fields
    var formFields by remember { mutableStateOf(currentForm.formFields) }
    var extractedInfo by remember { mutableStateOf(currentForm.extractedInfo) }

    // For editing, keep separate maps to hold edits until saved
    var editedFormFields by remember { mutableStateOf(currentForm.formFields) }
    var editedExtractedInfo by remember { mutableStateOf(currentForm.extractedInfo) }

    // Get related files using MockData helper functions
    val relatedFiles = remember(currentForm) {
        MockData.getRelatedFiles(currentForm.id)
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

    // Helper to persist form changes to DB
    fun persistFormUpdate(updatedForm: cn.edu.sjtu.deepsleep.docusnap.data.Form) {
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
                    .height(200.dp)
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
                            contentDescription = "Form image ${currentImageIndex + 1}",
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Tool buttons row: Parse/Autofill/Edit/Clear Form/Clear All/Copy/Help
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Parse button (parse both extracted info and form fields)
                if (!parsing) {
                    IconButton(
                        onClick = {
                            parsing = true
                            // Clear both extracted info and form fields
                            extractedInfo = emptyMap()
                            formFields = emptyList()
                            editedExtractedInfo = emptyMap()
                            editedFormFields = emptyList()

                            parsingJob = coroutineScope.launch {
                                try {
                                    // Convert images to base64
                                    val base64Images = imagesToShow.map { uri ->
                                        uri.substringAfter("base64,")
                                    }

                                    // Create processing job
                                    val job = jobPollingService.createJob(
                                        type = "form",
                                        payload = base64Images
                                    )

                                    // Update form with job ID
                                    form = currentForm.copy(jobId = job.id)
                                    viewModel.updateForm(form!!)
                                } catch (e: Exception) {
                                    jobError = "Job creation failed: ${e.message}"
                                    parsing = false
                                }
                            }
                        },
                        enabled = !autoFilling,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = "Parse")
                    }
                } else {
                    IconButton(
                        onClick = {
                            parsingJob?.cancel()
                            parsing = false
                            parsingJob = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                    }
                }

                // Autofill button (auto fill form fields)
                if (!autoFilling) {
                    IconButton(
                        onClick = {
                            autoFilling = true
                            autoFillingJob = coroutineScope.launch {
                                // TODO: BackendApiService.fillForm()
                                delay(2000) // Simulate auto-filling process
                                // After auto-filling, update form fields with realistic values
                                val updatedFormFields = formFields.map { field ->
                                    when (field.name.lowercase()) {
                                        "employee name" -> field.copy(value = "John Doe", isRetrieved = true)
                                        "department" -> field.copy(value = "Engineering", isRetrieved = true)
                                        "date" -> field.copy(value = "2024-01-15", isRetrieved = true)
                                        "amount" -> field.copy(value = "$12.50", isRetrieved = true)
                                        "full name" -> field.copy(value = "John Doe", isRetrieved = true)
                                        "date of birth" -> field.copy(value = "1990-05-15", isRetrieved = true)
                                        "passport number" -> field.copy(value = "A12345678", isRetrieved = true)
                                        "destination" -> field.copy(value = "Japan", isRetrieved = true)
                                        "duration" -> field.copy(value = "2 weeks", isRetrieved = true)
                                        else -> field.copy(value = null, isRetrieved = false) // Keep unavailable fields as null
                                    }
                                }
                                formFields = updatedFormFields
                                editedFormFields = updatedFormFields
                                // Persist the auto-filled results
                                persistFormUpdate(currentForm.copy(formFields = updatedFormFields))
                                autoFilling = false
                                autoFillingJob = null
                            }
                        },
                        enabled = formFields.isNotEmpty() && !parsing,
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

                // Edit button (edit both extracted info and form fields)
                IconButton(
                    onClick = {
                        if (isEditing) {
                            // Save changes to database when finishing editing
                            extractedInfo = editedExtractedInfo
                            formFields = editedFormFields
                            persistFormUpdate(currentForm.copy(
                                extractedInfo = editedExtractedInfo,
                                formFields = editedFormFields
                            ))
                        } else {
                            // Entering edit mode, copy current data
                            editedExtractedInfo = extractedInfo
                            editedFormFields = formFields
                        }
                        isEditing = !isEditing
                    },
                    enabled = (extractedInfo.isNotEmpty() || formFields.isNotEmpty()) && !parsing && !autoFilling,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditing) "Save" else "Edit"
                    )
                }

                // Clear Form button (clear form fields only)
                IconButton(
                    onClick = {
                        val clearedFormFields = formFields.map { field ->
                            field.copy(value = null, isRetrieved = false)
                        }
                        formFields = clearedFormFields
                        editedFormFields = clearedFormFields
                        persistFormUpdate(currentForm.copy(formFields = clearedFormFields))
                    },
                    enabled = formFields.isNotEmpty() && !parsing && !autoFilling,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ClearAll, contentDescription = "Clear Form")
                }

                // Clear All button (clear both extracted info and form fields)
                IconButton(
                    onClick = {
                        formFields = emptyList()
                        extractedInfo = emptyMap()
                        editedFormFields = emptyList()
                        editedExtractedInfo = emptyMap()
                        persistFormUpdate(currentForm.copy(
                            extractedInfo = emptyMap(),
                            formFields = emptyList()
                        ))
                    },
                    enabled = (extractedInfo.isNotEmpty() || formFields.isNotEmpty()) && !parsing && !autoFilling,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear All")
                }

                // Copy button (copy everything)
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val extractedText = extractedInfo.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                        val formFieldsText = formFields.joinToString("\n") { "${it.name}: ${it.value ?: ""}" }
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
                    enabled = (extractedInfo.isNotEmpty() || formFields.isNotEmpty()) && !parsing && !autoFilling,
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
                    Text("Parsing form...", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Show auto-filling message if auto-filling
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
            if ((if (isEditing) editedExtractedInfo else extractedInfo).isNotEmpty()) {
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
            }

            // Form Fields Section
            if ((if (isEditing) editedFormFields else formFields).isNotEmpty() && !autoFilling) {
                Log.d("FormDetailScreen", "Showing form fields section. Count: ${(if (isEditing) editedFormFields else formFields).size}")
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
                        (if (isEditing) editedFormFields else formFields).forEach { field ->
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
                            if (field != (if (isEditing) editedFormFields else formFields).last()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            } else if (!parsing && !autoFilling && extractedInfo.isEmpty() && formFields.isEmpty()) {
                // Show prompt message when both extracted info and form fields are empty
                Log.d("FormDetailScreen", "Showing no information available section. formFields.isEmpty: ${formFields.isEmpty()}, extractedInfo.isEmpty: ${extractedInfo.isEmpty()}")
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
                text = "Upload Date: ${currentForm.uploadDate}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Delete button at the bottom
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
                        // Always go back to gallery when deleting, regardless of source
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
    field: cn.edu.sjtu.deepsleep.docusnap.data.FormField,
    isEditing: Boolean,
    onNavigate: (String) -> Unit,
    onValueChange: ((String) -> Unit)? = null
) {
    var value by remember { mutableStateOf(field.value ?: "") }
    val context = LocalContext.current

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
                        onClick = { onNavigate("document_detail?documentId=${field.srcFileId}&fromImageProcessing=false") },
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
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
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