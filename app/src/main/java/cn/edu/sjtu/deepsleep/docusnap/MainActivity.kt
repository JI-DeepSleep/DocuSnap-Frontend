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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { BottomNavigation(navController = navController) }
    ) { innerPadding ->
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
                route = "document_detail?documentId={documentId}",
                arguments = listOf(
                    navArgument("documentId") { type = NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                val documentId = backStackEntry.arguments?.getString("documentId")
                DocumentDetailScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.navigate(Screen.DocumentGallery.route) },
                    documentId = documentId
                )
            }
            
            composable(Screen.FormGallery.route) {
                FormGalleryScreen(
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            
            composable(
                route = "form_detail?formId={formId}",
                arguments = listOf(
                    navArgument("formId") { type = NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                val formId = backStackEntry.arguments?.getString("formId")
                FormDetailScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.navigate(Screen.FormGallery.route) },
                    formId = formId
                )
            }
        }
    }
}