package com.example.medicalcare.data.repository

import com.example.medicalcare.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    // Create User (Default is patient role)
    suspend fun createUserProfile(
        uid: String,
        name: String,
        email: String,
        role: String = "PATIENT",
        specialization: String? = null,
        bio: String? = null,
        isSuperUser: Boolean = false
    ) {
        val data = hashMapOf<String, Any?>(
            "uid" to uid,
            "name" to name.trim(),
            "email" to email.trim(),
            "role" to role,
            "isSuperUser" to isSuperUser
        )

        if (role == "DOCTOR") {
            data["specialization"] = specialization?.trim().orEmpty()
            data["bio"] = bio?.trim().orEmpty()
        }

        usersCollection.document(uid).set(data).await()
    }

    // Create staff account (Doctor/Nurse) - Only for superuser doctors
    suspend fun createStaffAccount(
        uid: String,
        name: String,
        email: String,
        role: String,
        specialization: String? = null,
        bio: String? = null
    ) {
        val data = hashMapOf<String, Any?>(
            "uid" to uid,
            "name" to name.trim(),
            "email" to email.trim(),
            "role" to role,
            "isSuperUser" to false
        )

        if (role == "DOCTOR") {
            data["specialization"] = specialization?.trim().orEmpty()
            data["bio"] = bio?.trim().orEmpty()
        }

        usersCollection.document(uid).set(data).await()
    }

    // Fetch all users
    suspend fun getAllUsers(): List<User> {
        val snapshot = usersCollection.get().await()
        return snapshot.documents.mapNotNull { it.toObject(User::class.java) }
    }

    // Get User by their ID
    suspend fun getUserByUid(uid: String): User? {
        return usersCollection
            .document(uid)
            .get()
            .await()
            .toObject(User::class.java)
    }

    // Update User's Profile
    suspend fun updateUserProfile(
        uid: String,
        name: String? = null,
        specialization: String? = null,
        bio: String? = null
    ) {
        val updates = hashMapOf<String, Any>()

        if (name != null) {
            updates["name"] = name.trim()
        }
        if (specialization != null) {
            updates["specialization"] = specialization.trim()
        }
        if (bio != null) {
            updates["bio"] = bio.trim()
        }

        if (updates.isNotEmpty()) {
            usersCollection.document(uid).update(updates).await()
        }
    }

    // Get all doctors and nurses (for staff management)
    suspend fun getAllStaff(): List<User> {
        val snapshot = usersCollection
            .whereIn("role", listOf("DOCTOR"))
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(User::class.java) }
    }

    // Delete user account (for superuser only)
    suspend fun deleteUserAccount(uid: String) {
        usersCollection.document(uid).delete().await()
    }
}
