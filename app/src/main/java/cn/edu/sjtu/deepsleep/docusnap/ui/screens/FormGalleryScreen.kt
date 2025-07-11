package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.edu.sjtu.deepsleep.docusnap.data.MockData
import cn.edu.sjtu.deepsleep.docusnap.ui.components.DocumentCard
import cn.edu.sjtu.deepsleep.docusnap.ui.components.FormCard
import cn.edu.sjtu.deepsleep.docusnap.ui.components.SearchBar
import android.widget.Toast

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FormGalleryScreen(
    onNavigate: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedForms by remember { mutableStateOf(mutableSetOf<Int>()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Forms") },
            actions = {
                if (isSelectionMode) {
                    // Select All text button
                    TextButton(
                        onClick = {
                            if (selectedForms.size == MockData.mockForms.size) {
                                // Deselect all
                                selectedForms = mutableSetOf()
                            } else {
                                // Select all
                                selectedForms = (0 until MockData.mockForms.size).toMutableSet()
                            }
                        }
                    ) {
                        Text(
                            text = if (selectedForms.size == MockData.mockForms.size) {
                                "Deselect All"
                            } else {
                                "Select All"
                            },
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { onNavigate("search") },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Image Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(MockData.mockForms) { index, form ->
                    val isSelected = selectedForms.contains(index)
                    
                    val cardModifier = Modifier.combinedClickable(
                        onClick = {
                            if (isSelectionMode) {
                                // Toggle selection
                                val newSelected = selectedForms.toMutableSet()
                                if (isSelected) {
                                    newSelected.remove(index)
                                } else {
                                    newSelected.add(index)
                                }
                                selectedForms = newSelected
                            } else {
                                onNavigate("form_detail?formId=${form.id}&fromImageProcessing=false")
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                // Enter selection mode and select this card
                                isSelectionMode = true
                                selectedForms = mutableSetOf(index)
                            }
                        }
                    )
                    
                    FormCard(
                        form = form,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onSelectionChanged = { checked ->
                            val newSelected = selectedForms.toMutableSet()
                            if (checked) {
                                newSelected.add(index)
                            } else {
                                newSelected.remove(index)
                            }
                            selectedForms = newSelected
                        },
                        modifier = cardModifier
                    )
                }
            }
        }
    }
    
    // Bottom action bar for selection mode
    if (isSelectionMode) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedForms.size} selected",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isSelectionMode = false
                                selectedForms = mutableSetOf()
                            }
                        ) {
                            Text("Cancel")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                // Export selected forms to local media
                                Toast.makeText(
                                    context,
                                    "Exporting ${selectedForms.size} form(s) to local media...",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // TODO: Implement actual export logic
                            },
                            enabled = selectedForms.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Export",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Button(
                            onClick = {
                                showDeleteConfirmation = true
                            },
                            enabled = selectedForms.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete ${selectedForms.size} selected form(s)? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        // TODO: Implement actual deletion logic
                        showDeleteConfirmation = false
                        isSelectionMode = false
                        selectedForms = mutableSetOf()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
} 