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
import android.util.Base64
import cn.edu.sjtu.deepsleep.docusnap.util.CryptoUtil
import org.json.JSONObject

@Composable
fun JobStatusScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val jobPollingService = remember { JobPollingService(context) }
    val coroutineScope = rememberCoroutineScope()
    val privateKeyPem = context.assets.open("private_key.pem")
        .bufferedReader()
        .use { it.readText() }



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

                            val testBase64Image = context.assets.open("test_image_base64.txt")
                                .bufferedReader()
                                .use { it.readText() }

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
                JobCard(job = job, privateKeyPem = privateKeyPem)
            }
        }
    }
}

@Composable
fun JobCard(job: JobEntity, privateKeyPem: String) {
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
                // Show decrypted content for completed jobs
                if (job.status == "completed") {
                    val decrypted = tryDecryptJobResult(job, privateKeyPem)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Decrypted Content:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = decrypted ?: "[No decrypted content]",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
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

// Add this helper function after imports
private fun tryDecryptJobResult(job: JobEntity, privateKeyPem: String): String? {
    try {
        if (job.result == null || job.aesKey == null) return null
        // Debug: Print PEM info
        android.util.Log.d("JobStatusScreen", "PRIVATE_KEY first 100: ${privateKeyPem.take(100)}")
        android.util.Log.d("JobStatusScreen", "PRIVATE_KEY length: ${privateKeyPem.length}")
        android.util.Log.d("JobStatusScreen", "PRIVATE_KEY contains BEGIN: ${privateKeyPem.contains("BEGIN")}, END: ${privateKeyPem.contains("END")}")
        android.util.Log.d("JobStatusScreen", "AES key base64 length: ${job.aesKey.length}")
        android.util.Log.d("JobStatusScreen", "Result base64 length: ${job.result.length}")
        val privateKey = getPrivateKeyFromPem(privateKeyPem)
        val encryptedAesKeyBytes = Base64.decode(job.aesKey, Base64.NO_WRAP)
        val aesKeyBytes = rsaDecrypt(encryptedAesKeyBytes, privateKey)
        val decrypted = CryptoUtil.aesDecrypt(job.result, aesKeyBytes)
        return try {
            val json = JSONObject(decrypted)
            json.toString(2)
        } catch (e: Exception) {
            decrypted
        }
    } catch (e: Exception) {
        android.util.Log.e("JobStatusScreen", "Decryption failed: ${e.message}", e)
        return "[Decryption failed: ${e.message}\n" +
            "PEM first 100: ${privateKeyPem.take(100)}\n" +
            "PEM length: ${privateKeyPem.length}\n" +
            "PEM contains BEGIN: ${privateKeyPem.contains("BEGIN")}, END: ${privateKeyPem.contains("END")}" +
            "\nAES key base64 length: ${job.aesKey?.length ?: 0}\n" +
            "Result base64 length: ${job.result?.length ?: 0}\n" +
            "Stacktrace: ${e.stackTraceToString()}]"
    }
}

// Add these helpers after the above function
private fun getPrivateKeyFromPem(pem: String): java.security.PrivateKey {
    try {
        // Remove header and footer, and all whitespace
        val privateKeyPEM = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "") // Remove all whitespace including newlines
        
        android.util.Log.d("JobStatusScreen", "Cleaned PEM length: ${privateKeyPEM.length}")
        android.util.Log.d("JobStatusScreen", "Cleaned PEM first 50 chars: ${privateKeyPEM.take(50)}")
        
        val encoded = Base64.decode(privateKeyPEM, Base64.DEFAULT)
        android.util.Log.d("JobStatusScreen", "Decoded bytes length: ${encoded.size}")
        
        val keySpec = java.security.spec.PKCS8EncodedKeySpec(encoded)
        val kf = java.security.KeyFactory.getInstance("RSA")
        return kf.generatePrivate(keySpec)
    } catch (e: Exception) {
        android.util.Log.e("JobStatusScreen", "Failed to parse private key: ${e.message}", e)
        throw e
    }
}
private fun rsaDecrypt(data: ByteArray, privateKey: java.security.PrivateKey): ByteArray {
    val cipher = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
    cipher.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey)
    return cipher.doFinal(data)
} 