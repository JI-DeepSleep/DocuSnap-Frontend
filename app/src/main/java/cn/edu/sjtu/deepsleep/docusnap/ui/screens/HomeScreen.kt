package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.edu.sjtu.deepsleep.docusnap.service.DeviceDBService
import cn.edu.sjtu.deepsleep.docusnap.ui.components.SearchBar
import cn.edu.sjtu.deepsleep.docusnap.ui.components.TextInfoItem
import cn.edu.sjtu.deepsleep.docusnap.data.MockData

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // TODO: DeviceDBService.getFrequentTextInfo()
    val textInfoByCategory = remember { MockData.getFrequentTextInfo() }

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
                IconButton(
                    onClick = { onNavigate("job_status") },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = "Job Status",
                        modifier = Modifier.size(24.dp)
                    )
                }
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
            onSearch = { onNavigate("search") },
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
            text = "Frequently Used Text Info",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Dynamically display text info grouped by category
        textInfoByCategory.forEach { (category, textInfoList) ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = category,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    textInfoList.forEach { textInfo ->
                        TextInfoItem(
                            textInfo = textInfo,
                            onNavigate = onNavigate
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
} 