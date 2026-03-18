package com.example.medicalcare.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalcare.data.model.User
import com.example.medicalcare.data.repository.ConversationRepository
import com.example.medicalcare.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class ChatListRow(
    val conversationId: String,
    val otherUserId: String,
    val otherUserName: String,
    val lastMessageText: String,
    val unreadCount: Int
)

data class ChatListUiState(
    val isLoading: Boolean = false,
    val chats: List<ChatListRow> = emptyList(),
    val error: String? = null
)

class ChatViewModel(
    private val conversationRepo: ConversationRepository = ConversationRepository(),
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    var state by mutableStateOf(ChatListUiState())
        private set

    private var job: Job? = null

    fun start(currentUid: String) {
        job?.cancel()
        state = state.copy(isLoading = true, error = null)

        job = viewModelScope.launch {
            try {
                conversationRepo.observeUserConversations(currentUid).collect { convs ->
                    val rows = convs.mapNotNull { conv ->
                        val otherUid = conv.participants.firstOrNull { it != currentUid } ?: return@mapNotNull null

                        val otherUser = userRepo.getUserByUid(otherUid)
                            ?: User(uid = otherUid, name = "Unknown")

                        val lastMsg = conversationRepo.getLastMessage(conv.id)
                        val lastText = lastMsg?.text ?: ""

                        val lastRead = conv.lastReadAt[currentUid]
                        val unread = conversationRepo.queryUnreadCountFromFirestore(conv.id, currentUid, lastRead)

                        ChatListRow(
                            conversationId = conv.id,
                            otherUserId = otherUid,
                            otherUserName = otherUser.name,
                            lastMessageText = lastText,
                            unreadCount = unread
                        )
                    }

                    state = state.copy(isLoading = false, chats = rows)
                }
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Failed to load chats")
            }
        }
    }

    override fun onCleared() {
        job?.cancel()
        super.onCleared()
    }
}
