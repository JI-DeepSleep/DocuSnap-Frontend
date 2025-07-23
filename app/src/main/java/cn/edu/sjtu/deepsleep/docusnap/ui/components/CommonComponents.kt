package cn.edu.sjtu.deepsleep.docusnap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import cn.edu.sjtu.deepsleep.docusnap.data.SearchEntity
import android.widget.Toast
import cn.edu.sjtu.deepsleep.docusnap.data.MockData
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.edu.sjtu.deepsleep.docusnap.data.Document

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search..."
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Button(
            onClick = onSearch,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(56.dp),
            enabled = query.isNotEmpty()
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search")
        }
    }
}

// Unified search result card components
@Composable
fun SearchEntityCard(
    entity: SearchEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (entity) {
        is SearchEntity.TextEntity -> {
            TextualInfoCard(
                text = entity.text,
                srcFileId = entity.srcFileId,
                onNavigate = onClick,
                modifier = modifier
            )
        }
        is SearchEntity.DocumentEntity -> {
            DocumentSearchCard(
                document = entity.document,
                onClick = onClick,
                modifier = modifier
            )
        }
        is SearchEntity.FormEntity -> {
            FormSearchCard(
                form = entity.form,
                onClick = onClick,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun TextualInfoCard(
    text: String,
    srcFileId: String?,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Parse text as key-value pair (assuming format like "key: value")
    val keyValue = text.split(":", limit = 2)
    val key = if (keyValue.size > 1) keyValue[0].trim() else ""
    val value = if (keyValue.size > 1) keyValue[1].trim() else text
    
    // Get source file info
    val sourceDoc = if (srcFileId != null) MockData.mockDocuments.find { it.id == srcFileId } else null
    val sourceForm = if (srcFileId != null) MockData.mockForms.find { it.id == srcFileId } else null
    val sourceName = sourceDoc?.name ?: sourceForm?.name
    
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onNavigate,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // First line: key and source file with link icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = key,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                if (sourceName != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = sourceName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "Go to source file",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Second line: value with copy icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Text Info", text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy text",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentSearchCard(
    document: cn.edu.sjtu.deepsleep.docusnap.data.Document,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.height(120.dp)
        ) {
            // Preview image on the left spanning card vertically
            Card(
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val imageBitmap = remember(document.imageBase64s) {
                        if (document.imageBase64s.isNotEmpty()) {
                            try {
                                val imageBytes = Base64.decode(document.imageBase64s.first(), Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                bitmap?.asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                    }
                    
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Document preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = "📄",
                            fontSize = 24.sp
                        )
                    }
                }
            }
            
            // Content on the right
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // First line: small image icon and document name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "Document",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = document.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Second line: tags and date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tags
                    if (document.tags.isNotEmpty()) {
                        Row {
                            document.tags.take(2).forEach { tag ->
                                AssistChip(
                                    onClick = { },
                                    label = { Text(tag, fontSize = 10.sp) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                    }
                    
                    // Date
                    Text(
                        text = document.uploadDate,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Third line: info preview
                document.extractedInfo.entries.take(2).forEach { (key, value) ->
                    Text(
                        text = "$key: $value",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormSearchCard(
    form: cn.edu.sjtu.deepsleep.docusnap.data.Form,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.height(120.dp)
        ) {
            // Preview image on the left spanning card vertically
            Card(
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val imageBitmap = remember(form.imageBase64s) {
                        if (form.imageBase64s.isNotEmpty()) {
                            try {
                                val imageBytes = Base64.decode(form.imageBase64s.first(), Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                bitmap?.asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                    }
                    
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Form preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = "📋",
                            fontSize = 24.sp
                        )
                    }
                }
            }
            
            // Content on the right
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // First line: small list icon and form name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = "Form",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = form.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Second line: field count and date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${form.formFields.size} fields",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = form.uploadDate,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Third line: form fields preview
                form.formFields.filter { it.value != null && it.value.isNotEmpty() }.take(2).forEach { field ->
                    Text(
                        text = "${field.name}: ${field.value}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

// Reusable TextualInfoItem component moved from HomeScreen
@Composable
fun TextualInfoItem(
    text: String,
    onNavigate: (String) -> Unit,
    documentId: String? = null
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = { 
                if (documentId != null) {
                    onNavigate("document_detail?documentId=$documentId&fromImageProcessing=false")
                } else {
                    onNavigate("document_detail?fromImageProcessing=false")
                }
            },
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                Icons.Default.Link,
                contentDescription = "Go to source document",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(
            onClick = {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Text Info", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy text",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Component for text info that works with the data structure
@Composable
fun TextInfoItem(
    textInfo: cn.edu.sjtu.deepsleep.docusnap.data.TextInfo,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Display key-value pair
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${textInfo.key}: ${textInfo.value}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = { 
                // Navigate to source file (document or form)
                when {
                    MockData.mockDocuments.any { it.id == textInfo.srcFileId } -> 
                        onNavigate("document_detail?documentId=${textInfo.srcFileId}&fromImageProcessing=false")
                    MockData.mockForms.any { it.id == textInfo.srcFileId } -> 
                        onNavigate("form_detail?formId=${textInfo.srcFileId}&fromImageProcessing=false")
                    else -> onNavigate("document_detail?fromImageProcessing=false")
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
        IconButton(
            onClick = {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Text Info", "${textInfo.key}: ${textInfo.value}")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy text",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}


@Composable
fun DocumentCard(
    document: Document,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Decode the first image if available
    val imageBitmap = remember(document.imageBase64s) {
        if (document.imageBase64s.isNotEmpty()) {
            try {
                val bytes = Base64.decode(document.imageBase64s[0], Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Preview image in upper part with checkbox overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Gray.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Document preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "📄",
                        fontSize = 32.sp
                    )
                }

                // Selection checkbox (bottom right corner of image only)
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = onSelectionChanged,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }
            }

            // Bottom section with name, date, and status
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Document name
                Text(
                    text = document.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )

                // Date and status icon row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = document.uploadDate,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Parse status icon
                    Icon(
                        imageVector = if (document.extractedInfo.isNotEmpty()) {
                            Icons.Default.DocumentScanner
                        } else {
                            Icons.Default.RadioButtonUnchecked
                        },
                        contentDescription = if (document.extractedInfo.isNotEmpty()) {
                            "Parsed"
                        } else {
                            "Unparsed"
                        },
                        modifier = Modifier.size(16.dp),
                        tint = if (document.extractedInfo.isNotEmpty()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FormCard(
    form: cn.edu.sjtu.deepsleep.docusnap.data.Form,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Decode the first image if available
    val imageBitmap = remember(form.imageBase64s) {
        if (form.imageBase64s.isNotEmpty()) {
            try {
                val bytes = Base64.decode(form.imageBase64s[0], Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Preview image in upper part with checkbox overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Gray.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Form preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "📋",
                        fontSize = 32.sp
                    )
                }

                // Selection checkbox (bottom right corner of image only)
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = onSelectionChanged,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }
            }

            // Bottom section with name, date, and status
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Form name
                Text(
                    text = form.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )

                // Date and status icon row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = form.uploadDate,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Fill status icon
                    val filledFields = form.formFields.count { it.value != null && it.value.isNotEmpty() }
                    val totalFields = form.formFields.size
                    val isFilled = filledFields > 0 || totalFields == 0

                    Row {
                        // Parse status icon
                        Icon(
                            imageVector = if (form.extractedInfo.isNotEmpty()) {
                                Icons.Default.DocumentScanner
                            } else {
                                Icons.Default.RadioButtonUnchecked
                            },
                            contentDescription = if (form.extractedInfo.isNotEmpty()) {
                                "Parsed"
                            } else {
                                "Unparsed"
                            },
                            modifier = Modifier.size(16.dp),
                            tint = if (form.extractedInfo.isNotEmpty()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = if (isFilled) {
                                Icons.Default.AutoAwesome
                            } else {
                                Icons.Default.RadioButtonUnchecked
                            },
                            contentDescription = if (isFilled) {
                                "Filled"
                            } else {
                                "Unfilled"
                            },
                            modifier = Modifier.size(16.dp),
                            tint = if (isFilled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}