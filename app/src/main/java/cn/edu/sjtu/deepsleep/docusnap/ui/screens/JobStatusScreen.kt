package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            // Create a test job with proper payload
                            val testBase64Image = "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"
                            val testImages = listOf(testBase64Image)
                            val jobId = jobPollingService.createJob(
                                type = "doc",
                                payload = testImages
                            )
                            println("Created test job with ID: $jobId")
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
                            val jobId = jobPollingService.createJob(
                                type = "invalid_type",
                                payload = invalidPayload
                            )
                            println("Created invalid test job with ID: $jobId")
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
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
        
        // Jobs list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(jobs) { job ->
                JobCard(job = job)
            }
        }
    }
}

@Composable
fun JobCard(job: JobEntity) {
    var expanded by remember { mutableStateOf(false) }
    
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
                            text = "Result:",
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