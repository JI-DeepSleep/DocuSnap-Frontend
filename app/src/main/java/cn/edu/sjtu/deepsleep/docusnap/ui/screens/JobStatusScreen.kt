package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import cn.edu.sjtu.deepsleep.docusnap.data.local.AppDatabase
import cn.edu.sjtu.deepsleep.docusnap.data.local.JobEntity
import cn.edu.sjtu.deepsleep.docusnap.service.JobPollingService
import kotlinx.coroutines.launch
import android.util.Base64
import cn.edu.sjtu.deepsleep.docusnap.util.CryptoUtil
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun JobStatusScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val jobPollingService = remember { JobPollingService(context) }
    val coroutineScope = rememberCoroutineScope()
    var jobs by remember { mutableStateOf<List<JobEntity>>(emptyList()) }
    var cleanupMessage by remember { mutableStateOf<String?>(null) }
    var lastRefreshTime by remember { mutableStateOf("Never") }
    var isPollingActive by remember { mutableStateOf(false) }

    // Function to refresh jobs
    val refreshJobs = {
        coroutineScope.launch {
            try {
                val database = AppDatabase.getInstance(context)
                val jobDao = database.jobDao()
                jobDao.getAllJobs().collect { jobList ->
                    jobs = jobList
                    lastRefreshTime = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())
                }
            } catch (e: Exception) {
                println("Error loading jobs: ${e.message}")
            }
        }
    }

    // Function to check polling status
    val checkPollingStatus = {
        isPollingActive = jobPollingService.isPollingActive()
    }

    // Collect jobs from database
    LaunchedEffect(Unit) {
        refreshJobs()
        checkPollingStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Job Status",
                style = MaterialTheme.typography.headlineMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last refresh: $lastRefreshTime",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Polling: ${if (isPollingActive) "Active" else "Inactive"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPollingActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                IconButton(onClick = { refreshJobs() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                Button(onClick = onBackClick) {
                    Text("Back")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Test buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            // Create a test job with proper payload
                            val testBase64Image = context.assets.open("test_image_base64.txt")
                                .bufferedReader()
                                .use { it.readText() }
                            val testImages = listOf(testBase64Image)
                            val job = jobPollingService.createJob(
                                type = "doc",
                                id="0",
                                payload = testImages
                            )
                            println("Created test job with ID: ${job.id}")
                            refreshJobs()
                        } catch (e: Exception) {
                            println("Error creating test job: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            ) {
                Text("Create Test Job")
            }
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            // Create a test job with invalid data to trigger backend errors
                            val invalidPayload = mapOf(
                                "invalid_field" to "invalid_value",
                                "missing_required" to null
                            )
                            val job = jobPollingService.createJob(
                                type = "invalid_type",
                                id="0",
                                payload = invalidPayload
                            )
                            println("Created invalid test job with ID: ${job.id}")
                            refreshJobs()
                        } catch (e: Exception) {
                            println("Error creating invalid test job: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            ) {
                Text("Create Invalid Job")
            }
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val deletedCount = jobPollingService.cleanupOldJobs()
                            cleanupMessage = "Cleaned up $deletedCount old jobs"
                            println("Cleanup completed: $deletedCount jobs deleted")
                            refreshJobs()
                        } catch (e: Exception) {
                            cleanupMessage = "Error: ${e.message}"
                            println("Error during cleanup: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            ) {
                Text("Cleanup Old Jobs")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Polling control buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = {
                    jobPollingService.startPolling()
                    cleanupMessage = "Started polling service"
                    checkPollingStatus()
                }
            ) {
                Text("Start Polling")
            }
            Button(
                onClick = {
                    jobPollingService.stopPolling()
                    cleanupMessage = "Stopped polling service"
                    checkPollingStatus()
                }
            ) {
                Text("Stop Polling")
            }
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            // Manually trigger a single polling cycle
                            jobPollingService.pollPendingJobs()
                            jobPollingService.pollProcessingJobs()
                            cleanupMessage = "Manual polling cycle completed"
                            refreshJobs()
                        } catch (e: Exception) {
                            cleanupMessage = "Manual polling error: ${e.message}"
                        }
                    }
                }
            ) {
                Text("Manual Poll")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cleanup message
        cleanupMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (message.startsWith("Error"))
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = if (message.startsWith("Error"))
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Jobs list - Now using regular Column since the parent is scrollable
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            jobs.forEach { job ->
                JobCard(job = job, jobPollingService = jobPollingService)
            }
        }
    }
}

@Composable
fun JobCard(job: JobEntity, jobPollingService: JobPollingService) {
    var expanded by remember { mutableStateOf(false) }
    var decryptedResult by remember { mutableStateOf<String?>(null) }
    var decryptError by remember { mutableStateOf<String?>(null) }
    var isDecrypting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Reset state when job changes
    LaunchedEffect(job.id) {
        decryptedResult = null
        decryptError = null
        isDecrypting = false
    }

    // Decrypt result when expanded
    LaunchedEffect(expanded) {
        if (expanded && job.result != null && decryptedResult == null && !isDecrypting) {
            isDecrypting = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val result = jobPollingService.decryptJobResult(job.result, job)
                    withContext(Dispatchers.Main) {
                        decryptedResult = result
                        decryptError = null
                        isDecrypting = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        decryptedResult = null
                        decryptError = "Decryption failed: ${e.message}"
                        isDecrypting = false
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Job #${job.id}",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusChip(status = job.status)
                    IconButton(
                        onClick = { expanded = !expanded }
                    ) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Type: ${job.type}")
            Text("SHA256: ${job.sha256.take(16)}...")
            Text("Created: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(job.createdAt))}")
            Text("Updated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(job.updatedAt))}")
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                // Full SHA256
                Text(
                    text = "Full SHA256: ${job.sha256}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                // Client ID
                Text(
                    text = "Client ID: ${job.clientId}",
                    style = MaterialTheme.typography.bodySmall
                )
                // Content info
                Text(
                    text = "Has Content: ${job.hasContent}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (job.content != null) {
                    Text(
                        text = "Content (first 100 chars): ${job.content.take(100)}...",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (job.aesKey != null) {
                    Text(
                        text = "AES Key (first 50 chars): ${job.aesKey.take(50)}...",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (job.plainAesKey != null) {
                    Text(
                        text = "Plain AES Key (first 50 chars): ${job.plainAesKey.take(50)}...",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            // Error details with better formatting
            if (job.errorDetail != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Error Details:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = job.errorDetail,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            // Result details
            if (job.result != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Encrypted Result:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (expanded) job.result else "${job.result.take(100)}...",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // +++ FIXED: Decrypted result section with proper state management +++
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Decrypted Result:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        when {
                            isDecrypting -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }

                            decryptedResult != null -> {
                                Text(
                                    text = decryptedResult ?: "",
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            decryptError != null -> {
                                Text(
                                    text = decryptError ?: "Decryption error",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            else -> {
                                // Show nothing if not expanded or not decrypting
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (backgroundColor, textColor) = when (status) {
        "pending" -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        "processing" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        "completed" -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        "error" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
    }
    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = textColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}