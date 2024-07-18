package com.uz.sovchi.ui.chat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.uz.sovchi.data.chat.ChatController
import com.uz.sovchi.data.chat.ChatModel
import com.uz.sovchi.showToast

class ChatsViewModel : ViewModel() {

    var chatsList = arrayListOf<ChatModel>()
    var chats = MutableLiveData<List<ChatModel>>()
    var chatsLoading = MutableLiveData(false)

    private val chatsListener = EventListener<QuerySnapshot> { value, error ->
        chatsLoading.postValue(false)
        chatsLoading.value = false
        value?.toObjects(ChatModel::class.java)?.let {
            chatsList.clear()
            chatsList.addAll(it)
            ChatController.chats.clear()
            ChatController.chats.addAll(chatsList)
            chats.postValue(chatsList)
        }
    }

    private var regisration: ListenerRegistration? = null

    fun stopListeningChats() {
        regisration?.remove()
    }

    fun startObservingChats() {
        chatsLoading.postValue(true)
        chats.postValue(chatsList)
        regisration = ChatController.observeChats(chatsListener)
    }
}