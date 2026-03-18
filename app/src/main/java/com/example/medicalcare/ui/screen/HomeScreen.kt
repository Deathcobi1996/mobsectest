package com.example.medicalcare.ui.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.medicalcare.ui.components.NavBottomBar
import com.example.medicalcare.ui.components.NavTopBar
import com.example.medicalcare.ui.navigation.Routes
import com.example.medicalcare.ui.viewmodel.AppointmentDisplay
import com.example.medicalcare.ui.viewmodel.DashboardChatPreview
import com.example.medicalcare.ui.viewmodel.HomeViewModel


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeVM: HomeViewModel,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    // Create a compose-observable state
    val homeState = homeVM.state

    // Run loadHome() to fetch current user data once.
    LaunchedEffect(Unit) {
        homeVM.loadHome()
    }

    // Get current user data from state
    val user = homeState.currentUser

    // Check if user's role is doctor or not
    val isDoctor = user?.role == "DOCTOR"

    // Get user's name
    val userName = user?.name ?: "User"

    Scaffold(
        topBar = { NavTopBar(title = "Welcome, $userName", onLogout = onLogout) },
        bottomBar = {
            NavBottomBar(
                isDoctor = isDoctor,
                selectedRoute = Routes.Home.route,
                onSelectRoute = { onNavigate(it) },
                isSuperUser = homeState.currentUser?.isSuperUser == true
            )
        }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                homeState.loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                user == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Not logged in.")
                    }
                }

                !isDoctor -> {
                    // Patient Home
                    PatientHome(
                        appointments = homeState.appointments,
                        onViewAllAppointments = { onNavigate(Routes.Appointment.route) })
                }

                else -> {
                    // Doctor Dashboard
                    DoctorDashboard(
                        chats = homeState.incomingChats,
                        upcomingAppointments = homeState.upcomingAppointments,
                        onOpenChat = { otherUid ->
                            onNavigate(Routes.ChatThread.create(otherUid))
                        },
                        onViewAllChats = { onNavigate(Routes.Chat.route) },
                        onViewAllAppointments = { onNavigate(Routes.Appointment.route) })
                }
            }

            if (homeState.error != null) {
                Text(
                    text = homeState.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PatientHome(
    appointments: List<AppointmentDisplay>, onViewAllAppointments: () -> Unit
) {
    val upcoming = appointments.filter { !it.isOver }
    val recent = appointments.filter { it.isOver }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Schedule",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onViewAllAppointments) {
                Text("View all")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (appointments.isEmpty()) {
            Text("No appointments found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                if (upcoming.isNotEmpty()) {
                    item {
                        Text(
                            text = "Upcoming Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(upcoming, key = { "up_${it.appointment.id}" }) { appt ->
                        AppointmentCardSimple(appt)
                    }
                }

                if (recent.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Recent Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(recent, key = { "rect_${it.appointment.id}" }) { appt ->
                        AppointmentCardSimple(appt)
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorDashboard(
    chats: List<DashboardChatPreview>,
    upcomingAppointments: List<AppointmentDisplay>,
    onOpenChat: (String) -> Unit,
    onViewAllChats: () -> Unit,
    onViewAllAppointments: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // SECTION: Incoming messages
        SectionHeader(
            icon = { Icon(Icons.Default.MarkChatUnread, contentDescription = null) },
            title = "Incoming Patient Messages",
            actionText = "View all",
            onAction = onViewAllChats
        )

        Spacer(Modifier.height(8.dp))

        if (chats.isEmpty()) {
            Text(
                text = "No active chats yet.", color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column {
                    chats.take(5).forEachIndexed { idx, chat ->
                        ChatPreviewRow(
                            chat = chat, onClick = { onOpenChat(chat.otherUserId) })
                        if (idx != chats.take(5).lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // SECTION: Upcoming appointments
        SectionHeader(
            icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
            title = "Upcoming Appointments",
            actionText = "View all",
            onAction = onViewAllAppointments
        )

        Spacer(Modifier.height(8.dp))

        if (upcomingAppointments.isEmpty()) {
            Text(
                text = "No upcoming appointments.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(upcomingAppointments.take(5), key = { it.appointment.id }) { appt ->
                    AppointmentCardSimple(appt)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: @Composable () -> Unit, title: String, actionText: String, onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onAction) { Text(actionText) }
    }
}

@Composable
private fun ChatPreviewRow(
    chat: DashboardChatPreview, onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
        Text(
            text = chat.otherUserName, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }, supportingContent = {
        Text(
            text = chat.lastMessageText.ifBlank { "(No messages yet)" },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }, trailingContent = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (chat.unreadCount > 0) {
                AssistChip(
                    onClick = onClick, label = { Text("${chat.unreadCount}") })
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = "Open")
        }
    }, modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun AppointmentCardSimple(appointment: AppointmentDisplay) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (appointment.isOver)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${appointment.counterpartLabel}: ${appointment.counterpartName} - ${appointment.specialization}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Type: Follow-up Consultation",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Scheduled: ${appointment.dateText} ${appointment.timeText}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Duration: 30 mins",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (appointment.isOver) "Status: Over" else "Status: Upcoming",
                style = MaterialTheme.typography.bodyMedium,
                color = if (appointment.isOver) Color.Gray else MaterialTheme.colorScheme.primary
            )
            if (appointment.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Notes: ${appointment.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
