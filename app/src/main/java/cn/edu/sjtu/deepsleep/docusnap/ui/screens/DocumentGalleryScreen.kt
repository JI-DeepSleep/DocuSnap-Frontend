package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import cn.edu.sjtu.deepsleep.docusnap.ui.components.SearchBar
import cn.edu.sjtu.deepsleep.docusnap.ui.components.DocumentCard
import cn.edu.sjtu.deepsleep.docusnap.ui.viewmodels.DocumentViewModel
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.edu.sjtu.deepsleep.docusnap.di.AppModule
import cn.edu.sjtu.deepsleep.docusnap.ui.viewmodels.DocumentViewModelFactory

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentGalleryScreen(
    onNavigate: (String) -> Unit,
    viewModel: DocumentViewModel = viewModel(
        factory = DocumentViewModelFactory(
            AppModule.provideDocumentRepository(LocalContext.current)
        )
    )
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedDocuments by remember { mutableStateOf(mutableSetOf<String>()) } // Changed to use document IDs
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Collect state from ViewModel
    val rev_documents by viewModel.documents.collectAsState()
    val documents = remember(rev_documents) { rev_documents.reversed() }
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar
        TopAppBar(
            title = { Text(
                text = "Documents",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )  },
            actions = {
                if (isSelectionMode) {
                    // Select All text button
                    TextButton(
                        onClick = {
                            if (selectedDocuments.size == documents.size) {
                                // Deselect all
                                selectedDocuments = mutableSetOf()
                            } else {
                                // Select all
                                selectedDocuments = documents.map { it.id }.toMutableSet()
                            }
                        }
                    ) {
                        Text(
                            text = if (selectedDocuments.size == documents.size) {
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

            // Test button for development (remove in production)
//            Button(
//                onClick = {
//                    android.util.Log.d("DocumentGalleryScreen", "Add Test Data button clicked")
//                    Toast.makeText(context, "Adding test data...", Toast.LENGTH_SHORT).show()
//                    viewModel.addTestData()
//                },
//                modifier = Modifier.padding(bottom = 8.dp)
//            ) {
//                Text("Add Test Data")
//            }

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Image Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(documents) { index, document ->
                        val isSelected = selectedDocuments.contains(document.id)
                        
                        val cardModifier = Modifier.combinedClickable(
                            onClick = {
                                if (isSelectionMode) {
                                    // Toggle selection
                                    val newSelected = selectedDocuments.toMutableSet()
                                    if (isSelected) {
                                        newSelected.remove(document.id)
                                    } else {
                                        newSelected.add(document.id)
                                    }
                                    selectedDocuments = newSelected
                                } else {
                                    onNavigate("document_detail?documentId=${document.id}&fromImageProcessing=false")
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    // Enter selection mode and select this card
                                    isSelectionMode = true
                                    selectedDocuments = mutableSetOf(document.id)
                                }
                            }
                        )
                        
                        DocumentCard(
                            document = document,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onSelectionChanged = { checked ->
                                val newSelected = selectedDocuments.toMutableSet()
                                if (checked) {
                                    newSelected.add(document.id)
                                } else {
                                    newSelected.remove(document.id)
                                }
                                selectedDocuments = newSelected
                            },
                            modifier = cardModifier
                        )
                    }
                }
            }
        }
    }
    
    // Selection mode bottom bar
    if (isSelectionMode) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedDocuments.size} selected",
                    modifier = Modifier.weight(1f)
                )
                
                OutlinedButton(
                    onClick = {
                        isSelectionMode = false
                        selectedDocuments = mutableSetOf()
                    }
                ) {
                    Text("Cancel")
                }
                
                OutlinedButton(
                    onClick = {
                        // Export selected documents to local media
                        Toast.makeText(
                            context,
                            "Exporting ${selectedDocuments.size} document(s) to local media...",
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.exportForms(selectedDocuments.toList())
                    },
                    enabled = selectedDocuments.isNotEmpty()
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
                    enabled = selectedDocuments.isNotEmpty(),
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
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete ${selectedDocuments.size} selected document(s)? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocuments(selectedDocuments.toList())
                        showDeleteConfirmation = false
                        isSelectionMode = false
                        selectedDocuments = mutableSetOf()
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