package com.example.medicalcare.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalcare.data.model.User
import com.example.medicalcare.data.repository.UserRepository
import kotlinx.coroutines.launch

data class DoctorDiscoveryUiState(
    val isLoading: Boolean = false,
    val doctors: List<User> = emptyList(),
    val error: String? = null
)

class DoctorDiscoveryViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    // Compose-observable UI state
    // The state can only be changed within this ViewModel
    var state by mutableStateOf(DoctorDiscoveryUiState())
        private set

    // Load all doctors except themselves
    fun loadDoctors(currentUserUid: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)

            try {
                val doctors = userRepository.getAllUsers()
                    .asSequence()
                    .filter { it.role.equals("DOCTOR", ignoreCase = true) }
                    .filter { it.uid != currentUserUid }
                    .sortedBy { it.name.lowercase() }
                    .toList()

                state = state.copy(
                    isLoading = false,
                    doctors = doctors
                )
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load doctors"
                )
            }
        }
    }
}