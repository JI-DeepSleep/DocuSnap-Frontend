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
import cn.edu.sjtu.deepsleep.docusnap.ui.components.DocumentCard
import cn.edu.sjtu.deepsleep.docusnap.ui.components.FormCard
import cn.edu.sjtu.deepsleep.docusnap.ui.components.SearchBar
import cn.edu.sjtu.deepsleep.docusnap.ui.components.SectionHeader
import androidx.compose.material.icons.filled.Link
import androidx.compose.ui.Alignment

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

            // Search Results
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Textual Information
                if (searchResults.textualInfo.isNotEmpty()) {
                    item {
                        SectionHeader("Textual Information")
                    }
                    items(searchResults.textualInfo) { info ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = info,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onNavigate("document_display") }) {
                                    Icon(Icons.Default.Link, contentDescription = "Go to source document")
                                }
                            }
                        }
                    }
                }

                // Documents
                if (searchResults.documents.isNotEmpty()) {
                    item {
                        SectionHeader("Documents")
                    }
                    items(searchResults.documents) { document ->
                        DocumentCard(
                            document = document,
                            onClick = { onNavigate("document_display") }
                        )
                    }
                }

                // Forms
                if (searchResults.forms.isNotEmpty()) {
                    item {
                        SectionHeader("Forms")
                    }
                    items(searchResults.forms) { form ->
                        FormCard(
                            form = form,
                            selected = false,
                            onClick = { onNavigate("form_display") }
                        )
                    }
                }

                // No results message
                if (searchResults.documents.isEmpty() && 
                    searchResults.forms.isEmpty() && 
                    searchResults.textualInfo.isEmpty()) {
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