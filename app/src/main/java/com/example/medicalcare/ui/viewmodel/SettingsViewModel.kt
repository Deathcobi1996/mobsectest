package com.example.medicalcare.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalcare.data.model.User
import com.example.medicalcare.data.repository.UserRepository
import com.example.medicalcare.firebase.FirebaseModule
import kotlinx.coroutines.launch

data class SettingsUiState(
    val user: User? = null,              // Current user data
    val name: String = "",               // Editable name
    val specialization: String = "",     // Editable specialization (doctors)
    val bio: String = "",                // Editable bio (doctors)
    val loading: Boolean = false,        // Loading state
    val error: String? = null,           // Error messages
    val successMessage: String? = null   // Success messages
)

class SettingsViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    // Compose-observable UI state
    // The state can only be changed within this ViewModel
    var state by mutableStateOf(SettingsUiState())
        private set

    // Rest of code / functions goes below
    // Load User's Profile
    fun loadUserProfile() {
        val currentUserId = FirebaseModule.auth.currentUser?.uid ?: return

        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                val user = userRepository.getUserByUid(currentUserId)
                if (user != null) {
                    state = state.copy(
                        user = user,
                        name = user.name,
                        specialization = user.specialization ?: "",
                        bio = user.bio ?: "",
                        loading = false
                    )
                }
            } catch (e: Exception) {
                state = state.copy(
                    loading = false,
                    error = "Failed to load profile: ${e.message}"
                )
            }
        }
    }

    // Input handlers
    fun onNameChange(value: String) {
        state = state.copy(name = value, error = null, successMessage = null)
    }
    fun onSpecializationChange(value: String) {
        state = state.copy(specialization = value, error = null, successMessage = null)
    }
    fun onBioChange(value: String) {
        state = state.copy(bio = value, error = null, successMessage = null)
    }

    // Update User Profile
    fun updateProfile(onSuccess: () -> Unit = {}) {
        val currentUserId = FirebaseModule.auth.currentUser?.uid ?: return
        val currentUser = state.user ?: return

        // Validation
        if (state.name.isBlank()) {
            state = state.copy(error = "Name cannot be empty")
            return
        }

        // For doctors, validate specialization
        if (currentUser.role == "DOCTOR" && state.specialization.isBlank()) {
            state = state.copy(error = "Specialization cannot be empty for doctors")
            return
        }

        viewModelScope.launch {
            state = state.copy(loading = true, error = null, successMessage = null)
            try {
                userRepository.updateUserProfile(
                    uid = currentUserId,
                    name = state.name,
                    specialization = if (currentUser.role == "DOCTOR")
                        state.specialization else null,
                    bio = if (currentUser.role == "DOCTOR")
                        state.bio else null
                )
                // Reload the profile to reflect changes
                val updatedUser = userRepository.getUserByUid(currentUserId)
                state = state.copy(
                    user = updatedUser,
                    loading = false,
                    successMessage = "Profile updated successfully!"
                )
                // Trigger the callback to refresh HomeViewModel
                onSuccess()
            } catch (e: Exception) {
                state = state.copy(
                    loading = false,
                    error = "Failed to update profile: ${e.message}"
                )
            }
        }
    }
}