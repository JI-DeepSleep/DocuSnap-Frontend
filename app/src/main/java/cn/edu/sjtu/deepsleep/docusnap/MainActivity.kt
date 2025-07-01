package cn.edu.sjtu.deepsleep.docusnap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cn.edu.sjtu.deepsleep.docusnap.navigation.Screen
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
    
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
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
            
            composable(Screen.CameraCapture.route) {
                CameraCaptureScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable(Screen.LocalMedia.route) {
                LocalMediaScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable(Screen.ImageProcessing.route) {
                ImageProcessingScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable(Screen.UploadedFormSelection.route) {
                UploadedFormSelectionScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable(Screen.FillForm.route) {
                FillFormScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable(Screen.AccessDocument.route) {
                AccessDocumentScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable(Screen.DocumentTextualInfo.route) {
                DocumentTextualInfoScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable(Screen.DocumentImage.route) {
                DocumentImageScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable(Screen.DocumentDisplay.route) {
                DocumentDisplayScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable(Screen.AccessForm.route) {
                AccessFormScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable(Screen.FormDisplay.route) {
                FormDisplayScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}