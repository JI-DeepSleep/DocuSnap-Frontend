package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.edu.sjtu.deepsleep.docusnap.data.AppConstants
import cn.edu.sjtu.deepsleep.docusnap.data.AppSettings
import cn.edu.sjtu.deepsleep.docusnap.data.SettingsManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()
    
    var pinProtectionEnabled by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var backendUrl by remember { mutableStateOf(AppConstants.DEFAULT_BACKEND_URL) }
    var backendPublicKey by remember { mutableStateOf(AppConstants.DEFAULT_BACKEND_PUBLIC_KEY) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    
    // Load current settings
    LaunchedEffect(Unit) {
        settingsManager.settings.collect { settings ->
            pinProtectionEnabled = settings.pinProtectionEnabled
            pin = settings.pin
            confirmPin = settings.pin
            backendUrl = settings.backendUrl
            backendPublicKey = settings.backendPublicKey
        }
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
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
        }

        Spacer(modifier = Modifier.height(24.dp))

        // PIN Protection Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PIN Protection",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Require PIN to access the app",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = pinProtectionEnabled,
                        onCheckedChange = { 
                            pinProtectionEnabled = it
                            if (!it) {
                                pin = ""
                                confirmPin = ""
                            }
                        }
                    )
                }

                if (pinProtectionEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it },
                        label = { Text("PIN (4-6 digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it },
                        label = { Text("Confirm PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = pin.isNotEmpty() && confirmPin.isNotEmpty() && pin != confirmPin
                    )
                    
                    if (pin.isNotEmpty() && confirmPin.isNotEmpty() && pin != confirmPin) {
                        Text(
                            text = "PINs do not match",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Backend Configuration Section
        Card(
            modifier = Modifier.fillMaxWidth()
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
                        text = "Backend Configuration",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.Help, contentDescription = "Help")
                    }
                }
                
                Text(
                    text = "Custom backend server URL",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = backendUrl,
                    onValueChange = { backendUrl = it },
                    label = { Text("Backend URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = backendPublicKey,
                    onValueChange = { backendPublicKey = it },
                    label = { Text("Backend Public Key") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                        .verticalScroll(rememberScrollState()),
                    singleLine = false
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save Button
        Button(
            onClick = { 
                scope.launch {
                    val newSettings = AppSettings(
                        pinProtectionEnabled = pinProtectionEnabled,
                        pin = if (pinProtectionEnabled) pin else "",
                        backendUrl = backendUrl,
                        backendPublicKey = backendPublicKey
                    )
                    settingsManager.updateSettings(newSettings)
                    showSaveSuccess = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !pinProtectionEnabled || (pin.isNotEmpty() && confirmPin.isNotEmpty() && pin == confirmPin)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Settings")
        }
    }

    // Help Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Backend Configuration") },
            text = { 
                Text(
                    "You can deploy your own backend server for enhanced security and privacy. " +
                    "Our backend code is open source and available at: ${AppConstants.BACKEND_GITHUB_URL}\n\n" +
                    "By default, the app uses our hosted backend service. " +
                    "To use your own server, enter the full URL including the protocol (e.g., https://your-server.com)."
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Save Success Dialog
    if (showSaveSuccess) {
        AlertDialog(
            onDismissRequest = { 
                showSaveSuccess = false
                onBackClick()
            },
            title = { Text("Settings Saved") },
            text = { Text("Your settings have been saved successfully.") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showSaveSuccess = false
                        onBackClick()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}