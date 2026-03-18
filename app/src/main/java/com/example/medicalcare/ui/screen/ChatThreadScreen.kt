package com.example.medicalcare.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medicalcare.ui.components.NavBottomBar
import com.example.medicalcare.ui.navigation.Routes
import com.example.medicalcare.ui.viewmodel.ChatThreadViewModel
import com.example.medicalcare.ui.viewmodel.HomeViewModel
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.LocationOn
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    otherUid: String,
    homeVM: HomeViewModel,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    chatVM: ChatThreadViewModel = viewModel()
) {
    val currentUser = homeVM.state.currentUser
    val currentUid = currentUser?.uid
    val isDoctor = currentUser?.role == "DOCTOR"

    val chatState = chatVM.state

    val chatContext = LocalContext.current
    val fusedLocation = remember { LocationServices.getFusedLocationProviderClient(chatContext) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) return@rememberLauncherForActivityResult

        @SuppressLint("MissingPermission")
        fusedLocation.lastLocation.addOnSuccessListener { loc ->
            if (currentUid == null) return@addOnSuccessListener

            if (loc != null) {
                chatVM.sendLocation(currentUid, loc.latitude, loc.longitude)
            } else {
                // ✅ fallback: actively request a fresh location fix
                val cts = CancellationTokenSource()
                fusedLocation.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cts.token
                ).addOnSuccessListener { cur ->
                    if (cur != null) {
                        chatVM.sendLocation(currentUid, cur.latitude, cur.longitude)
                    }
                }
            }
        }
    }

    LaunchedEffect(currentUid, otherUid) {
        if (currentUid != null) chatVM.startChat(currentUid, otherUid)
    }

    LaunchedEffect(chatState.messages.size, currentUid) {
        if (currentUid != null) chatVM.markRead(currentUid)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chatState.otherUser?.name ?: "Chat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onLogout) { Text("Logout") }
                }
            )
        },
        bottomBar = {
            NavBottomBar(
                isDoctor = isDoctor,
                selectedRoute = Routes.Chat.route,
                onSelectRoute = { onNavigate(it) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                currentUid == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Not logged in.")
                    }
                }
                chatState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                chatState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${chatState.error}")
                    }
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            items(chatState.messages, key = { it.id }) { msg ->
                                val isMine = msg.senderId == currentUid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium) {
                                        val isLocation = msg.mapsUrl != null && msg.lat != null && msg.lng != null

                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(text = msg.text)

                                            if (isLocation) {
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    text = "Open in Maps",
                                                    modifier = Modifier.clickable {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(msg.mapsUrl))
                                                        chatContext.startActivity(intent)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }

                        Divider()

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = "Share location")
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = chatState.input,
                                onValueChange = chatVM::setInput,
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Type a message...") }
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { chatVM.sendMessage(currentUid) }) {
                                Text("Send")
                            }
                        }
                    }
                }
            }
        }
    }
}
