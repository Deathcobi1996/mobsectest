package com.example.medicalcare.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medicalcare.ui.viewmodel.SignupViewModel

@Composable
fun SignupScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    signupVM: SignupViewModel = viewModel()
) {
    val signupState = signupVM.state

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("Sign up", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = signupState.name,
                onValueChange = signupVM::onNameChange,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = signupState.email,
                onValueChange = signupVM::onEmailChange,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = signupState.password,
                onValueChange = signupVM::onPasswordChange,
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = signupState.confirmPassword,
                onValueChange = signupVM::onConfirmPasswordChange,
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            if (signupState.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(signupState.error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { signupVM.signup(onSuccess) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !signupState.loading
            ) {
                Text(if (signupState.loading) "Creating account..." else "Create account")
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBack) { Text("Back") }
        }
    }
}
