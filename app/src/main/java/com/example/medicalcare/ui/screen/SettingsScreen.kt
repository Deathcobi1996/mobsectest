package com.example.medicalcare.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medicalcare.ui.components.NavBottomBar
import com.example.medicalcare.ui.components.NavTopBar
import com.example.medicalcare.ui.navigation.Routes
import com.example.medicalcare.ui.viewmodel.SettingsViewModel
import com.example.medicalcare.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    homeVM: HomeViewModel,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
    settingsVM: SettingsViewModel = viewModel()
) {
    // Load user profile on first composition
    LaunchedEffect(Unit) {
        settingsVM.loadUserProfile()
    }

    // Read state and subscribe the composable to it
    val homeState = homeVM.state
    val settingsState = settingsVM.state

    // Get current user data from state
    val user = settingsState.user

    // Check if user's role is doctor or not
    val isDoctor = user?.role == "DOCTOR"

    // Navigation Bar
    Scaffold(
        topBar = { NavTopBar(title = "Adjust Your Settings", onLogout = onLogout) },
        bottomBar = {
            NavBottomBar(
                isDoctor = isDoctor,
                selectedRoute = Routes.Settings.route,
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
            if (settingsState.loading && user == null) {
                // Initial loading
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (user != null) {
                // Profile form
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Text(
                        text = "Edit Your Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // User Info Card (Read-only)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Email",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Role",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Role",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = user.role,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Editable Fields
                    Text(
                        text = "Editable Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Name field
                    OutlinedTextField(
                        value = settingsState.name,
                        onValueChange = { settingsVM.onNameChange(it) },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !settingsState.loading,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Name"
                            )
                        }
                    )

                    // Doctor-specific fields
                    if (isDoctor) {
                        OutlinedTextField(
                            value = settingsState.specialization,
                            onValueChange = { settingsVM.onSpecializationChange(it) },
                            label = { Text("Specialization") },
                            placeholder = { Text("e.g., Cardiologist, Pediatrician") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !settingsState.loading,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.MedicalServices,
                                    contentDescription = "Specialization"
                                )
                            }
                        )

                        OutlinedTextField(
                            value = settingsState.bio,
                            onValueChange = { settingsVM.onBioChange(it) },
                            label = { Text("Bio") },
                            placeholder = { Text("Tell patients about yourself and your experience...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            maxLines = 6,
                            enabled = !settingsState.loading
                        )
                    }

                    // Error message (matching LoginScreen pattern)
                    if (settingsState.error != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = settingsState.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Success message
                    if (settingsState.successMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = settingsState.successMessage,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save button
                    Button(
                        onClick = {
                            settingsVM.updateProfile(
                                onSuccess = {
                                    // Refresh HomeViewModel after successful update
                                    homeVM.refreshUserProfile()
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !settingsState.loading
                    ) {
                        if (settingsState.loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = "Save Changes",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                // Error state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Failed to load profile",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { settingsVM.loadUserProfile() }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}