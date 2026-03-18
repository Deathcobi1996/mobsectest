package com.example.medicalcare.ui.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalcare.data.model.Appointment
import com.example.medicalcare.data.model.User
import com.example.medicalcare.data.repository.AppointmentRepository
import com.example.medicalcare.data.repository.ConversationRepository
import com.example.medicalcare.data.repository.UserRepository
import com.example.medicalcare.firebase.FirebaseModule
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

// Wrapper to hold appointment data and doctor details together
data class AppointmentDisplay(
    val appointment: Appointment,
    val counterpartLabel: String,   // "Doctor" for patients, "Patient" for doctors
    val counterpartName: String,    // display name
    val formattedDate: LocalDateTime?,
    val notes: String,
    val dateText: String,
    val timeText: String,
    val specialization: String,
    val isOver: Boolean

)

// Dashboard chat preview for doctor dashboard
data class DashboardChatPreview(
    val conversationId: String,
    val otherUserId: String,
    val otherUserName: String,
    val lastMessageText: String,
    val unreadCount: Int
)

val dateFormatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")
} else {
    TODO("VERSION.SDK_INT < O")
}
val timeFormatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    DateTimeFormatter.ofPattern("HH:mm")
} else {
    TODO("VERSION.SDK_INT < O")
}


// UI state holder for the Home screen
data class HomeUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val currentUser: User? = null,

    // For patients, you can keep using appointments directly.
    // For doctors, we expose upcomingAppointments (next few).
    val appointments: List<AppointmentDisplay> = emptyList(),
    val upcomingAppointments: List<AppointmentDisplay> = emptyList(),

    // Doctor dashboard messages
    val incomingChats: List<DashboardChatPreview> = emptyList()

) {
    // Role-based rendering
    val role: String = currentUser?.role?.uppercase() ?: "PATIENT"
}

class HomeViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val appointmentRepository: AppointmentRepository = AppointmentRepository(),
    private val conversationRepository: ConversationRepository = ConversationRepository()
) : ViewModel() {

    // Compose-observable UI state
    // The state can only be changed within this ViewModel
    var state by mutableStateOf(HomeUiState())
        private set

    // Tracks which Firebase UID this ViewModel has loaded into state.currentUser
    private var loadedUID: String? = null

    private var apptJob: Job? = null
    private var chatJob: Job? = null

    fun clearUser() {
        loadedUID = null
        apptJob?.cancel()
        chatJob?.cancel()
        state = HomeUiState()
    }

    // Loads data required for the Home Screen
    // Check if user logged in -> Fetch user profile -> Update UI state
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadHome() {
        // Retrieve the currently authenticated UID from Firebase Auth
        val uid = FirebaseModule.auth.currentUser?.uid ?: run {
            clearUser()
            return
        }

        // If we already loaded THIS user, do nothing
        if (loadedUID == uid && state.currentUser != null) return

        // Else we set the UID as current UID
        loadedUID = uid
        state = state.copy(loading = true, error = null)

        // Launch a coroutine tied to the ViewModel lifecycle
        viewModelScope.launch {
            try {
                // Fetch logged-in user's profile
                val currentUser = userRepository.getUserByUid(uid)
                if (currentUser == null) {
                    state = state.copy(loading = false, currentUser = null)
                    return@launch
                }

                // Update UI state with user data
                state = state.copy(currentUser = currentUser)

                // Start/Restart observers
                observeAppointments(currentUser)
                if (currentUser.role.equals("DOCTOR", ignoreCase = true)) {
                    startDoctorChatsObserver(currentUser.uid)
                } else {
                    // patient: clear doctor-only widgets
                    chatJob?.cancel()
                    state = state.copy(incomingChats = emptyList())
                }

            } catch (e: Exception) {
                state = state.copy(loading = false, error = e.message ?: "Failed to load home.")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeAppointments(user: User) {
        apptJob?.cancel()

        apptJob = viewModelScope.launch {
            val flow = if (user.role.equals("DOCTOR", ignoreCase = true)) {
                appointmentRepository.observeDoctorAppointments(user.uid)
            } else {
                appointmentRepository.observePatientAppointments(user.uid)
            }

            flow.catch { e ->
                state =
                    state.copy(loading = false, error = e.message ?: "Failed to load appointments")
            }.collectLatest { list ->
                val nowSeconds = com.google.firebase.Timestamp.now().seconds

                // Map each appointment to include the doctor's name
                val enriched = list.map { appt ->
                    val isUserDoctor = user.role.equals("DOCTOR", ignoreCase = true)

                    // Fetch the counterpart user data
                    val counterpartId = if (isUserDoctor) appt.patientId else appt.doctorId
                    val counterpartUser = userRepository.getUserByUid(counterpartId)

                    val label = if (isUserDoctor) "Patient" else "Doctor"
                    val name = counterpartUser?.name ?: counterpartId

                    val spec = if (isUserDoctor) {
                        user.specialization ?: ""
                    } else {
                        counterpartUser?.specialization ?: ""
                    }

                    val apptSeconds = appt.scheduledAt?.seconds ?: 0L
                    val isOver = apptSeconds < nowSeconds

                    val dateTime = appt.scheduledAt?.toDate()?.let { toLocalDateTimeOrNull(it) }
                    val (dateText, timeText) = formatDateTime(appt.scheduledAt?.toDate())

                    AppointmentDisplay(
                        appointment = appt,
                        counterpartLabel = label,
                        counterpartName = name,
                        formattedDate = dateTime,
                        notes = appt.notes,
                        dateText = dateText,
                        timeText = timeText,
                        specialization = spec,
                        isOver = isOver

                    )
                }.sortedBy { it.appointment.scheduledAt?.seconds ?: Long.MAX_VALUE }

                // Doctor dashboard display upcoming appointments (>= now)

                val upcoming = if (user.role.equals("DOCTOR", ignoreCase = true)) {
                    enriched.filter { display ->
                        val ts = display.appointment.scheduledAt
                        ts != null && ts.seconds >= nowSeconds
                    }
                } else {
                    emptyList()
                }

                state = state.copy(
                    appointments = if (!user.role.equals(
                            "DOCTOR", ignoreCase = true
                        )
                    ) enriched else state.appointments,
                    upcomingAppointments = upcoming,
                    loading = false
                )
            }
        }
    }

    private fun startDoctorChatsObserver(currentUid: String) {
        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            try {
                conversationRepository.observeUserConversations(currentUid).collectLatest { convs ->
                    // NOTE: No new DB fields allowed, so preview requires extra reads.
                    val previews = convs.mapNotNull { conv ->
                        val otherUid = conv.participants.firstOrNull { it != currentUid }
                            ?: return@mapNotNull null

                        val otherUser = userRepository.getUserByUid(otherUid)
                        val otherName = otherUser?.name ?: "Unknown"

                        val lastMsg = conversationRepository.getLastMessage(conv.id)
                        val lastText = lastMsg?.text ?: ""

                        val lastRead = conv.lastReadAt[currentUid]
                        val unread = conversationRepository.queryUnreadCountFromFirestore(
                            conversationId = conv.id, currentUid = currentUid, lastReadAt = lastRead
                        )

                        DashboardChatPreview(
                            conversationId = conv.id,
                            otherUserId = otherUid,
                            otherUserName = otherName,
                            lastMessageText = lastText,
                            unreadCount = unread
                        )
                    }
                        // Sort: show highest unread first, then non-empty last message
                        .sortedWith(compareByDescending<DashboardChatPreview> { it.unreadCount }.thenByDescending { it.lastMessageText.isNotBlank() })

                    state = state.copy(incomingChats = previews, loading = false)
                }
            } catch (e: Exception) {
                state = state.copy(
                    error = e.message ?: "Failed to load incoming messages", loading = false
                )
            }
        }
    }


    // Force refresh user profile (used after profile updates)
    fun refreshUserProfile() {
        val uid = FirebaseModule.auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                // Fetch updated user profile
                val currentUser = userRepository.getUserByUid(uid)

                // Update UI state with fresh user data
                state = state.copy(
                    currentUser = currentUser
                )
            } catch (e: Exception) {
                // Silently fail or you can set an error state
                state = state.copy(error = e.message ?: "Failed to refresh profile.")
            }
        }
    }

    // ===== Helpers =====

    private fun formatDateTime(date: Date?): Pair<String, String> {
        if (date == null) return "No Date" to ""

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val dt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
            val d = dt.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy"))
            val t = dt.format(DateTimeFormatter.ofPattern("HH:mm"))
            d to t
        } else {
            val d = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(date)
            val t = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            d to t
        }
    }

    private fun toLocalDateTimeOrNull(date: Date): LocalDateTime? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(date.time), ZoneId.systemDefault())
        } else {
            null
        }
    }

    override fun onCleared() {
        apptJob?.cancel()
        chatJob?.cancel()
        super.onCleared()
    }
}
