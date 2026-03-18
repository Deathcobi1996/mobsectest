package com.example.medicalcare.data.model

import com.google.firebase.Timestamp

data class Appointment(
    val id: String = "",
    val doctorId: String = "",
    val patientId: String = "",
    val scheduledAt: Timestamp? = null,
    val notes: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
