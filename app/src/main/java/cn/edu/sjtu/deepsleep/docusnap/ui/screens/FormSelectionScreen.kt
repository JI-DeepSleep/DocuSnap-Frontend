package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.edu.sjtu.deepsleep.docusnap.data.MockData
import cn.edu.sjtu.deepsleep.docusnap.ui.components.FormCard

@Composable
fun FormSelectionScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedFormId by remember { mutableStateOf<String?>(null) }

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
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(MockData.mockForms) { form ->
                FormCard(
                    form = form,
                    selected = selectedFormId == form.id,
                    onClick = { selectedFormId = form.id },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Bottom Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
                onClick = { if (selectedFormId != null) onNavigate("form_autofill") },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedFormId != null
            ) {
                Text("Fill This Form")
            }
        }
    }
} 