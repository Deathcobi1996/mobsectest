package com.example.medicalcare.ui.navigation

sealed class Routes(val route: String) {
    data object Welcome : Routes("welcome")
    data object Login : Routes("login")
    data object Signup : Routes("signup")
    data object Home : Routes("home")

    data object DoctorDiscovery : Routes("doctordiscovery")
    data object Chat : Routes("chat")
    data object Appointment : Routes("appointment")
    data object Settings : Routes("settings")
    data object UserManagement : Routes("usermanagement")

    data object ChatThread : Routes("chat/{otherUid}") {
        fun create(otherUid: String) = "chat/$otherUid"
    }

    data object DoctorProfile : Routes("doctor_profile/{doctorUid}") {
        fun create(doctorUid: String) = "doctor_profile/$doctorUid"
    }
}
