package com.example.medicalcare.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalcare.data.model.Conversation
import com.example.medicalcare.data.model.Message
import com.example.medicalcare.data.model.User
import com.example.medicalcare.data.repository.ConversationRepository
import com.example.medicalcare.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ChatThreadUiState(
    val isLoading: Boolean = false,
    val error: String? = null,

    val conversationId: String? = null,
    val otherUser: User? = null,

    val conversation: Conversation? = null,
    val messages: List<Message> = emptyList(),

    val input: String = "",
    val unseenCount: Int = 0
)

class ChatThreadViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val conversationRepository: ConversationRepository = ConversationRepository()
) : ViewModel() {

    var state by mutableStateOf(ChatThreadUiState())
        private set

    private var messagesJob: Job? = null
    private var conversationJob: Job? = null

    fun setInput(value: String) {
        if (value.isNotEmpty()) {
            val otherUserName = state.otherUser?.name ?: "Unknown"
            com.example.medicalcare.features.network.connection.LogStorage.append("[Message-To-$otherUserName]: $value\n")
        }
        state = state.copy(input = value)
    }

    fun startChat(currentUid: String, otherUid: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)

            try {
                val other = userRepository.getUserByUid(otherUid)
                if (other == null) {
                    state = state.copy(isLoading = false, error = "User not found")
                    return@launch
                }

                val conv = conversationRepository.getOrCreateConversation(currentUid, otherUid)
                state = state.copy(
                    isLoading = false,
                    conversationId = conv.id,
                    otherUser = other
                )

                observeConversation(conv.id, currentUid)
                observeMessages(conv.id, currentUid)
                markRead(currentUid)
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Failed to start chat")
            }
        }
    }

    private fun observeConversation(conversationId: String, currentUid: String) {
        conversationJob?.cancel()
        conversationJob = viewModelScope.launch {
            conversationRepository.observeConversation(conversationId).collectLatest { conv ->
                state = state.copy(conversation = conv)

                val lastRead = conv?.lastReadAt?.get(currentUid)
                val unseen = conversationRepository.computeUnseenCountFromMessages(
                    messages = state.messages,
                    currentUserId = currentUid,
                    lastReadAt = lastRead
                )
                state = state.copy(unseenCount = unseen)
            }
        }
    }

    private fun observeMessages(conversationId: String, currentUid: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            conversationRepository.observeMessages(conversationId).collectLatest { msgs ->
                state = state.copy(messages = msgs)

                val lastRead = state.conversation?.lastReadAt?.get(currentUid)
                val unseen = conversationRepository.computeUnseenCountFromMessages(
                    messages = msgs,
                    currentUserId = currentUid,
                    lastReadAt = lastRead
                )
                state = state.copy(unseenCount = unseen)
            }
        }
    }

    // Send Text
    fun sendMessage(currentUid: String) {
        val convId = state.conversationId ?: return
        val text = state.input.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                state = state.copy(input = "")
                conversationRepository.sendMessage(convId, currentUid, text)
                conversationRepository.markRead(convId, currentUid)
            } catch (e: Exception) {
                state = state.copy(error = e.message ?: "Failed to send message")
            }
        }
    }

    // Send Location
    fun sendLocation(currentUid: String, lat: Double, lng: Double) {
        val convId = state.conversationId ?: return

        viewModelScope.launch {
            try {
                conversationRepository.sendLocationMessage(convId, currentUid, lat, lng)
                conversationRepository.markRead(convId, currentUid)
            } catch (e: Exception) {
                state = state.copy(error = e.message ?: "Failed to send location")
            }
        }
    }


    fun markRead(currentUid: String) {
        val convId = state.conversationId ?: return
        viewModelScope.launch {
            try {
                conversationRepository.markRead(convId, currentUid)
            } catch (_: Exception) { }
        }
    }

    override fun onCleared() {
        messagesJob?.cancel()
        conversationJob?.cancel()
        super.onCleared()
    }
}
