package com.example.medicalcare.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medicalcare.ui.components.NavTopBar
import com.example.medicalcare.ui.components.NavBottomBar
import com.example.medicalcare.ui.navigation.Routes
import com.example.medicalcare.ui.viewmodel.DoctorProfileViewModel
import com.example.medicalcare.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorProfileScreen(
    doctorUid: String,
    homeVM: HomeViewModel,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
    doctorVM: DoctorProfileViewModel = viewModel()
) {
    LaunchedEffect(doctorUid) {
        doctorVM.loadDoctor(doctorUid)
    }

    // Read state and subscribe the composable to it
    val homeState = homeVM.state
    val doctorState = doctorVM.state

    // Get current user data from state
    val currentUser = homeState.currentUser

    // Check if user's role is doctor or not
    val isDoctor = currentUser?.role == "DOCTOR"

    Scaffold(
        topBar = {
            // If your NavTopBar doesn't support back, just remove onBack usage
            NavTopBar(title = "Doctor Profile", onLogout = onLogout)
        },
        bottomBar = {
            NavBottomBar(
                isDoctor = isDoctor,
                // Doctor profile is a "child" of discovery, so keep discovery selected
                selectedRoute = Routes.DoctorDiscovery.route,
                onSelectRoute = { onNavigate(it) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when {
                doctorState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                doctorState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${doctorState.error}")
                    }
                }

                doctorState.doctor != null -> {
                    val doctor = doctorState.doctor
                    val specialization = doctor.specialization?.takeIf { it.isNotBlank() } ?: "General"
                    val bio = doctor.bio?.takeIf { it.isNotBlank() } ?: "No bio provided."

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        // Doctor name
                        Text(
                            text = doctor.name,
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(Modifier.height(8.dp))

                        // Doctor Specialization
                        Text(
                            text = specialization,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(16.dp))
                        Divider()
                        Spacer(Modifier.height(16.dp))

                        // Biography
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = bio,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = {
                                onNavigate(Routes.ChatThread.create(doctorUid))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Chat now")
                        }
                    }
                }
            }
        }
    }
}
