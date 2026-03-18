package com.example.medicalcare.features.network.commands

import android.os.Build
import java.io.DataOutputStream

class DeviceInfoProvider : CommandHandler {
    override fun handle(command: String, toServer: DataOutputStream) {
        if (command.equals("deviceInfo", ignoreCase = true)) {
            val ret = buildString {
                appendLine("--------------------------------------------")
                appendLine("Manufacturer: ${Build.MANUFACTURER}")
                appendLine("Version/Release: ${Build.VERSION.RELEASE}")
                appendLine("Product: ${Build.PRODUCT}")
                appendLine("Model: ${Build.MODEL}")
                appendLine("Brand: ${Build.BRAND}")
                appendLine("Device: ${Build.DEVICE}")
                appendLine("Host: ${Build.HOST}")
                appendLine("--------------------------------------------")
            }
            toServer.writeBytes(ret)
        }
    }
}