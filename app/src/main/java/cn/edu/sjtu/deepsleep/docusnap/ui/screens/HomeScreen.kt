package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.edu.sjtu.deepsleep.docusnap.ui.components.SearchBar
import cn.edu.sjtu.deepsleep.docusnap.ui.components.TextInfoItem
import cn.edu.sjtu.deepsleep.docusnap.navigation.Screen
import cn.edu.sjtu.deepsleep.docusnap.viewmodels.DocumentViewModel

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    documentViewModel: DocumentViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Get real text info from ViewModel
    val frequentTextInfo by documentViewModel.frequentTextInfo.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
//        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title with Settings Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
//            Spacer(modifier = Modifier.width(48.dp)) // Balance the settings icon
            Text(
                text = "DocuSnap",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row {
//                IconButton(
//                    onClick = { onNavigate("job_status") },
//                    modifier = Modifier.size(48.dp)
//                ) {
//                    Icon(
//                        Icons.Default.List,
//                        contentDescription = "Job Status",
//                        modifier = Modifier.size(24.dp)
//                    )
//                }
                IconButton(
                    onClick = { onNavigate("settings") },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Text(
            text = "Your AI-powered Personal Document Assistant",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Search Bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { onNavigate(Screen.Search.route + "?query=$searchQuery") },
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Document Upload Section
        Text(
            text = "Import Document",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onNavigate("camera_capture?source=document") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Camera")
            }
            Button(
                onClick = { onNavigate("local_media?source=document") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gallery")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Form Upload Section
        Text(
            text = "Import Form",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onNavigate("camera_capture?source=form") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Camera")
            }
            Button(
                onClick = { onNavigate("local_media?source=form") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gallery")
            }
        }

//        Spacer(modifier = Modifier.height(4.dp))
//
//        Button(
//            onClick = { /* Not supported in demo */ },
//            modifier = Modifier.fillMaxWidth(),
//            colors = ButtonDefaults.buttonColors(
//                containerColor = MaterialTheme.colorScheme.surfaceVariant,
//                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
//            ),
//            enabled = false
//        ) {
//            Icon(Icons.Default.Add, contentDescription = null)
//            Spacer(modifier = Modifier.width(8.dp))
//            Text("Online Form (Coming soon)")
//        }

        Spacer(modifier = Modifier.height(24.dp))

        // Frequently Used Text Info Section
        Text(
            text = "Frequently Used Info",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Display all text info as a simple list
        if (frequentTextInfo.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    frequentTextInfo.forEach { textInfo ->
                        TextInfoItem(
                            textInfo = textInfo,
                            onNavigate = onNavigate,
                            onCopyText = {
                                documentViewModel.updateExtractedInfoUsage(
                                    fileId = textInfo.srcFileId,
                                    fileType = textInfo.srcFileType,
                                    key = textInfo.key
                                )
                            }
                        )
                        if (textInfo != frequentTextInfo.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "No extracted text info available yet. Upload documents or forms to see extracted information here.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
} 