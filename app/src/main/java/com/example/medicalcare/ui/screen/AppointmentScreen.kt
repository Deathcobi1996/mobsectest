package com.example.medicalcare.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medicalcare.data.model.Appointment
import com.example.medicalcare.data.model.User
import com.example.medicalcare.ui.components.NavBottomBar
import com.example.medicalcare.ui.components.NavTopBar
import com.example.medicalcare.ui.navigation.Routes
import com.example.medicalcare.ui.viewmodel.AppointmentViewModel
import com.example.medicalcare.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentScreen(
    homeVM: HomeViewModel,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
    appointmentVM: AppointmentViewModel = viewModel()
) {
    val homeState = homeVM.state
    val user = homeState.currentUser
    val isDoctor = user?.role == "DOCTOR"
    val doctorId = user?.uid

    val state = appointmentVM.state

    LaunchedEffect(doctorId, isDoctor) {
        if (doctorId != null && isDoctor) {
            appointmentVM.startDoctor(doctorId)
        }
    }

    Scaffold(
        topBar = { NavTopBar(title = "Appointments", onLogout = onLogout) },
        bottomBar = {
            NavBottomBar(
                isDoctor = isDoctor,
                selectedRoute = Routes.Appointment.route,
                onSelectRoute = { onNavigate(it) },
                isSuperUser = homeState.currentUser?.isSuperUser == true
            )
        },
        floatingActionButton = {
            if (isDoctor) {
                FloatingActionButton(onClick = { appointmentVM.openCreateDialog() }) {
                    Icon(Icons.Default.Add, contentDescription = "New appointment")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                doctorId == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Not logged in.")
                    }
                }
                !isDoctor -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Only doctors can manage appointments.")
                    }
                }
                state.isLoading && state.appointments.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.error}")
                    }
                }
                else -> {
                    Column(Modifier.fillMaxSize().padding(12.dp)) {
                        Spacer(Modifier.height(12.dp))

                        if (state.appointments.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No appointments yet. Tap + to create one.")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                items(state.appointments, key = { it.id }) { appt ->
                                    AppointmentRow(
                                        appt = appt,
                                        patients = state.patients,
                                        onEdit = { appointmentVM.openEditDialog(appt) },
                                        onDelete = { appointmentVM.deleteAppointment(appt.id) }
                                    )
                                    Spacer(Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }
            }

            if (state.showCreateDialog) {
                AppointmentDialog(
                    title = "New Appointment",
                    patients = state.patients,
                    selectedPatientId = state.selectedPatientId,
                    selectedDateMillis = state.selectedDateMillis,
                    selectedHour = state.selectedHour,
                    selectedMinute = state.selectedMinute,
                    onDateChange = appointmentVM::setSelectedDateMillis,
                    onTimeChange = appointmentVM::setSelectedTime,
                    notesInput = state.notesInput,
                    error = state.error,
                    onSelectPatient = appointmentVM::setSelectedPatientId,
                    onNotesChange = appointmentVM::setNotesInput,
                    onDismiss = appointmentVM::closeDialogs,
                    onConfirm = { if (doctorId != null) appointmentVM.createAppointment(doctorId) },
                    confirmText = "Create"
                )
            }

            if (state.showEditDialog) {
                AppointmentDialog(
                    title = "Edit Appointment",
                    patients = state.patients,
                    selectedPatientId = state.selectedPatientId,
                    selectedDateMillis = state.selectedDateMillis,
                    selectedHour = state.selectedHour,
                    selectedMinute = state.selectedMinute,
                    onDateChange = appointmentVM::setSelectedDateMillis,
                    onTimeChange = appointmentVM::setSelectedTime,
                    notesInput = state.notesInput,
                    error = state.error,
                    onSelectPatient = appointmentVM::setSelectedPatientId,
                    onNotesChange = appointmentVM::setNotesInput,
                    onDismiss = appointmentVM::closeDialogs,
                    onConfirm = appointmentVM::saveEdits,
                    confirmText = "Save"
                )
            }
        }
    }
}

@Composable
private fun AppointmentRow(
    appt: Appointment,
    patients: List<User>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dt = appt.scheduledAt?.toDate()
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dtText = dt?.let { fmt.format(it) } ?: "—"

    val patientName = patients.firstOrNull { it.uid == appt.patientId }?.name
        ?: appt.patientId

    Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(patientName, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(dtText, style = MaterialTheme.typography.bodyMedium)
                if (appt.notes.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = appt.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppointmentDialog(
    title: String,
    patients: List<User>,
    selectedPatientId: String,
    selectedDateMillis: Long?,
    selectedHour: Int,
    selectedMinute: Int,
    onDateChange: (Long?) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    notesInput: String,
    error: String?,
    onSelectPatient: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String
) {
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    val selectedName =
                        patients.firstOrNull { it.uid == selectedPatientId }?.name ?: "Select patient"

                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Patient") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = MenuAnchorType.PrimaryEditable,
                                enabled = true
                            )
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        patients.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = {
                                    onSelectPatient(p.uid)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

                val selectedDateText = remember(selectedDateMillis) {
                    if (selectedDateMillis != null) {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = selectedDateMillis
                        dateFmt.format(cal.time)
                    } else {
                        "Select date"
                    }
                }

                val selectedTimeText = remember(selectedHour, selectedMinute) {
                    "%02d:%02d".format(selectedHour, selectedMinute)
                }

                var showDatePicker by remember { mutableStateOf(false) }
                var showTimePicker by remember { mutableStateOf(false) }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) { Text(selectedDateText) }

                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) { Text(selectedTimeText) }
                }

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = selectedDateMillis
                    )

                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                onDateChange(datePickerState.selectedDateMillis)
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                if (showTimePicker) {
                    val timeState = rememberTimePickerState(
                        initialHour = selectedHour,
                        initialMinute = selectedMinute,
                        is24Hour = true
                    )

                    AlertDialog(
                        onDismissRequest = { showTimePicker = false },
                        title = { Text("Select time") },
                        text = { TimePicker(state = timeState) },
                        confirmButton = {
                            TextButton(onClick = {
                                onTimeChange(timeState.hour, timeState.minute)
                                showTimePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = onNotesChange,
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}