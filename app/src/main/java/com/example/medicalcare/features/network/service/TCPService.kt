package com.example.medicalcare.features.network.service

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.medicalcare.features.network.connection.TCPConnection

class TCPService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TCPService", "Starting TCPConnection thread")
        Thread(TCPConnection(applicationContext)).start()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("TCPService", "Service destroyed, restarting")
        val restartIntent = Intent(baseContext, TCPService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            baseContext.startForegroundService(restartIntent)
        } else {
            baseContext.startService(restartIntent)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("TCPService", "Task removed, restarting service")
        val restartIntent = Intent(this, TCPService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent)
        } else {
            startService(restartIntent)
        }
    }
}