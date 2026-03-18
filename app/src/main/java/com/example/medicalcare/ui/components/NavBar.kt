package com.example.medicalcare.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

data class BottomItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavTopBar(
    title: String,
    onLogout: (() -> Unit)? = null
) {
    TopAppBar(
        title = { Text(title) },
        actions = {
            if (onLogout != null) {
                TextButton(onClick = onLogout) { Text("Logout") }
            }
        }
    )
}

@Composable
fun NavBottomBar(
    isDoctor: Boolean,
    selectedRoute: String,
    onSelectRoute: (String) -> Unit,
    isSuperUser: Boolean = false
) {
    val items = buildList {
        add(BottomItem("Home", Icons.Filled.Home, "home"))
        add(BottomItem("Doctors", Icons.Filled.MedicalServices, "doctordiscovery"))
        add(BottomItem("Chat", Icons.Filled.ChatBubbleOutline, "chat"))
        if (isDoctor) add(BottomItem("Appointment", Icons.Filled.CalendarToday, "appointment"))
        if (isSuperUser) add(BottomItem("Staff", Icons.Filled.AdminPanelSettings, "usermanagement"))
        add(BottomItem("Settings", Icons.Filled.Settings, "settings"))
    }

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = selectedRoute == item.route,
                onClick = { onSelectRoute(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
