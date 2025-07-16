package cn.edu.sjtu.deepsleep.docusnap.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import cn.edu.sjtu.deepsleep.docusnap.navigation.Screen

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object FormGallery : BottomNavItem(
        route = Screen.FormGallery.route,
        title = "Forms",
        icon = Icons.Default.Assignment
    )
    object Home : BottomNavItem(
        route = Screen.Home.route,
        title = "Home",
        icon = Icons.Default.Home
    )
    object DocumentGallery : BottomNavItem(
        route = Screen.DocumentGallery.route,
        title = "Documents",
        icon = Icons.Default.Description
    )
}

@Composable
fun BottomNavigation(navController: NavController) {
    val items = listOf(
        BottomNavItem.FormGallery,
        BottomNavItem.Home,
        BottomNavItem.DocumentGallery
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = {},
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                },
                alwaysShowLabel = false
            )
        }
    }
}