package com.example.medicalcare.data.repository

import com.example.medicalcare.data.model.Conversation
import com.example.medicalcare.data.model.Message
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.Query

class ConversationRepository {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val conversations = firestore.collection("conversations")

    private fun participantsKey(a: String, b: String): String {
        return listOf(a, b).sorted().joinToString("_")
    }

    // Find conversation (or create if it doesn't exist)
    suspend fun getOrCreateConversation(userA: String, userB: String): Conversation {
        val key = participantsKey(userA, userB)

        val existing = conversations
            .whereEqualTo("participantsKey", key)
            .limit(1)
            .get()
            .await()

        if (!existing.isEmpty) {
            val doc = existing.documents.first()
            val conv = doc.toObject(Conversation::class.java) ?: Conversation()
            return conv.copy(id = doc.id)
        }

        val now = Timestamp.now()
        val data = hashMapOf<String, Any>(
            "participants" to listOf(userA, userB),
            "participantsKey" to key,
            "createdAt" to now,
            "updatedAt" to now,
            "lastReadAt" to hashMapOf(
                userA to now,
                userB to now
            )
        )

        val ref = conversations.add(data).await()
        return Conversation(
            id = ref.id,
            participants = listOf(userA, userB),
            participantsKey = key,
            createdAt = now,
            updatedAt = now,
            lastReadAt = mapOf(userA to now, userB to now)
        )
    }

    fun observeMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val sub = conversations
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.map { d ->
                    val m = d.toObject(Message::class.java) ?: Message()
                    m.copy(id = d.id)
                } ?: emptyList()

                trySend(list).isSuccess
            }

        awaitClose { sub.remove() }
    }

    fun observeConversation(conversationId: String): Flow<Conversation?> = callbackFlow {
        val sub = conversations
            .document(conversationId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val conv = snap?.toObject(Conversation::class.java)?.copy(id = snap.id)
                trySend(conv).isSuccess
            }

        awaitClose { sub.remove() }
    }

    suspend fun sendMessage(conversationId: String, senderId: String, text: String) {
        val now = Timestamp.now()
        val msgData = hashMapOf<String, Any>(
            "senderId" to senderId,
            "text" to text,
            "createdAt" to now
        )

        conversations.document(conversationId)
            .collection("messages")
            .add(msgData)
            .await()

        // Update conversation meta
        val updates = hashMapOf<String, Any>(
            "updatedAt" to now,
            "lastReadAt.$senderId" to now
        )
        conversations.document(conversationId).update(updates).await()
    }

    // For sending location
    suspend fun sendLocationMessage(conversationId: String, senderId: String, lat: Double, lng: Double) {
        val now = Timestamp.now()
        val url = "https://maps.google.com/?q=$lat,$lng"

        val msgData = hashMapOf<String, Any>(
            "senderId" to senderId,
            "text" to "📍 Shared a location",
            "createdAt" to now,
            "lat" to lat,
            "lng" to lng,
            "mapsUrl" to url
        )

        conversations.document(conversationId)
            .collection("messages")
            .add(msgData)
            .await()

        // keep conversation meta behaviour consistent with sendMessage()
        val updates = hashMapOf<String, Any>(
            "updatedAt" to now,
            "lastReadAt.$senderId" to now
        )
        conversations.document(conversationId).update(updates).await()
    }


    suspend fun markRead(conversationId: String, userId: String) {
        val now = Timestamp.now()
        conversations.document(conversationId)
            .update(mapOf("lastReadAt.$userId" to now))
            .await()
    }

    // Compute unseen messages from other user based on lastReadAt[currentUserId]
    // Messages reside in memory, no firestore call
    // If lastReadAt does not exist, it is 0.
    fun computeUnseenCountFromMessages(
        messages: List<Message>,
        currentUserId: String,
        lastReadAt: Timestamp?
    ): Int {
        if (lastReadAt == null) return 0

        return messages.count { msg ->
            val t = msg.createdAt ?: return@count false
            msg.senderId != currentUserId && t.seconds > lastReadAt.seconds
        }
    }

    fun observeUserConversations(currentUid: String): Flow<List<Conversation>> = callbackFlow {
        val sub = conversations
            .whereArrayContains("participants", currentUid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }

                val list = snap?.documents?.map { d ->
                    val c = d.toObject(Conversation::class.java) ?: Conversation()
                    c.copy(id = d.id)
                } ?: emptyList()

                trySend(list).isSuccess
            }

        awaitClose { sub.remove() }
    }

    // For preview in ChatScreen
    suspend fun getLastMessage(conversationId: String): Message? {
        val snap = conversations.document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        val doc = snap.documents.firstOrNull() ?: return null
        val m = doc.toObject(Message::class.java) ?: return null
        return m.copy(id = doc.id)
    }

    // Firestore query-based calculation
    // For preview in ChatScreen, Works without loading all messages
    suspend fun queryUnreadCountFromFirestore(conversationId: String, currentUid: String, lastReadAt: Timestamp?): Int {
        if (lastReadAt == null) return 0

        val snap = conversations.document(conversationId)
            .collection("messages")
            .whereGreaterThan("createdAt", lastReadAt)
            .get()
            .await()

        return snap.documents.count { doc ->
            (doc.getString("senderId") ?: "") != currentUid
        }
    }
}
