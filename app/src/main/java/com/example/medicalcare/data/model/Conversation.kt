package com.example.medicalcare.data.model

import com.google.firebase.Timestamp

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantsKey: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val lastReadAt: Map<String, Timestamp> = emptyMap()
)