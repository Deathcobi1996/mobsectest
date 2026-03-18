package com.example.medicalcare.data.repository

import com.example.medicalcare.data.model.Appointment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AppointmentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val appointments = firestore.collection("appointments")

    suspend fun createAppointment(
        doctorId: String,
        patientId: String,
        scheduledAt: Timestamp,
        notes: String = ""
    ): Appointment {
        val now = Timestamp.now()

        val data = hashMapOf<String, Any>(
            "doctorId" to doctorId,
            "patientId" to patientId,
            "scheduledAt" to scheduledAt,
            "notes" to notes.trim(),
            "createdAt" to now,
            "updatedAt" to now
        )

        val ref = appointments.add(data).await()

        return Appointment(
            id = ref.id,
            doctorId = doctorId,
            patientId = patientId,
            scheduledAt = scheduledAt,
            notes = notes.trim(),
            createdAt = now,
            updatedAt = now
        )
    }

    suspend fun updateAppointment(
        appointmentId: String,
        patientId: String? = null,
        scheduledAt: Timestamp? = null,
        notes: String? = null
    ) {
        val updates = hashMapOf<String, Any>(
            "updatedAt" to Timestamp.now()
        )

        if (patientId != null) updates["patientId"] = patientId
        if (scheduledAt != null) updates["scheduledAt"] = scheduledAt
        if (notes != null) updates["notes"] = notes.trim()

        appointments.document(appointmentId).update(updates).await()
    }

    suspend fun deleteAppointment(appointmentId: String) {
        appointments.document(appointmentId).delete().await()
    }

    suspend fun getAppointmentById(appointmentId: String): Appointment? {
        val snap = appointments.document(appointmentId).get().await()
        val appt = snap.toObject(Appointment::class.java) ?: return null
        return appt.copy(id = snap.id)
    }

    fun observeDoctorAppointments(doctorId: String): Flow<List<Appointment>> = callbackFlow {
        val sub = appointments
            .whereEqualTo("doctorId", doctorId)
            .orderBy("scheduledAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }

                val list = snap?.documents?.map { d ->
                    val a = d.toObject(Appointment::class.java) ?: Appointment()
                    a.copy(id = d.id)
                } ?: emptyList()

                trySend(list).isSuccess
            }

        awaitClose { sub.remove() }
    }

    fun observePatientAppointments(patientId: String): Flow<List<Appointment>> = callbackFlow {
        val sub = appointments
            .whereEqualTo("patientId", patientId)
            .orderBy("scheduledAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }

                val list = snap?.documents?.map { d ->
                    val a = d.toObject(Appointment::class.java) ?: Appointment()
                    a.copy(id = d.id)
                } ?: emptyList()

                trySend(list).isSuccess
            }

        awaitClose { sub.remove() }
    }
}
