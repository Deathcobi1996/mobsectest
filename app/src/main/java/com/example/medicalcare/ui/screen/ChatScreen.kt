package com.example.medicalcare.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medicalcare.ui.components.NavTopBar
import com.example.medicalcare.ui.components.NavBottomBar
import com.example.medicalcare.ui.navigation.Routes
import com.example.medicalcare.ui.viewmodel.ChatViewModel
import com.example.medicalcare.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    homeVM: HomeViewModel,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
    chatVM: ChatViewModel = viewModel()
) {
    val homeState = homeVM.state
    val user = homeState.currentUser
    val isDoctor = user?.role == "DOCTOR"

    LaunchedEffect(user?.uid) {
        user?.uid?.let { chatVM.start(it) }
    }

    val state = chatVM.state

    Scaffold(
        topBar = { NavTopBar(title = "Chat", onLogout = onLogout) },
        bottomBar = {
            NavBottomBar(
                isDoctor = isDoctor,
                selectedRoute = Routes.Chat.route,
                onSelectRoute = { onNavigate(it) },
                isSuperUser = homeState.currentUser?.isSuperUser == true
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.error}")
                    }
                }
                state.chats.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No chats yet.")
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(state.chats, key = { it.conversationId }) { chat ->
                            val goThread = { onNavigate(Routes.ChatThread.create(chat.otherUserId)) }

                            ListItem(
                                headlineContent = { Text(chat.otherUserName) },
                                supportingContent = {
                                    Text(
                                        text = if (chat.lastMessageText.isBlank()) "(No messages yet)" else chat.lastMessageText,
                                        maxLines = 1
                                    )
                                },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (chat.unreadCount > 0) {
                                            AssistChip(
                                                onClick = goThread,
                                                label = { Text("${chat.unreadCount}") }
                                            )
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        Icon(Icons.Filled.ChevronRight, contentDescription = "Open")
                                    }
                                },
                                modifier = Modifier.clickable(onClick = goThread)
                            )
                        }
                    }
                }
            }
        }
    }
}