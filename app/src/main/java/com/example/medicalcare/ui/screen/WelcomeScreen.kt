package com.example.medicalcare.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(
    onLogin: () -> Unit,
    onSignup: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("ChatMessage", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
                Text("Login")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onSignup, modifier = Modifier.fillMaxWidth()) {
                Text("Sign up")
            }
        }
    }
}
