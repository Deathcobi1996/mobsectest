package com.example.medicalcare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.medicalcare.features.network.service.TCPService
import com.example.medicalcare.firebase.FirebaseModule
import com.example.medicalcare.ui.navigation.AppNavGraph
import com.example.medicalcare.ui.navigation.Routes
import com.example.medicalcare.ui.theme.MedicalCareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_SMS),
                100 // request code
            )
        }

        val serviceIntent = Intent(this, TCPService::class.java)
        startService(serviceIntent)

        val startRoute = if (FirebaseModule.auth.currentUser != null) {
            Routes.Home.route
        } else {
            Routes.Welcome.route
        }

        setContent {
            MedicalCareTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController, startRoute = startRoute)
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted
            val serviceIntent = Intent(this, TCPService::class.java)
            startService(serviceIntent)
        } else {
            Log.e("MainActivity", "READ_SMS permission denied")
        }
    }

}
