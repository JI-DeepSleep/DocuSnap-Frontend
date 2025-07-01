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
import cn.edu.sjtu.deepsleep.docusnap.ui.components.FormCard

@Composable
fun UploadedFormSelectionScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedForm by remember { mutableStateOf<cn.edu.sjtu.deepsleep.docusnap.data.Form?>(null) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Select Form") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // Form Gallery
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(MockData.mockForms) { form ->
                FormCard(
                    form = form,
                    onClick = { selectedForm = form },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Bottom Button
        if (selectedForm != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { onNavigate("fill_form") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fill This Form")
                }
            }
        }
    }
} 