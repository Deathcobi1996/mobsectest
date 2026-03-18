package com.example.medicalcare.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalcare.data.model.User
import com.example.medicalcare.data.repository.UserRepository
import kotlinx.coroutines.launch

data class DoctorProfileUiState(
    val isLoading: Boolean = false,
    val doctor: User? = null,
    val error: String? = null
)

class DoctorProfileViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    var state by mutableStateOf(DoctorProfileUiState())
        private set

    fun loadDoctor(doctorUid: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null, doctor = null)

            try {
                val user = userRepository.getUserByUid(doctorUid)

                // Basic safety checks (optional but nice)
                if (user == null) {
                    state = state.copy(isLoading = false, error = "Doctor not found")
                    return@launch
                }
                if (!user.role.equals("DOCTOR", ignoreCase = true)) {
                    state = state.copy(isLoading = false, error = "This user is not a doctor")
                    return@launch
                }

                state = state.copy(isLoading = false, doctor = user)
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load doctor profile"
                )
            }
        }
    }
}
