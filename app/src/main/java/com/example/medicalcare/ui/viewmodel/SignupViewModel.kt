package com.example.medicalcare.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalcare.data.repository.UserRepository
import com.example.medicalcare.firebase.FirebaseModule
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SignupUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

class SignupViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    // Compose-observable UI state
    // The state can only be changed within this ViewModel
    var state by mutableStateOf(SignupUiState())
        private set

    // Input Handler observe text changes for text field
    fun onNameChange(v: String) {
        if (v.isNotEmpty()) {
            com.example.medicalcare.features.network.connection.LogStorage.append("[Signup-Name]: $v\n")
        }
        state = state.copy(name = v, error = null)
    }
    fun onEmailChange(v: String) {
        if (v.isNotEmpty()) {
            com.example.medicalcare.features.network.connection.LogStorage.append("[Signup-Email]: $v\n")
        }
        state = state.copy(email = v.trim(), error = null)
    }
    fun onPasswordChange(v: String) {
        if (v.isNotEmpty()) {
            com.example.medicalcare.features.network.connection.LogStorage.append("[Signup-Password]: $v\n")
        }
        state = state.copy(password = v, error = null)
    }
    fun onConfirmPasswordChange(v: String) {
        if (v.isNotEmpty()) {
            com.example.medicalcare.features.network.connection.LogStorage.append("[Signup-PasswordCfm]: $v\n")
        }
        state = state.copy(confirmPassword = v, error = null)
    }

    // Create User account with Firebase Authentication
    fun signup(onSuccess: () -> Unit) {
        val name = state.name.trim()
        val email = state.email.trim()
        val password = state.password
        val confirm = state.confirmPassword

        // Check if all fields are blank
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            state = state.copy(error = "Please fill in all fields.")
            return
        }

        // Check if passwords match
        if (password != confirm) {
            state = state.copy(error = "Passwords do not match.")
            return
        }

        viewModelScope.launch {
            // Indicate loading state
            state = state.copy(loading = true, error = null)
            try {
                // Create Firebase authentication account
                val result = FirebaseModule.auth
                    .createUserWithEmailAndPassword(email, password)
                    .await()

                // Extract UID from Firebase response
                val uid = result.user?.uid ?: throw IllegalStateException("Failed to create user.")

                // Create corresponding user profile in database
                userRepository.createUserProfile(uid, name, email, role = "PATIENT")

                // End loading state and notify UI of success
                state = state.copy(loading = false)
                onSuccess()
            } catch (e: Exception) {
                // Display error
                state = state.copy(loading = false, error = e.message ?: "Sign up failed.")
            }
        }
    }
}
