package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.edu.sjtu.deepsleep.docusnap.service.DeviceDBService
import cn.edu.sjtu.deepsleep.docusnap.ui.components.SearchBar
import cn.edu.sjtu.deepsleep.docusnap.ui.components.SearchEntityCard
import androidx.compose.runtime.LaunchedEffect
 import androidx.compose.runtime.getValue
 import androidx.compose.runtime.setValue
 import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import cn.edu.sjtu.deepsleep.docusnap.data.SearchEntity
import cn.edu.sjtu.deepsleep.docusnap.data.FileType
import cn.edu.sjtu.deepsleep.docusnap.data.local.AppDatabase
import cn.edu.sjtu.deepsleep.docusnap.di.AppModule


@Composable
fun SearchScreen(
    query: String?,
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit,
    documentViewModel: cn.edu.sjtu.deepsleep.docusnap.ui.viewmodels.DocumentViewModel
) {
    var searchQuery by remember { mutableStateOf(query ?: "") }

    var searchResults by remember { mutableStateOf<List<SearchEntity>>(emptyList()) }
    val context = LocalContext.current
    val documentRepository = remember { AppModule.provideDocumentRepository(context) }

    LaunchedEffect(searchQuery) {
        searchResults = documentRepository.searchByQuery(searchQuery)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Search Results") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                onSearch = { /* Search functionality would be implemented here */ },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Search Results Count
            if (searchResults.isNotEmpty()) {
                Text(
                    // MINIMAL CHANGE 3.2: Get the size from the list directly.
                    text = "${searchResults.size} results found",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Unified Search Results
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // MINIMAL CHANGE 3.3: Iterate over the new `searchResults` list directly.
                items(searchResults) { entity ->
                    SearchEntityCard(
                        entity = entity,
                        onClick = {
                            when (entity) {
                                is cn.edu.sjtu.deepsleep.docusnap.data.SearchEntity.TextEntity -> {
                                    if (entity.srcFileId != null && entity.srcFileType != null) {
                                        when (entity.srcFileType) {
                                            FileType.DOCUMENT -> onNavigate("document_detail?documentId=${entity.srcFileId}&fromImageProcessing=false")
                                            FileType.FORM -> onNavigate("form_detail?formId=${entity.srcFileId}&fromImageProcessing=false")
                                        }
                                    } else {
                                        onNavigate("document_detail?fromImageProcessing=false")
                                    }
                                }
                                is cn.edu.sjtu.deepsleep.docusnap.data.SearchEntity.DocumentEntity -> {
                                    onNavigate("document_detail?documentId=${entity.document.id}&fromImageProcessing=false")
                                }
                                is cn.edu.sjtu.deepsleep.docusnap.data.SearchEntity.FormEntity -> {
                                    onNavigate("form_detail?formId=${entity.form.id}&fromImageProcessing=false")
                                }
                            }
                        },
                        onCopyText = {
                            when (entity) {
                                is cn.edu.sjtu.deepsleep.docusnap.data.SearchEntity.TextEntity -> {
                                    if (entity.srcFileId != null && entity.srcFileType != null) {
                                        // Parse the text to extract key
                                        val keyValue = entity.text.split(":", limit = 2)
                                        val key = if (keyValue.size > 1) keyValue[0].trim() else ""
                                        if (key.isNotEmpty()) {
                                            documentViewModel.updateExtractedInfoUsage(
                                                fileId = entity.srcFileId,
                                                fileType = entity.srcFileType,
                                                key = key
                                            )
                                        }
                                    }
                                }
                                else -> { /* No action needed for other entity types */ }
                            }
                        }
                    )
                }

                // No results message
                // MINIMAL CHANGE 3.4: The condition is updated to check the list directly
                // and also check if the user has actually typed something.
                if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No results found",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Try different search terms",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 