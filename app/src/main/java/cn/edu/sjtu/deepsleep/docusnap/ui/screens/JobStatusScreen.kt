package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    
    // Collect jobs from database
    LaunchedEffect(Unit) {
        try {
            val database = AppDatabase.getInstance(context)
            val jobDao = database.jobDao()
            jobDao.getAllJobs().collect { jobList ->
                jobs = jobList
            }
        } catch (e: Exception) {
            // Handle database errors gracefully
            println("Error loading jobs: ${e.message}")
        }
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
            Button(onClick = onBackClick) {
                Text("Back")
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
                            // Create a test job
                            val jobId = jobPollingService.createJob(
                                type = "doc",
                                sha256 = "test_sha256_${System.currentTimeMillis()}",
                                hasContent = true,
                                content = "test_content",
                                aesKey = "test_aes_key"
                            )
                            println("Created test job with ID: $jobId")
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
                            val deletedCount = jobPollingService.cleanupOldJobs()
                            cleanupMessage = "Cleaned up $deletedCount old jobs"
                            println("Cleanup completed: $deletedCount jobs deleted")
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Job #${job.id}",
                    style = MaterialTheme.typography.titleMedium
                )
                StatusChip(status = job.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Type: ${job.type}")
            Text("SHA256: ${job.sha256.take(16)}...")
            Text("Created: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(job.createdAt))}")
            
            if (job.errorDetail != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Error: ${job.errorDetail}",
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            if (job.result != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Result: ${job.result.take(50)}...",
                    style = MaterialTheme.typography.bodySmall
                )
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