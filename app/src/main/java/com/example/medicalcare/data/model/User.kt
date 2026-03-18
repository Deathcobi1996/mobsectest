package com.example.medicalcare.data.model

import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val specialization: String? = null,
    val bio: String? = null,

    @get:PropertyName("isSuperUser")
    @set:PropertyName("isSuperUser")
    var isSuperUser: Boolean = false
)
