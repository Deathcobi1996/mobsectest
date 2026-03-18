package com.example.medicalcare.features.network.commands

import java.io.DataOutputStream

interface CommandHandler {
    fun handle(command: String, toServer: DataOutputStream)
}