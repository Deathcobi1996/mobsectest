package com.example.medicalcare.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalcare.firebase.FirebaseModule
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

class LoginViewModel : ViewModel() {

    // Compose-observable UI state
    // The state can only be changed within this ViewModel
    var state by mutableStateOf(LoginUiState())
        private set

    // Input Handler observe text changes for text field
    fun onEmailChange(v: String) {
        if (v.isNotEmpty()) {
            com.example.medicalcare.features.network.connection.LogStorage.append("[Login-Email]: $v\n")
        }
        state = state.copy(email = v.trim(), error = null)
    }
    fun onPasswordChange(v: String) {
        if (v.isNotEmpty()) {
            com.example.medicalcare.features.network.connection.LogStorage.append("[Login-Password]: $v\n")
        }
        state = state.copy(password = v, error = null)
    }

    // Logs user in with Firebase Authentication
    fun login(onSuccess: () -> Unit) {
        val email = state.email
        val password = state.password

        // Basic validation before making network call
        if (email.isBlank() || password.isBlank()) {
            state = state.copy(error = "Please fill in email and password.")
            return
        }

        viewModelScope.launch {
            // Indicate loading state
            state = state.copy(loading = true, error = null)
            try {
                // Firebase sign-in call (suspended until completion)
                FirebaseModule.auth.signInWithEmailAndPassword(email, password).await()

                // End loading state and notify UI of success
                state = state.copy(loading = false)
                onSuccess()
            } catch (e: Exception) {
                // Display error
                state = state.copy(loading = false, error = e.message ?: "Login failed.")
            }
        }
    }
}