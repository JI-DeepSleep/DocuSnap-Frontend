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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.edu.sjtu.deepsleep.docusnap.ui.components.SearchBar
import cn.edu.sjtu.deepsleep.docusnap.data.MockData
import android.widget.Toast

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // App Title
        Text(
            text = "DocuSnap",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
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
            text = "Upload Document",
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
            text = "Upload Form",
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

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = { /* Not supported in demo */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            enabled = false
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Online Form (Coming soon)")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Frequently Used Text Info Section
        Text(
            text = "Frequently Used Text Info",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // TODO: dynamically retrieve text info sorted by search frequency
        // Sample frequently used text info
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
                TextualInfoItem(
                    text = "• Starbucks receipt: $12.50 on 2024-01-15",
                    onNavigate = onNavigate
                )
                TextualInfoItem(
                    text = "• Office supplies: $1,245.50 due 2024-02-10",
                    onNavigate = onNavigate
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Important Contacts",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextualInfoItem(
                    text = "• HR Department: hr@company.com",
                    onNavigate = onNavigate
                )
                TextualInfoItem(
                    text = "• IT Support: support@company.com",
                    onNavigate = onNavigate
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Travel Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextualInfoItem(
                    text = "• Flight: CA1234, Shanghai to Beijing, 2024-02-15",
                    onNavigate = onNavigate
                )
                TextualInfoItem(
                    text = "• Hotel: Hilton Shanghai, Confirmation #HIL789",
                    onNavigate = onNavigate
                )
            }
        }
    }
}

@Composable
private fun TextualInfoItem(
    text: String,
    onNavigate: (String) -> Unit
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
            onClick = { onNavigate("document_detail") },
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