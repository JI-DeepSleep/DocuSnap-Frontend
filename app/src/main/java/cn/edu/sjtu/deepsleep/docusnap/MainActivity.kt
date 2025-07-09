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
                route = "image_processing?photoUri={photoUri}&source={source}",
                arguments = listOf(
                    navArgument("photoUri") { type = NavType.StringType; nullable = true },
                    navArgument("source") { type = NavType.StringType; defaultValue = "document" }
                )
            ) { backStackEntry ->
                val photoUri = backStackEntry.arguments?.getString("photoUri")
                val source = backStackEntry.arguments?.getString("source") ?: "document"
                ImageProcessingScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() },
                    photoUri = photoUri,
                    source = source
                )
            }
            
            composable(Screen.FormSelection2Fill.route) {
                FormSelectionScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable(Screen.FormAutoFill.route) {
                FormFillScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable(Screen.DocumentOverview.route) {
                DocumentOverviewScreen(
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
            
            composable(Screen.DocumentGallery.route) {
                DocumentImageScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { }
                )
            }
            
            composable(Screen.DocumentDetail.route) {
                DocumentDetailScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable(Screen.FormOverview.route) {
                FormOverviewScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { }
                )
            }
            
            composable(Screen.FormDetail.route) {
                FormDisplayScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}