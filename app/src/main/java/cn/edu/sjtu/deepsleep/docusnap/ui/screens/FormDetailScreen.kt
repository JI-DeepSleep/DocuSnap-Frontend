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

@Composable
fun FormDetailScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit,
    formId: String? = null
) {
    // Find the specific form by ID, or use the first one as fallback
    val form = remember(formId) {
        if (formId != null) {
            MockData.mockForms.find { it.id == formId } ?: MockData.mockForms.first()
        } else {
            MockData.mockForms.first()
        }
    }
    var isEditing by remember { mutableStateOf(false) }
    var parsing by remember { mutableStateOf(false) }
    var parsingJob by remember { mutableStateOf<Job?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Image navigation state
    var currentImageIndex by remember { mutableStateOf(0) }
    val imageUris = remember { form.imageUris }
    
    // Store original form fields for restoration during parsing
    val originalFormFields = remember { form.formFields }
    
    // Create mutable state for form fields with proper initial highlighting
    var formFields by remember { 
        mutableStateOf(
            form.formFields.map { field ->
                // If field is not retrieved and has empty value, set value to null for proper highlighting
                if (!field.isRetrieved && (field.value.isNullOrEmpty() || field.value == "")) {
                    field.copy(value = null)
                } else {
                    field
                }
            }
        )
    }

    // Get related files using MockData helper functions
    val relatedFiles = remember(form) {
        MockData.getRelatedDocumentsForForm(form.id)
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
            title = { Text(form.name) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        // TODO: Implement export logic (save form image to gallery)
                        Toast.makeText(context, "Form saved to local media", Toast.LENGTH_SHORT).show()
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
                            contentDescription = "Form image ${currentImageIndex + 1}",
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
                                    text = "📋",
                                    fontSize = 48.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = form.name,
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
                text = form.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Form Fields Section with Help Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Form Fields",
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

            // Action buttons row: Parse, Edit, Clear, Auto-fill, Clear Values
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
                            parsing = true
                            parsingJob = scope.launch {
                                // TODO： form parsing API
                                delay(1000) // Simulate parsing
                                // Parse restores the original field structure with empty values
                                formFields = originalFormFields.map { field ->
                                    field.copy(value = null, isRetrieved = false)
                                }
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
                            parsingJob?.cancel()
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
                    onClick = { isEditing = !isEditing },
                    enabled = formFields.isNotEmpty() && !parsing,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditing) "Save" else "Edit"
                    )
                }
                
                // Clear Values button
                IconButton(
                    onClick = { 
                        formFields = formFields.map { field ->
                            field.copy(value = null, isRetrieved = false)
                        }
                    },
                    enabled = formFields.isNotEmpty() && !parsing,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ClearAll, contentDescription = "Clear Values")
                }

                // Clear button (clears everything)
                IconButton(
                    onClick = {
                        formFields = emptyList()
                    },
                    enabled = formFields.isNotEmpty() && !parsing,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear All")
                }
            }

            // Show parsing/auto-filling message or form fields
            if (parsing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Parsing form fields...", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
            } else if (formFields.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        formFields.forEach { field ->
                            FormFieldDisplayItem(
                                field = field, 
                                isEditing = isEditing,
                                onNavigate = onNavigate
                            )
                            if (field != formFields.last()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            } else {
                // Show empty state when no fields
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
                            "No form fields available. Use Parse to extract fields from the form.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
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
                text = "Upload Date: ${form.uploadDate}",
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
            title = { Text("Form Field Actions") },
            text = {
                Column {
                    HelpItem(
                        icon = Icons.Default.Animation,
                        title = "Parse",
                        description = "Extract field names from the form image. This identifies what fields need to be filled without adding values."
                    )
                    HelpItem(
                        icon = Icons.Default.Edit,
                        title = "Edit",
                        description = "Toggle edit mode to manually modify field values."
                    )
                    HelpItem(
                        icon = Icons.Default.ClearAll,
                        title = "Clear Values",
                        description = "Clear all field values while keeping the field names intact."
                    )
                    HelpItem(
                        icon = Icons.Default.Clear,
                        title = "Clear All",
                        description = "Remove all form fields completely. Use Parse to restore them."
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
                        // TODO: Implement actual deletion logic
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
    onNavigate: (String) -> Unit
) {
    var value by remember { mutableStateOf(field.value ?: "") }
    val context = LocalContext.current
    
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
                // Show link to source document if srcDocId is present
                if (field.srcDocId != null) {
                    IconButton(
                        onClick = { onNavigate("document_detail?documentId=${field.srcDocId}&fromImageProcessing=false") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "Go to source document",
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
                    onValueChange = { value = it },
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