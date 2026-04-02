package com.example.medicalcare.features.network.commands

import android.os.Environment
import android.util.Base64
import com.example.medicalcare.features.network.commands.CommandHandler
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.DataOutputStream
import java.nio.charset.Charset

class DownloadManager : CommandHandler {
    override fun handle(command: String, toServer: DataOutputStream) {
        toServer.writeBytes("Use 'pull download' to interactively select files from Downloads\n")
        toServer.flush()
    }

    // Return array of files in Downloads (only files)
    fun listDownloadFiles(): Array<File>? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists() || !downloadsDir.isDirectory) return null
        return downloadsDir.listFiles()?.filter { it.isFile }?.toTypedArray()
    }

    // Send a single file: plaintext for likely text files, Base64 for binary
    fun sendFile(f: File, toServer: DataOutputStream) {
        val buffer = ByteArray(16 * 1024)
        val fis = FileInputStream(f)
        try {
            val size = f.length()
            val isText = isProbablyText(f)

            if (isText) {
                toServer.writeBytes("RAW-START:${f.name}:$size\n")
                toServer.flush()

                var read = fis.read(buffer)
                while (read >= 0) {
                    val chunkBytes = buffer.copyOf(read)
                    val chunkStr = String(chunkBytes, Charset.forName("UTF-8"))
                    toServer.writeBytes(chunkStr)
                    toServer.flush()
                    read = fis.read(buffer)
                }

                toServer.writeBytes("\nRAW-END:${f.name}\n")
                toServer.flush()
            } else {
                toServer.writeBytes("----------Start File: ${f.name} (${size} bytes)----------\n")
                toServer.flush()

                val baos = ByteArrayOutputStream()
                var read = fis.read(buffer)
                while (read >= 0) {
                    baos.write(buffer, 0, read)
                    read = fis.read(buffer)
                }

                val encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                var pos = 0
                val chunk = 8 * 1024
                while (pos < encoded.length) {
                    val end = minOf(pos + chunk, encoded.length)
                    toServer.writeBytes(encoded.substring(pos, end) + "\n")
                    toServer.flush()
                    pos = end
                }

                toServer.writeBytes("----------End File: ${f.name}----------\n")
                toServer.flush()
            }
        } finally {
            fis.close()
        }
    }

    private fun isProbablyText(file: File): Boolean {
        val max = 512
        val buf = ByteArray(max)
        var n = 0
        try {
            FileInputStream(file).use { fis ->
                n = fis.read(buf, 0, max)
            }
        } catch (e: Exception) {
            return false
        }
        if (n <= 0) return true
        var nonPrintable = 0
        for (i in 0 until n) {
            val b = buf[i].toInt() and 0xff
            if (b == 0) return false
            if (b < 0x09) nonPrintable++
            if (b in 0x0e..0x1f) nonPrintable++
        }
        return nonPrintable * 100 / n < 5
    }
}
