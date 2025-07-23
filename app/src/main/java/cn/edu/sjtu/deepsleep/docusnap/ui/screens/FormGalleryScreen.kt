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
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.edu.sjtu.deepsleep.docusnap.di.AppModule
import cn.edu.sjtu.deepsleep.docusnap.ui.viewmodels.DocumentViewModel
import cn.edu.sjtu.deepsleep.docusnap.ui.viewmodels.DocumentViewModelFactory
import cn.edu.sjtu.deepsleep.docusnap.ui.components.DocumentCard
import cn.edu.sjtu.deepsleep.docusnap.ui.components.FormCard
import cn.edu.sjtu.deepsleep.docusnap.ui.components.SearchBar
import android.widget.Toast

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FormGalleryScreen(
    onNavigate: (String) -> Unit,
    viewModel: DocumentViewModel = viewModel(
        factory = DocumentViewModelFactory(
            AppModule.provideDocumentRepository(LocalContext.current)
        )
    )
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedForms by remember { mutableStateOf(mutableSetOf<String>()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Collect forms and reverse the order
    val rev_forms by viewModel.forms.collectAsState()
    val forms = remember(rev_forms) { rev_forms.reversed() }

    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar
        TopAppBar(
            title = { Text(
                text = "Forms",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            ) },
            actions = {
                if (isSelectionMode) {
                    TextButton(
                        onClick = {
                            if (selectedForms.size == forms.size) {
                                selectedForms = mutableSetOf()
                            } else {
                                selectedForms = forms.map { it.id }.toMutableSet()
                            }
                        }
                    ) {
                        Text(
                            text = if (selectedForms.size == forms.size) {
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

            if (isLoading) {
                Box(
                    modifier = Modifier.weight(1f),
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
                    itemsIndexed(forms) { index, form ->
                        val isSelected = selectedForms.contains(form.id)
                        val cardModifier = Modifier.combinedClickable(
                            onClick = {
                                if (isSelectionMode) {
                                    val newSelected = selectedForms.toMutableSet()
                                    if (isSelected) {
                                        newSelected.remove(form.id)
                                    } else {
                                        newSelected.add(form.id)
                                    }
                                    selectedForms = newSelected
                                } else {
                                    onNavigate("form_detail?formId=${form.id}&fromImageProcessing=false")
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedForms = mutableSetOf(form.id)
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
                                    newSelected.add(form.id)
                                } else {
                                    newSelected.remove(form.id)
                                }
                                selectedForms = newSelected
                            },
                            modifier = cardModifier
                        )
                    }
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
                                Toast.makeText(
                                    context,
                                    "Exporting ${selectedForms.size} form(s) to local media...",
                                    Toast.LENGTH_SHORT
                                ).show()
                                viewModel.exportForms(selectedForms.toList())
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
                        viewModel.deleteForms(selectedForms.toList())
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