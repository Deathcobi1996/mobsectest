package com.example.medicalcare.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medicalcare.ui.components.NavBottomBar
import com.example.medicalcare.ui.components.NavTopBar
import com.example.medicalcare.ui.navigation.Routes
import com.example.medicalcare.ui.viewmodel.UserManagementViewModel
import com.example.medicalcare.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    homeVM: HomeViewModel,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
    userManagementVM: UserManagementViewModel = viewModel()
) {
    // Load current user and staff list
    LaunchedEffect(Unit) {
        userManagementVM.loadCurrentUser()
        userManagementVM.loadStaffList()
    }

    val homeState = homeVM.state
    val managementState = userManagementVM.state
    val isLoadingUser = managementState.currentUser == null
    val isDoctor = homeState.currentUser?.role == "DOCTOR"
    val isSuperUser = managementState.currentUser?.isSuperUser == true

    // Tab state for switching between "Add Staff" and "View Staff"
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = { NavTopBar(title = "Staff Management", onLogout = onLogout) },
        bottomBar = {
            NavBottomBar(
                isDoctor = isDoctor,
                selectedRoute = Routes.UserManagement.route,
                onSelectRoute = { onNavigate(it) },
                isSuperUser = homeState.currentUser?.isSuperUser == true
            )
        }
    ) { padding ->
        if (isLoadingUser) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (!isSuperUser) {
            // Show access denied message for non-superuser
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Access Denied",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Access Denied",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Only superuser doctors can access this page",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { onNavigate(Routes.Home.route) }) {
                        Text("Go to Home")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Tab Row
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Add Staff") },
                        icon = { Icon(Icons.Default.PersonAdd, "Add Staff") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("View Staff") },
                        icon = { Icon(Icons.Default.People, "View Staff") }
                    )
                }

                // Content based on selected tab
                when (selectedTab) {
                    0 -> AddStaffTab(userManagementVM, managementState)
                    1 -> ViewStaffTab(userManagementVM, managementState)
                }
            }
        }
    }
}

@Composable
fun AddStaffTab(
    viewModel: UserManagementViewModel,
    state: com.example.medicalcare.ui.viewmodel.UserManagementUiState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create New Staff Account",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Role Selection
        Text(
            text = "Select Role",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.role == "DOCTOR",
                onClick = { viewModel.onRoleChange("DOCTOR") },
                label = { Text("Doctor") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = "Doctor"
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider()

        // Basic Information
        Text(
            text = "Basic Information",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = state.name,
            onValueChange = { viewModel.onNameChange(it) },
            label = { Text("Full Name") },
            placeholder = { Text("Enter full name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Person, "Name")
            }
        )

        OutlinedTextField(
            value = state.email,
            onValueChange = { viewModel.onEmailChange(it) },
            label = { Text("Email") },
            placeholder = { Text("Enter email address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Email, "Email")
            }
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = { viewModel.onPasswordChange(it) },
            label = { Text("Password") },
            placeholder = { Text("Minimum 6 characters") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            leadingIcon = {
                Icon(Icons.Default.Lock, "Password")
            }
        )

        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = { viewModel.onConfirmPasswordChange(it) },
            label = { Text("Confirm Password") },
            placeholder = { Text("Re-enter password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            leadingIcon = {
                Icon(Icons.Default.Lock, "Confirm Password")
            }
        )

        // Professional Information
        if (state.role == "DOCTOR") {
            HorizontalDivider()

            Text(
                text = "Professional Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = state.specialization,
                onValueChange = { viewModel.onSpecializationChange(it) },
                label = { Text("Specialization ${if (state.role == "DOCTOR") "*" else ""}") },
                placeholder = { Text("e.g., Cardiology, Emergency Care") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.MedicalServices, "Specialization")
                }
            )

            OutlinedTextField(
                value = state.bio,
                onValueChange = { viewModel.onBioChange(it) },
                label = { Text("Bio (Optional)") },
                placeholder = { Text("Brief professional background...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 4
            )
        }

        // Error and Success Messages
        if (state.error != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        if (state.successMessage != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = state.successMessage,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Create Button
        Button(
            onClick = { viewModel.createStaffAccount(onSuccess = {}) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !state.loading
        ) {
            if (state.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.PersonAdd, "Create", modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Create ${state.role} Account",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ViewStaffTab(
    viewModel: UserManagementViewModel,
    state: com.example.medicalcare.ui.viewmodel.UserManagementUiState
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.loadingStaff) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.staffList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PeopleAlt,
                        contentDescription = "No Staff",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No staff members found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Staff Members (${state.staffList.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(state.staffList) { staff ->
                    StaffCard(staff = staff, onDelete = {
                        viewModel.deleteStaffAccount(staff.uid, onSuccess = {})
                    })
                }
            }
        }
    }
}

@Composable
fun StaffCard(
    staff: com.example.medicalcare.data.model.User,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (staff.role == "DOCTOR") Icons.Default.MedicalServices
                        else Icons.Default.LocalHospital,
                        contentDescription = staff.role,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = staff.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = staff.role,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = staff.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!staff.specialization.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = "Specialization",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = staff.specialization,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Staff Account?") },
            text = {
                Text("Are you sure you want to delete ${staff.name}'s account? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}