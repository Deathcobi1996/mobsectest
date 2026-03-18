package com.example.medicalcare.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medicalcare.ui.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    loginVM: LoginViewModel = viewModel()
) {
    val loginState = loginVM.state

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("Login", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = loginState.email,
                onValueChange = loginVM::onEmailChange,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = loginState.password,
                onValueChange = loginVM::onPasswordChange,
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            if (loginState.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(loginState.error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { loginVM.login(onSuccess) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loginState.loading
            ) {
                Text(if (loginState.loading) "Logging in..." else "Login")
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBack) { Text("Back") }
        }
    }
}
