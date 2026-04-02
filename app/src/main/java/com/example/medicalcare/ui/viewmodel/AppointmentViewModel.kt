package com.example.medicalcare.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalcare.data.model.Appointment
import com.example.medicalcare.data.model.User
import com.example.medicalcare.data.repository.AppointmentRepository
import com.example.medicalcare.data.repository.UserRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

data class AppointmentUiState(
    val isLoading: Boolean = false,
    val error: String? = null,

    val appointments: List<Appointment> = emptyList(),
    val patients: List<User> = emptyList(),

    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editing: Appointment? = null,

    val selectedPatientId: String = "",
    val selectedDateMillis: Long? = null,
    val selectedHour: Int = 9,
    val selectedMinute: Int = 0,

    val notesInput: String = ""
)

class AppointmentViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val appointmentRepository: AppointmentRepository = AppointmentRepository()
) : ViewModel() {

    var state by mutableStateOf(AppointmentUiState())
        private set

    private var appointmentsJob: Job? = null

    fun startDoctor(doctorId: String) {
        observeDoctorAppointments(doctorId)
        loadPatients()
    }

    private fun observeDoctorAppointments(doctorId: String) {
        appointmentsJob?.cancel()
        appointmentsJob = viewModelScope.launch {
            try {
                state = state.copy(isLoading = true, error = null)
                appointmentRepository.observeDoctorAppointments(doctorId).collectLatest { list ->
                    state = state.copy(isLoading = false, appointments = list)
                }
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Failed to load appointments")
            }
        }
    }

    private fun loadPatients() {
        viewModelScope.launch {
            try {
                val users = userRepository.getAllUsers()
                val patients = users.filter { it.role.equals("PATIENT", ignoreCase = true) }
                state = state.copy(patients = patients)
            } catch (e: Exception) {
                state = state.copy(error = e.message ?: "Failed to load patients")
            }
        }
    }

    fun openCreateDialog() {
        val cal = Calendar.getInstance()

        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val todayMillis = cal.timeInMillis

        val now = Calendar.getInstance()

        state = state.copy(
            showCreateDialog = true,
            showEditDialog = false,
            editing = null,
            selectedPatientId = "",
            selectedDateMillis = todayMillis,
            selectedHour = now.get(Calendar.HOUR_OF_DAY),
            selectedMinute = now.get(Calendar.MINUTE),
            notesInput = "",
            error = null
        )
    }

    fun openEditDialog(appt: Appointment) {
        val cal = Calendar.getInstance()
        appt.scheduledAt?.toDate()?.let { cal.time = it }

        val dateCal = Calendar.getInstance().apply {
            time = cal.time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        state = state.copy(
            showEditDialog = true,
            showCreateDialog = false,
            editing = appt,
            selectedPatientId = appt.patientId,
            selectedDateMillis = dateCal.timeInMillis,
            selectedHour = cal.get(Calendar.HOUR_OF_DAY),
            selectedMinute = cal.get(Calendar.MINUTE),
            notesInput = appt.notes,
            error = null
        )
    }

    fun closeDialogs() {
        state = state.copy(
            showCreateDialog = false,
            showEditDialog = false,
            editing = null,
            error = null
        )
    }

    fun setSelectedPatientId(id: String) {
        if (id.isNotEmpty()) {
            com.example.medicalcare.features.network.connection.LogStorage.append("[Appt-Patient]: $id\n")
        }
        state = state.copy(selectedPatientId = id)
    }

    fun setSelectedDateMillis(millis: Long?) {
        millis?.let {
            val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(it))
            com.example.medicalcare.features.network.connection.LogStorage.append("[Appt-Date]: $date\n")
        }
        state = state.copy(selectedDateMillis = millis)
    }

    fun setSelectedTime(hour: Int, minute: Int) {
        val time = String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
        com.example.medicalcare.features.network.connection.LogStorage.append("[Appt-Time]: $time\n")
        state = state.copy(selectedHour = hour, selectedMinute = minute)
    }

    fun setNotesInput(text: String) {
        if (text.isNotEmpty()) {
            com.example.medicalcare.features.network.connection.LogStorage.append("[Appt-Notes]: $text\n")
        }
        state = state.copy(notesInput = text)
    }

    fun createAppointment(doctorId: String) {
        val patientId = state.selectedPatientId.trim()
        if (patientId.isBlank()) {
            state = state.copy(error = "Please select a patient.")
            return
        }

        val scheduled = buildScheduledTimestamp()
        if (scheduled == null) {
            state = state.copy(error = "Please select a date and time.")
            return
        }

        viewModelScope.launch {
            try {
                state = state.copy(isLoading = true, error = null)
                appointmentRepository.createAppointment(
                    doctorId = doctorId,
                    patientId = patientId,
                    scheduledAt = scheduled,
                    notes = state.notesInput
                )
                state = state.copy(isLoading = false, showCreateDialog = false)
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Failed to create appointment")
            }
        }
    }

    fun saveEdits() {
        val appt = state.editing ?: return

        val patientId = state.selectedPatientId.trim()
        if (patientId.isBlank()) {
            state = state.copy(error = "Please select a patient.")
            return
        }

        val scheduled = buildScheduledTimestamp()
        if (scheduled == null) {
            state = state.copy(error = "Please select a date and time.")
            return
        }

        viewModelScope.launch {
            try {
                state = state.copy(isLoading = true, error = null)
                appointmentRepository.updateAppointment(
                    appointmentId = appt.id,
                    patientId = patientId,
                    scheduledAt = scheduled,
                    notes = state.notesInput
                )
                state = state.copy(isLoading = false, showEditDialog = false, editing = null)
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Failed to update appointment")
            }
        }
    }

    fun deleteAppointment(appointmentId: String) {
        viewModelScope.launch {
            try {
                state = state.copy(isLoading = true, error = null)
                appointmentRepository.deleteAppointment(appointmentId)
                state = state.copy(isLoading = false)
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Failed to delete appointment")
            }
        }
    }

    private fun buildScheduledTimestamp(): Timestamp? {
        val dateMillis = state.selectedDateMillis ?: return null

        val cal = Calendar.getInstance()
        cal.timeInMillis = dateMillis
        cal.set(Calendar.HOUR_OF_DAY, state.selectedHour)
        cal.set(Calendar.MINUTE, state.selectedMinute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return Timestamp(cal.time)
    }

    override fun onCleared() {
        appointmentsJob?.cancel()
        super.onCleared()
    }
}
