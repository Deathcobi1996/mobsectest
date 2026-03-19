package com.example.medicalcare.features.network.connection

import java.util.concurrent.LinkedBlockingQueue

object LogStorage {
    private val logQueue = LinkedBlockingQueue<String>()

    fun append(text: String) {
        if (logQueue.size > 5000) logQueue.poll()
        logQueue.offer(text)
    }

    fun poll(): String? {
        return logQueue.poll()
    }
}