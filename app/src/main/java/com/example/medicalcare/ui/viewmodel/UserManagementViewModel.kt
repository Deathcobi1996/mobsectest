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
import kotlinx.coroutines.tasks.await

data class UserManagementUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val role: String = "DOCTOR", // Default to DOCTOR
    val specialization: String = "",
    val bio: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val staffList: List<User> = emptyList(),
    val loadingStaff: Boolean = false,
    val currentUser: User? = null
)

class UserManagementViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    var state by mutableStateOf(UserManagementUiState())
        private set

    // Load current user to check if they're a superuser
    fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val uid = FirebaseModule.auth.currentUser?.uid
                if (uid != null) {
                    val user = userRepository.getUserByUid(uid)
                    state = state.copy(currentUser = user)
                }
            } catch (e: Exception) {
                state = state.copy(error = "Failed to load user info: ${e.message}")
            }
        }
    }

    // Load all staff members (doctors)
    fun loadStaffList() {
        viewModelScope.launch {
            state = state.copy(loadingStaff = true)
            try {
                val staffList = userRepository.getAllStaff()
                state = state.copy(staffList = staffList, loadingStaff = false)
            } catch (e: Exception) {
                state = state.copy(
                    error = "Failed to load staff list: ${e.message}",
                    loadingStaff = false
                )
            }
        }
    }

    // Input handlers
    fun onNameChange(v: String) {
        state = state.copy(name = v, error = null, successMessage = null)
    }

    fun onEmailChange(v: String) {
        state = state.copy(email = v.trim(), error = null, successMessage = null)
    }

    fun onPasswordChange(v: String) {
        state = state.copy(password = v, error = null, successMessage = null)
    }

    fun onConfirmPasswordChange(v: String) {
        state = state.copy(confirmPassword = v, error = null, successMessage = null)
    }

    fun onRoleChange(v: String) {
        state = state.copy(role = v, error = null, successMessage = null)
    }

    fun onSpecializationChange(v: String) {
        state = state.copy(specialization = v, error = null, successMessage = null)
    }

    fun onBioChange(v: String) {
        state = state.copy(bio = v, error = null, successMessage = null)
    }

    // Create staff account (Doctor)
    fun createStaffAccount(onSuccess: () -> Unit) {
        val name = state.name.trim()
        val email = state.email.trim()
        val password = state.password
        val confirm = state.confirmPassword
        val role = state.role
        val specialization = state.specialization.trim()
        val bio = state.bio.trim()

        // Validation
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            state = state.copy(error = "Please fill in all required fields.")
            return
        }

        if (password != confirm) {
            state = state.copy(error = "Passwords do not match.")
            return
        }

        if (password.length < 6) {
            state = state.copy(error = "Password must be at least 6 characters.")
            return
        }

        if (role == "DOCTOR" && specialization.isBlank()) {
            state = state.copy(error = "Please specify the doctor's specialization.")
            return
        }

        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                // Create Firebase authentication account
                val result = FirebaseModule.auth
                    .createUserWithEmailAndPassword(email, password)
                    .await()

                val uid = result.user?.uid ?: throw IllegalStateException("Failed to create user.")

                // Create staff profile in database
                userRepository.createStaffAccount(
                    uid = uid,
                    name = name,
                    email = email,
                    role = role,
                    specialization = if (specialization.isNotBlank()) specialization else null,
                    bio = if (bio.isNotBlank()) bio else null
                )

                // Sign out the newly created account to prevent automatic login
                FirebaseModule.auth.signOut()

                // Clear form and show success
                state = state.copy(
                    loading = false,
                    name = "",
                    email = "",
                    password = "",
                    confirmPassword = "",
                    specialization = "",
                    bio = "",
                    successMessage = "Successfully created ${role.lowercase()} account for $name"
                )

                // Reload staff list
                loadStaffList()
                onSuccess()

            } catch (e: Exception) {
                state = state.copy(
                    loading = false,
                    error = e.message ?: "Failed to create account."
                )
            }
        }
    }

    // Delete staff account
    fun deleteStaffAccount(uid: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                userRepository.deleteUserAccount(uid)
                state = state.copy(successMessage = "Staff account deleted successfully")
                loadStaffList()
                onSuccess()
            } catch (e: Exception) {
                state = state.copy(error = "Failed to delete account: ${e.message}")
            }
        }
    }

    // Clear messages
    fun clearMessages() {
        state = state.copy(error = null, successMessage = null)
    }
}