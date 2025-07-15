package cn.edu.sjtu.deepsleep.docusnap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import cn.edu.sjtu.deepsleep.docusnap.data.SettingsManager

@Composable
fun PinVerificationScreen(
    onPinVerified: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    
    var pin by remember { mutableStateOf("") }
    var storedPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    
    // Load stored PIN
    LaunchedEffect(Unit) {
        settingsManager.settings.collect { settings ->
            storedPin = settings.pin
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Icon/Logo
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title
        Text(
            text = "Enter PIN",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Enter your PIN to access DocuSnap",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // PIN Input
        OutlinedTextField(
            value = pin,
            onValueChange = { 
                pin = it
                showError = false
            },
            label = { Text("PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = showError
        )
        
        if (showError) {
            Text(
                text = "Incorrect PIN. Please try again.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Verify Button
        Button(
            onClick = {
                if (pin == storedPin) {
                    onPinVerified()
                } else {
                    showError = true
                    pin = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = pin.isNotEmpty()
        ) {
            Text("Verify PIN")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Forgot PIN option (in a real app, this would reset PIN protection)
        TextButton(
            onClick = { /* TODO: Implement forgot PIN functionality */ }
        ) {
            Text("Forgot PIN?")
        }
    }
} 