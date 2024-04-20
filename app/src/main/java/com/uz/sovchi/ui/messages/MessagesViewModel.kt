package com.uz.sovchi.ui.messages

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.messages.Message
import com.uz.sovchi.data.messages.MessagesRepository
import com.uz.sovchi.showToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MessagesViewModel : ViewModel() {

    private var repository = MessagesRepository()

    val messagesList = arrayListOf<Message>()
    val messages = MutableLiveData<ArrayList<Message>>()
    val loading = MutableLiveData(false)
    val empty = MutableLiveData(false)

    fun refresh() {
        loadingJob?.cancel()
        loadingJob = null
        loading.value = false
        messagesList.clear()
        messages.postValue(messagesList)
        loadMessages()
    }

    private var loadingJob: Job? = null

    fun loadMessages() {
        if (loading.value == true) return
        loadingJob?.cancel()
        loadingJob = null
        loading.postValue(true)
        loading.value = true
        empty.postValue(false)
        loadingJob = viewModelScope.launch {
            val job = this
            val lastMessage = messagesList.lastOrNull()
            repository.loadMessages(lastMessage?.date, LocalUser.user.uid, 8) {
                if (job != loadingJob) return@loadMessages
                loading.postValue(false)
                loading.value = false
                messagesList.addAll(it)
                if (messagesList.isEmpty() && it.isEmpty()) {
                    empty.postValue(true)
                } else {
                    empty.postValue(false)
                }
                messages.postValue(messagesList)
            }
        }
    }
}