package com.example.medicalcare.data.model

import com.google.firebase.Timestamp

data class Message(
    // For Regular Texts
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null,

    // For Location Sharing
    val lat: Double? = null,
    val lng: Double? = null,
    val mapsUrl: String? = null
)
