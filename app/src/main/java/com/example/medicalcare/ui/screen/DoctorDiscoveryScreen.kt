package com.example.medicalcare.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medicalcare.ui.components.NavBottomBar
import com.example.medicalcare.ui.components.NavTopBar
import com.example.medicalcare.ui.navigation.Routes
import com.example.medicalcare.ui.viewmodel.DoctorDiscoveryViewModel
import com.example.medicalcare.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDiscoveryScreen(
    homeVM: HomeViewModel,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
    doctorVM: DoctorDiscoveryViewModel = viewModel()
) {
    // Read state and subscribe the composable to it
    val homeState = homeVM.state
    val doctorState = doctorVM.state

    // Get current user data from state
    val currentUser = homeState.currentUser

    // Check if user's role is doctor or not
    val isDoctor = currentUser?.role == "DOCTOR"

    // Load doctors only when we know who the current user is
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            doctorVM.loadDoctors(currentUserUid = uid)
        }
    }

    Scaffold(
        topBar = { NavTopBar(title = "Discover Your Doctors", onLogout = onLogout) },
        bottomBar = {
            NavBottomBar(
                isDoctor = isDoctor,
                selectedRoute = Routes.DoctorDiscovery.route,
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

                doctorState.doctors.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No doctors found.")
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                    ) {
                        items(doctorState.doctors, key = { it.uid }) { doctor ->
                            val specializationText =
                                doctor.specialization?.takeIf { it.isNotBlank() } ?: "General"

                            // Navigate to individual doctor profile
                            val goToDoctorDetails = {
                                onNavigate(Routes.DoctorProfile.create(doctor.uid))
                            }

                            ListItem(
                                headlineContent = { Text(doctor.name) },
                                supportingContent = { Text(specializationText) },
                                trailingContent = {
                                    IconButton(onClick = goToDoctorDetails) {
                                        Icon(
                                            imageVector = Icons.Filled.ChevronRight,
                                            contentDescription = "View more"
                                        )
                                    }
                                },
                                modifier = Modifier.clickable(onClick = goToDoctorDetails)
                            )
                        }
                    }
                }
            }
        }
    }
}