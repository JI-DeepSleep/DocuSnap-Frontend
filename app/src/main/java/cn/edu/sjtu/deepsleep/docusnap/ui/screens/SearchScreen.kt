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
import cn.edu.sjtu.deepsleep.docusnap.data.MockData
import cn.edu.sjtu.deepsleep.docusnap.ui.components.SearchBar
import cn.edu.sjtu.deepsleep.docusnap.ui.components.SearchEntityCard

@Composable
fun SearchScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults = remember { MockData.mockSearchResults }

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
            if (searchResults.entities.isNotEmpty()) {
                Text(
                    text = "${searchResults.entities.size} results found",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Unified Search Results
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(searchResults.entities) { entity ->
                    SearchEntityCard(
                        entity = entity,
                        onClick = {
                            when (entity) {
                                is cn.edu.sjtu.deepsleep.docusnap.data.SearchEntity.TextEntity -> {
                                    // For text entities, navigate to the source document if available
                                    val sourceDoc = MockData.mockDocuments.find { it.name == entity.sourceDocument }
                                    if (sourceDoc != null) {
                                        onNavigate("document_detail?documentId=${sourceDoc.id}&fromImageProcessing=false")
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
                        }
                    )
                }

                // No results message
                if (searchResults.entities.isEmpty()) {
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