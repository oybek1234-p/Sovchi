package com.uz.sovchi.ui.messages

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.messages.Message
import com.uz.sovchi.data.messages.MessagesRepository

class MessagesViewModel : ViewModel() {

    private var repository = MessagesRepository()

    val messagesList = arrayListOf<Message>()
    val messages = MutableLiveData<ArrayList<Message>>()
    val loading = MutableLiveData(false)
    val empty = MutableLiveData(false)

    fun refresh() {
        messagesList.clear()
        messages.postValue(messagesList)
        loadMessages()
    }

    fun loadMessages() {
        if (loading.value == true) return
        val lastMessage = messagesList.lastOrNull()
        loading.postValue(true)
        empty.postValue(false)
        repository.loadMessages(lastMessage?.id, LocalUser.user.uid, 8) {
            loading.postValue(false)
            messagesList.addAll(it)
            if (messagesList.isEmpty() && it.isEmpty()) {
                empty.postValue(true)
            }else {
                empty.postValue(false)
            }
            messages.postValue(messagesList)
        }
    }
}