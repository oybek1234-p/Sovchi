package com.uz.sovchi.ui.chat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.UserRepository
import com.uz.sovchi.data.chat.ChatController
import com.uz.sovchi.data.chat.ChatMessageModel
import com.uz.sovchi.data.chat.ChatModel
import com.uz.sovchi.data.utils.DateUtils
import com.uz.sovchi.data.utils.FirebaseUtils.toObjectsSafe
import com.uz.sovchi.handleException
import com.uz.sovchi.postVal
import com.uz.sovchi.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.UUID

class ChatMessagesViewModel : ViewModel() {

    var messagesList = arrayListOf<ChatMessageModel>()
    var messages: MutableLiveData<ArrayList<ChatMessageModel>> = MutableLiveData()

    var newMessageAdded = false

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
            val date = DateUtils.getUserLastSeenTime(it)
            lastOnlineDate.postVal(date)
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
            blocked.postVal(isBlocked || meBlocked)
        }
    }

    //Checks if I blocked that user
    private fun checkMeBlocked() {
        ChatController.checkMeBlocked(nomzodId) {
            meBlocked = it
            blocked.postVal(isBlocked || meBlocked)
        }
    }

    private fun setChatModel(chatModel: ChatModel) {
        if (isBlocked) return
        viewModelScope.launch {
            chatId = chatModel.chatId
            this@ChatMessagesViewModel.chatModel.value = chatModel
            this@ChatMessagesViewModel.chatModel.postVal(chatModel)
            observeChatModel(chatModel.id)
            setUnreadCountZero()
            if (hasCached.not()) {
                loadMessages()
            } else {
                loadingAll.postVal(false)
                observeDeleteMessages()
            }
        }
    }

    private fun setUnreadCountZero() {
        if (chatModel.value != null) {
            if (chatModel.value!!.unreadCount > 0) {
                ChatController.setUnreadChatZero(chatModel.value!!.id)
            }
        } else {
            showToast("Chat model null")
        }
    }

    fun sendMessage(text: String, photo: String, done: (success: Boolean) -> Unit) {
        if (isBlocked) return
        val chat = chatModel.value ?: return
        val message = ChatMessageModel(
            UUID.randomUUID().toString(),
            chat.chatId.ifEmpty { UUID.randomUUID().toString() },
            text,
            LocalUser.user.uid,
            nomzodId,
            0,
            photo,
            "",
            false,
            listOf(nomzodId, LocalUser.user.uid)
        )
        if (photo.isNotEmpty()) {
            message.message = "Rasm"
        }
        UserRepository.increaseRequest()
        viewModelScope.launch {
            newMessageAdded = true
            message.date = System.currentTimeMillis()
            ChatController.chats.find { it.userId == message.receiverId }?.apply {
                lastMessageByMe = true
                lastMessage = message.message
                lastDate = System.currentTimeMillis()
            }

            messagesList.add(0, message)
            messages.postVal(messagesList)

            ChatController.sendMessage(message) { success, updated ->
                if (success) {
                    updated?.let {
                        try {
                            val index = messagesList.indexOf(message)
                            messagesList.removeAt(index)
                            messagesList.add(index, it)
                            messages.postVal(messagesList)
                        } catch (e: Exception) {
                            handleException(e)
                        }
                    }
                    done.invoke(true)
                } else {
                    messagesList.remove(message)
                    messages.postVal(messagesList)
                    done.invoke(false)
                }
            }
            ChatController.cacheMessages[chatId] = messagesList
        }
    }

    private var chatModelListener = EventListener<DocumentSnapshot> { value, error ->
        val model = value?.toObject(ChatModel::class.java)
        if (model != null) {
            chatModel.value = model
            if (model.unreadCount > 0) {
                setUnreadCountZero()
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
        deleteMessagesRegister?.remove()
    }

    private fun observeChatModel(
        id: String
    ) {
        chatModelRegister = ChatController.observeChatModel(id, chatModelListener)
    }

    var hasCached = false

    fun loadByChatId(id: String) {
        loadingAll.postVal(true)
        observeLastOnline()
        ChatController.loadChatByChatId(id) {
            loadingAll.postVal(false)
            setChatModel(it!!)
        }
    }

    fun loadByNomzodId(id: String) {
        loadingAll.postVal(true)
        observeLastOnline()
        checkBlocked()
        checkMeBlocked()
        try {
            val cached = ChatController.cacheMessages.values.flatten()
                .filter { ((it.senderId == id || it.senderId == LocalUser.user.uid) && (it.receiverId == id || it.receiverId == LocalUser.user.uid)) }
            if (cached.isNotEmpty()) {
                messagesList.clear()
                messagesList.addAll(cached)
                if (cached.isNotEmpty()) {
                    hasCached = true
                }
                updateMessages()
            }
        } catch (e: Exception) {
            handleException(e)
        }
        viewModelScope.launch(Dispatchers.Default) {
            ChatController.loadChatByNomzodId(id) {
                //    loadingAll.postVal(false)
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
                    setChatModel(it)
                }
            }
        }

    }

    private var endReached = false

    fun loadMessages() : Boolean {
        if (isBlocked || loadingMessages.value == true || chatId.isEmpty() || endReached) return false

        loadingMessages.postVal(true)
        ChatController.loadMessagesSecure(
            viewModelScope, chatId, "", messagesList.lastOrNull(), null, limit = 18
        ) {
            if (isBlocked) return@loadMessagesSecure
            try {
                loadingMessages.postVal(false)
                if (loadingAll.value == true) {
                    loadingAll.postVal(false)
                }
                messagesList.addAll(it)
                if (it.isEmpty()) {
                    endReached = true
                }
                observeDeleteMessages()
                ChatController.cacheMessages[chatId] = messagesList
                updateMessages()
            } catch (e: Exception) {
                handleException(e)
            }
        }
        return true
    }

    private var updateJob: Job? = null
    private fun updateMessages() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch(Dispatchers.Default) {
            val newList = ArrayList<ChatMessageModel>(messagesList)
            Collections.copy(newList, messagesList)
            newList.distinctBy { it.id }
            newList.forEach {
                if (isActive.not()) {
                    return@launch
                }
                if (it.date == null) {
                    it.date = System.currentTimeMillis()
                }
            }
            if (isActive) {
                messagesList = newList
                messages.postVal(newList)
            }
        }
    }

    private var loadingNewMessages = MutableLiveData(false)
    private var loadingNewJob: Job? = null
    private fun loadNewMessages() {
        if (isBlocked) return
        if (chatId.isEmpty()) return
        if (loadingNewJob != null) return
        loadingNewJob = viewModelScope.launch {
            loadingNewMessages.postVal(true)
            val messagesL = messagesList
            ChatController.loadMessagesSecure(viewModelScope,
                chatId,
                nomzodId,
                null,
                messagesL.firstOrNull { it.senderId == nomzodId }) {
                try {
                    loadingNewMessages.postVal(false)
                    newMessageAdded = true
                    messagesList.addAll(0, it)
                    ChatController.cacheMessages[chatId] = messagesList
                    updateMessages()
                    loadingNewJob = null
                } catch (e: Exception) {
                    handleException(e)
                }
            }
        }
    }

    fun unblockUser() {
        ChatController.unblockUser(nomzodId)
        meBlocked = false
        blocked.postVal(isBlocked)
        loadByNomzodId(nomzodId)
    }

    fun blockUser(reportText: String) {
        ChatController.blockUser(nomzodId, chatId, reportText)
        meBlocked = true
        blocked.postVal(true)
    }

    private var isFirstDelete = true
    private var deleteMessageObserving = false
    private var deleteMessagesRegister: ListenerRegistration? = null
    private val deleteMessagesObserver = EventListener<QuerySnapshot> { value, error ->
        viewModelScope.launch(Dispatchers.Default) {
            try {
                var deletedItems = value?.toObjectsSafe(ChatMessageModel::class.java)
                deletedItems = deletedItems?.distinctBy { it.id }
                withContext(Dispatchers.Main) {
                    if (deletedItems.isNullOrEmpty().not()) {
                        deletedItems?.forEach {
                            val message = messagesList.find { mess -> it.id == mess.id }
                            message?.deleted = true
                        }
                        if (isFirstDelete) {
                            isFirstDelete = false
                        } else {
                            messages.postVal(messagesList)
                        }
                    }
                }
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }

    private fun observeDeleteMessages() {
        if (chatId.isEmpty()) return
        if (deleteMessageObserving) return
        if (deleteMessagesRegister != null) {
            deleteMessagesRegister?.remove()
            deleteMessagesRegister = null
        }
        deleteMessageObserving = true
        deleteMessagesRegister =
            ChatController.observeDeleteMessages(chatId, deleteMessagesObserver)
    }

    fun deleteMessage(message: ChatMessageModel) {
        val chat = chatModel.value
        if (chat != null) ChatController.deleteMessage(message, chat)
        messagesList.remove(message)
        messages.postVal(messagesList)
    }
}