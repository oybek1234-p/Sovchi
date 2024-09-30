package com.uz.sovchi.ui.chat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.uz.sovchi.data.UserRepository
import com.uz.sovchi.data.chat.ChatController
import com.uz.sovchi.data.chat.ChatModel
import com.uz.sovchi.data.utils.FirebaseUtils.toObjectsSafe
import com.uz.sovchi.postVal
import com.uz.sovchi.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatsViewModel : ViewModel() {

    var chatsList = arrayListOf<ChatModel>()
    var chats = MutableLiveData<List<ChatModel>>()
    var chatsLoading = MutableLiveData(false)

    companion object {
        var lastSeenTimesObserves = hashMapOf<String, Long>()
    }

    private var lastSeenValueEventListeners = hashMapOf<String, ValueEventListener?>()

    override fun onCleared() {
        super.onCleared()
        lastSeenValueEventListeners.values.filterNotNull().forEach {
            UserRepository.removeLastSeenListener(it)
        }
    }

    private fun observeLastSeenTime(userId: String) {
        if (lastSeenTimesObserves.containsKey(userId).not()) {
            lastSeenTimesObserves[userId] = -1
            lastSeenValueEventListeners[userId] = UserRepository.observeLastSeen(userId) { it ->
                lastSeenTimesObserves[userId] = it
                chats.postVal(chatsList)
            }
        }
    }

    private val chatsListener = EventListener<QuerySnapshot> { value, error ->
        chatsLoading.postVal(false)
        chatsLoading.value = false
        viewModelScope.launch(Dispatchers.Default) {
            value?.toObjectsSafe(ChatModel::class.java)?.let { it ->
                withContext(Dispatchers.Main) {
                    chatsList.clear()
                    chatsList.addAll(it)
                    it.filter { lastSeenTimesObserves.contains(it.userId).not() }.forEach {
                        observeLastSeenTime(it.userId)
                    }
                    ChatController.chats.clear()
                    ChatController.chats.addAll(chatsList)
                    chats.postVal(chatsList)
                }
            }
        }
    }

    private var regisration: ListenerRegistration? = null

    fun stopListeningChats() {
        regisration?.remove()
    }

    fun startObservingChats() {
        chatsLoading.postVal(true)
        regisration = ChatController.observeChats(chatsListener)
    }
}