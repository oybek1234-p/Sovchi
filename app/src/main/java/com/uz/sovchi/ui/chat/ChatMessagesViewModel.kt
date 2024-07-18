package com.uz.sovchi.ui.chat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ListenerRegistration
import com.uz.sovchi.DateUtils
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.UserRepository
import com.uz.sovchi.data.chat.ChatController
import com.uz.sovchi.data.chat.ChatMessageModel
import com.uz.sovchi.data.chat.ChatModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ChatMessagesViewModel : ViewModel() {

    var messagesList = arrayListOf<ChatMessageModel>()
    var messages: MutableLiveData<ArrayList<ChatMessageModel>> = MutableLiveData()

    val chatModel = MutableLiveData<ChatModel?>()
    val loadingAll = MutableLiveData(false)
    val loadingMessages = MutableLiveData(false)
    var nomzodId = ""
    var nomzodName = ""
    var nomzodPhoto = ""
    val lastOnlineDate = MutableLiveData("")
    var lastSeenListener: ValueEventListener? = null
    var chatId: String = ""

    var meBlocked = false
    var isBlocked = false
    var blocked = MutableLiveData(false)

    private fun observeLastOnline() {
        if (nomzodId.isEmpty()) return
        lastSeenListener = UserRepository.observeLastSeen(nomzodId) {
            val date = DateUtils.formatDate(it)
            lastOnlineDate.postValue(date)
        }
    }

    private fun removeLastOnlineListener() {
        lastSeenListener?.let {
            UserRepository.removeLastSeenListener(it)
        }
    }

    private var blockListenerRegistration: ListenerRegistration? = null

    private fun checkBlocked() {
        blockListenerRegistration = ChatController.checkBlocked(nomzodId) {
            isBlocked = it
            blocked.postValue(it)
        }
    }

    private fun checkMeBlocked() {
        ChatController.checkMeBlocked(nomzodId) {
            isBlocked = it
            meBlocked = it
            blocked.postValue(it)
        }
    }

    private fun setChatModel(chatModel: ChatModel) {
        if (isBlocked) return
        this.chatModel.value = chatModel
        this.chatModel.postValue(chatModel)
        observeChatModel(chatModel.id)
        setUnreadCountZero()
        loadMessages()
    }

    fun setUnreadCountZero() {
        if (chatModel.value != null) {
            if (chatModel.value!!.unreadCount > 0) {
                ChatController.setUnreadChatZero(chatModel.value!!.id)
            }
        }
    }

    fun sendMessage(text: String, photo: String) {
        if (isBlocked) return
        val chat = chatModel.value ?: return
        val message = ChatMessageModel(
            System.currentTimeMillis().toString(),
            chat.chatId,
            text,
            LocalUser.user.uid,
            nomzodId,
            System.currentTimeMillis().toString(),
            photo
        )
        if (photo.isNotEmpty()){
            message.message = "foto"
        }
        viewModelScope.launch {
            ChatController.sendMessage(message) {}
            messagesList.add(0, message)
            messages.postValue(messagesList)
        }
    }

    private var chatModelListener = EventListener<DocumentSnapshot> { value, error ->
        val model = value?.toObject(ChatModel::class.java)
        if (model != null) {
            chatModel.postValue(model)
            if (model.unreadCount > 0) {
                loadNewMessages()
            }
        }
    }

    private var chatModelRegister: ListenerRegistration? = null

    override fun onCleared() {
        super.onCleared()
        chatModelRegister?.remove()
        removeLastOnlineListener()
        blockListenerRegistration?.remove()
    }

    private fun observeChatModel(
        id: String
    ) {
        chatModelRegister = ChatController.observeChatModel(id, chatModelListener)
    }

    fun loadByChatId(id: String) {
        loadingAll.postValue(true)

        chatId = id

        checkBlocked()
        checkMeBlocked()
        observeLastOnline()
        ChatController.loadChatModelById(id) {
            loadingAll.postValue(false)
            if (it == null) {
                val tempChatModel = ChatModel(
                    LocalUser.user.uid + nomzodId,
                    LocalUser.user.uid,
                    "",
                    false,
                    0,
                    nomzodId,
                    nomzodName,
                    nomzodPhoto,
                    System.currentTimeMillis().toString(),
                    System.currentTimeMillis(),
                    System.currentTimeMillis()
                )
                setChatModel(tempChatModel)
            } else {
                viewModelScope.launch {
                    it.let {
                        setChatModel(it)
                    }
                }
            }
        }
    }

    fun loadByNomzodId(id: String) {
        loadingAll.postValue(true)

        chatId = id

        checkBlocked()
        checkMeBlocked()
        observeLastOnline()
        ChatController.loadChatModel(id) {
            loadingAll.postValue(false)
            if (it == null) {
                val tempChatModel = ChatModel(
                    LocalUser.user.uid + nomzodId,
                    LocalUser.user.uid,
                    "",
                    false,
                    0,
                    nomzodId,
                    nomzodName,
                    nomzodPhoto,
                    System.currentTimeMillis().toString(),
                    System.currentTimeMillis(),
                    System.currentTimeMillis()
                )
                setChatModel(tempChatModel)
            } else {
                viewModelScope.launch {
                    it.let {
                        setChatModel(it)
                    }
                }
            }
        }
    }

    fun loadMessages() {
        if (isBlocked) return
        if (loadingMessages.value == true) return
        val chat = chatModel.value ?: return
        loadingMessages.postValue(true)
        val messagesL = messages.value
        ChatController.loadMessages(chatId = chat.chatId, messagesL?.lastOrNull(), null) {
            if (isBlocked) return@loadMessages
            loadingMessages.postValue(false)
            viewModelScope.launch {
                messagesList.addAll(it)
                val distinct = messagesList.distinctBy { it.id }
                messagesList.clear()
                messagesList.addAll(distinct)
                messagesList.sortByDescending { it.date.toLongOrNull() }
                messages.postValue(messagesList)
            }
        }
    }

    private var loadingNewMessages = MutableLiveData(false)
    private var loadingNewJob: Job? = null
    private fun loadNewMessages() {
        if (isBlocked) return
        loadingNewJob = viewModelScope.launch {
            val chat = chatModel.value ?: return@launch
            loadingNewMessages.postValue(true)
            val messagesL = messagesList
            ChatController.loadMessages(chatId = chat.chatId, null, messagesL.firstOrNull()) {
                loadingNewMessages.postValue(false)
                viewModelScope.launch {
                    messagesList.addAll(it)
                    messagesList.sortByDescending { it.date }
                    messagesList = messagesList.distinctBy { it.id } as ArrayList<ChatMessageModel>
                    messages.postValue(messagesList)
                }
            }
        }
    }

    fun blockUser() {
        ChatController.blockUser(nomzodId)
        isBlocked = true
        blocked.postValue(true)
    }

}