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
import cn.edu.sjtu.deepsleep.docusnap.ui.components.SearchBar
import cn.edu.sjtu.deepsleep.docusnap.ui.components.SectionHeader
import androidx.compose.material.icons.filled.Link
import androidx.compose.ui.Alignment

@Composable
fun DocumentOverviewScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Documents") },
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
                onSearch = { onNavigate("search") },
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Recommended Info Section
                item {
                    SectionHeader(
                        title = "Recommended Info",
                        actionText = "See All",
                        onActionClick = { onNavigate("document_textual_info") }
                    )
                }

                // Sample recommended info
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Recent Expenses",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "• Starbucks receipt: $12.50 on 2024-01-15",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(onClick = { onNavigate("document_detail") }) {
                                    Icon(Icons.Default.Link, contentDescription = "Go to source document")
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "• Office supplies: $1,245.50 due 2024-02-10",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(onClick = { onNavigate("document_detail") }) {
                                    Icon(Icons.Default.Link, contentDescription = "Go to source document")
                                }
                            }
                        }
                    }
                }

                // Recommended Images Section
                item {
                    SectionHeader(
                        title = "Recommended Images",
                        actionText = "See All",
                        onActionClick = { onNavigate("document_gallery") }
                    )
                }

                // Sample document images
                items(MockData.mockDocuments.take(3)) { document ->
                    DocumentCard(
                        document = document,
                        onClick = { onNavigate("document_detail") }
                    )
                }
            }
        }
    }
} 