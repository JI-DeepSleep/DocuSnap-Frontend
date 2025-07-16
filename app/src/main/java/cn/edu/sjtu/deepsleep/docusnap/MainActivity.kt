package cn.edu.sjtu.deepsleep.docusnap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cn.edu.sjtu.deepsleep.docusnap.data.SettingsManager
import cn.edu.sjtu.deepsleep.docusnap.navigation.Screen
import cn.edu.sjtu.deepsleep.docusnap.ui.components.BottomNavigation
import cn.edu.sjtu.deepsleep.docusnap.ui.screens.*
import cn.edu.sjtu.deepsleep.docusnap.ui.theme.DocuSnapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DocuSnapTheme {
                DocuSnapApp()
            }
        }
    }
}

@Composable
fun DocuSnapApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    
    var pinProtectionEnabled by remember { mutableStateOf(false) }
    var isPinVerified by remember { mutableStateOf(false) }
    
    // Check PIN protection status
    LaunchedEffect(Unit) {
        settingsManager.settings.collect { settings ->
            pinProtectionEnabled = settings.pinProtectionEnabled
        }
    }
    
    // Determine start destination based on PIN protection
    val startDestination = if (pinProtectionEnabled && !isPinVerified) {
        Screen.PinVerification.route
    } else {
        Screen.Home.route
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { 
            if (!pinProtectionEnabled || isPinVerified) {
                BottomNavigation(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            
            composable(Screen.Search.route) {
                SearchScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable(
                route = "camera_capture?source={source}",
                arguments = listOf(
                    navArgument("source") { type = NavType.StringType; defaultValue = "document" }
                )
            ) { backStackEntry ->
                val source = backStackEntry.arguments?.getString("source") ?: "document"
                CameraCaptureScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() },
                    source = source
                )
            }
            
            composable(
                route = "local_media?source={source}",
                arguments = listOf(
                    navArgument("source") { type = NavType.StringType; defaultValue = "document" }
                )
            ) { backStackEntry ->
                val source = backStackEntry.arguments?.getString("source") ?: "document"
                LocalMediaScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() },
                    source = source
                )
            }
            
            composable(
                route = "image_processing?photoUris={photoUris}&source={source}",
                arguments = listOf(
                    navArgument("photoUris") { type = NavType.StringType; nullable = true },
                    navArgument("source") { type = NavType.StringType; defaultValue = "document" }
                )
            ) { backStackEntry ->
                val photoUris = backStackEntry.arguments?.getString("photoUris")
                val source = backStackEntry.arguments?.getString("source") ?: "document"
                ImageProcessingScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() },
                    photoUris = photoUris,
                    source = source
                )
            }
            
            composable(Screen.DocumentGallery.route) {
                DocumentGalleryScreen(
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            
            composable(
                route = "document_detail?documentId={documentId}&fromImageProcessing={fromImageProcessing}&photoUris={photoUris}",
                arguments = listOf(
                    navArgument("documentId") { type = NavType.StringType; nullable = true },
                    navArgument("fromImageProcessing") { type = NavType.BoolType; defaultValue = false },
                    navArgument("photoUris") { type = NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                val photoUris = backStackEntry.arguments?.getString("photoUris")
                val documentId = backStackEntry.arguments?.getString("documentId")
                val fromImageProcessing = backStackEntry.arguments?.getBoolean("fromImageProcessing") ?: false
                DocumentDetailScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { 
                        if (fromImageProcessing) {
                            navController.navigate(Screen.DocumentGallery.route)
                        } else {
                            navController.popBackStack()
                        }
                    },
                    documentId = documentId,
                    fromImageProcessing = fromImageProcessing,
                    photoUris = photoUris
                )
            }
            
            composable(Screen.FormGallery.route) {
                FormGalleryScreen(
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            composable(
                // [1. MODIFIED] Added "&photoUris={photoUris}" to the route pattern.
                route = "form_detail?formId={formId}&fromImageProcessing={fromImageProcessing}&photoUris={photoUris}",
                arguments = listOf(
                    navArgument("formId") { type = NavType.StringType; nullable = true },
                    navArgument("fromImageProcessing") { type = NavType.BoolType; defaultValue = false },
                    navArgument("photoUris") { type = NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                // [3. ADDED] Extracted the new photoUris argument from the navigation entry.
                val photoUris = backStackEntry.arguments?.getString("photoUris")
                val formId = backStackEntry.arguments?.getString("formId")
                val fromImageProcessing = backStackEntry.arguments?.getBoolean("fromImageProcessing") ?: false

                FormDetailScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = {
                        if (fromImageProcessing) {
                            navController.navigate(Screen.FormGallery.route)
                        } else {
                            navController.popBackStack()
                        }
                    },
                    formId = formId,
                    // [4. ADDED] Passed the new photoUris argument to the FormDetailScreen.
                    photoUris = photoUris
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable(Screen.PinVerification.route) {
                PinVerificationScreen(
                    onPinVerified = { 
                        isPinVerified = true
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.PinVerification.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}