package com.uz.sovchi.data.chat

import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.uz.sovchi.data.ImageUploader
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.utils.FirebaseUtils.toObjectSafe
import com.uz.sovchi.data.utils.FirebaseUtils.toObjectsSafe
import com.uz.sovchi.data.valid
import com.uz.sovchi.handleException
import com.uz.sovchi.postVal
import com.uz.sovchi.ui.photo.PickPhotoFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ChatController {

    private val chatsReference = FirebaseFirestore.getInstance().collection("chatsSecure")
    val messagesReference = FirebaseFirestore.getInstance().collection("chatMessagesSecure")
    private val blockedUsersReference = FirebaseFirestore.getInstance().collection("blockUsers")

    var chats = arrayListOf<ChatModel>()
    val cacheMessages = hashMapOf<String, ArrayList<ChatMessageModel>>()
    var chatsObserving = false

    var uploadingPhotosMessageIds = mutableSetOf<String>()
    var uploadingPhotosLive = MutableLiveData(uploadingPhotosMessageIds)

    var currentOpenedChatNomzodId = ""
    suspend fun sendMessage(
        chatModel: ChatMessageModel, done: (
            success: Boolean, updated: ChatMessageModel?
        ) -> Unit
    ) {
        val uploadModel = {
            val copy = chatModel.copy(date = FieldValue.serverTimestamp())
            messagesReference.document(copy.id).set(copy).addOnSuccessListener {
                messagesReference.document(copy.id).get().addOnSuccessListener {
                    done.invoke(true, it.toObjectSafe(ChatMessageModel::class.java, copy))
                }.addOnFailureListener {
                    done.invoke(true, copy)
                }
            }.addOnFailureListener {
                done.invoke(false, null)
            }
        }
        if (chatModel.photo.isNotEmpty()) {
            uploadingPhotosMessageIds.add(chatModel.id)
            uploadingPhotosLive.postVal(uploadingPhotosMessageIds)
            ImageUploader.uploadImage(
                PickPhotoFragment.Image(chatModel.photo), ImageUploader.UploadImageTypes.CHAT_PHOTO
            ) {
                chatModel.photo = it ?: ""

                uploadingPhotosMessageIds.remove(chatModel.id)
                uploadingPhotosLive.postVal(uploadingPhotosMessageIds)
                uploadModel.invoke()
            }
        } else {
            uploadModel.invoke()
        }
    }

    fun unblockUser(nomzodId: String) {
        blockedUsersReference.document(LocalUser.user.uid + nomzodId).delete()
    }

    fun blockUser(nomzodId: String, chatId: String, report: String) {
        if (LocalUser.user.valid.not()) return
        val blockModel = BlockModel().apply {
            userId = LocalUser.user.uid
            this.nomzodId = nomzodId
            this.reportText = report
            this.date = System.currentTimeMillis()
        }
        chats.removeAll { it.userId == nomzodId }
        cacheMessages[chatId]?.clear()
        blockedUsersReference.document(LocalUser.user.uid + nomzodId).set(blockModel)
        chatsReference.document(LocalUser.user.uid + nomzodId).delete()
    }

    fun checkBlocked(nomzodId: String, block: (blocked: Boolean) -> Unit): ListenerRegistration {
        return blockedUsersReference.document(nomzodId + LocalUser.user.uid)
            .addSnapshotListener { value, v ->
                block.invoke(
                    value?.exists() == true
                )
            }
    }

    fun checkMeBlocked(nomzodId: String, block: (blocked: Boolean) -> Unit) {
        blockedUsersReference.document(LocalUser.user.uid + nomzodId).get().addOnSuccessListener {
            block.invoke(it.exists())
        }.addOnFailureListener {
            block.invoke(false)
        }
    }

    fun setUnreadChatZero(chatId: String) {
        chatsReference.document(chatId).update(ChatModel::unreadCount.name, 0)
    }

    fun observeChatModel(
        id: String, eventListener: EventListener<DocumentSnapshot>
    ): ListenerRegistration {
        return chatsReference.document(id).addSnapshotListener(eventListener)
    }

    fun loadMessagesSecure(
        scope: CoroutineScope,
        chatId: String,
        senderId: String,
        startMessage: ChatMessageModel?,
        endMessage: ChatMessageModel?,
        limit: Long = 12L,
        done: (messages: List<ChatMessageModel>) -> Unit
    ) {
        var query: Query =
            messagesReference.orderBy(ChatMessageModel::date.name, Query.Direction.DESCENDING)
                .whereArrayContains("users", LocalUser.user.uid).whereEqualTo("chatId", chatId)
                .whereEqualTo(ChatMessageModel::deleted.name, false).limit(limit)

        if (senderId.isEmpty().not()) {
            query = query.whereEqualTo(ChatMessageModel::senderId.name, senderId)
        }
        if (startMessage != null) {
            query = query.startAfter(startMessage.date)
        }
        if (endMessage != null) {
            query = query.endBefore(endMessage.date)
        }

        query.get().addOnSuccessListener {
            scope.launch(Dispatchers.Default) {
                val messages = it.toObjectsSafe(ChatMessageModel::class.java)
                withContext(Dispatchers.Main) {
                    var list = cacheMessages[chatId]
                    if (list == null) {
                        list = ArrayList()
                        cacheMessages[chatId] = list
                    }
                    list.addAll(messages)
                    done.invoke(messages)
                }
            }
        }.addOnFailureListener {
            handleException(it)
        }
    }

    fun loadChatByChatId(chatId: String, done: (model: ChatModel?) -> Unit) {
        if (chatId.isEmpty()) return
        chatsReference.whereEqualTo(ChatModel::chatId.name, chatId).get().addOnSuccessListener {
            MainScope().launch {
                val model = it.toObjectsSafe(ChatModel::class.java).firstOrNull()
                done.invoke(model)
            }
        }.addOnFailureListener {
            done.invoke(null)
        }
    }

    fun loadChatByNomzodId(nomzodId: String, done: (model: ChatModel?) -> Unit) {
        if (nomzodId.isEmpty()) return
        chatsReference.whereArrayContains("users", LocalUser.user.uid)
            .whereEqualTo(ChatModel::myId.name, LocalUser.user.uid)
            .whereEqualTo(ChatModel::userId.name, nomzodId).get().addOnSuccessListener {
                MainScope().launch {
                    val model = it.toObjectsSafe(ChatModel::class.java).firstOrNull()
                    done.invoke(model)
                }
            }.addOnFailureListener {
                done.invoke(null)
            }
    }

    private var preloadingChats = false
    private var preloadingMessages = false

    private fun preloadMessages(scope: CoroutineScope) {
        if (preloadingMessages) return
        chats.sortedByDescending { it.lastDate }.forEachIndexed { index, chatModel ->
            if (index < 5) {
                loadMessagesSecure(scope, chatModel.chatId, "", null, null, limit = 12) {}
            }
        }
    }

    fun preloadChats(scope: CoroutineScope) {
        if (preloadingChats) {
            return
        }
        chatsReference.orderBy(ChatModel::lastDate.name, Query.Direction.DESCENDING).limit(8)
            .whereEqualTo(ChatModel::myId.name, LocalUser.user.uid)
            .whereArrayContains("users", LocalUser.user.uid).get().addOnSuccessListener {
                if (chatsObserving) return@addOnSuccessListener
                scope.launch(Dispatchers.Default) {
                    val list = it.toObjectsSafe(ChatModel::class.java)
                    chats.addAll(list)
                    preloadingChats = false
                    preloadMessages(scope)
                }
            }
    }

    fun observeDeleteMessages(
        chatId: String, eventListener: EventListener<QuerySnapshot>
    ): ListenerRegistration {
        val query =
            messagesReference.orderBy(ChatMessageModel::date.name, Query.Direction.DESCENDING)
                .whereArrayContains("users", LocalUser.user.uid).whereEqualTo("chatId", chatId)
                .whereEqualTo(ChatMessageModel::deleted.name, true).limit(15)
        return query.addSnapshotListener(eventListener)
    }

    fun observeChats(eventListener: EventListener<QuerySnapshot>): ListenerRegistration {
        chatsObserving = true
        val register =
            chatsReference.orderBy(ChatModel::lastDate.name, Query.Direction.DESCENDING).limit(200)
                .whereEqualTo(ChatModel::myId.name, LocalUser.user.uid)
                .whereArrayContains("users", LocalUser.user.uid).addSnapshotListener(eventListener)
        return register
    }

    fun deleteMessage(messageModel: ChatMessageModel, chatModel: ChatModel) {
        messagesReference.document(messageModel.id).update(
            ChatMessageModel::deleted.name, true
        ).addOnFailureListener {
            handleException(it)
        }
        cacheMessages.values.forEach { it ->
            it.removeIf { it.id == messageModel.id }
        }
        //Update chat model
        if (chatModel.lastMessage == messageModel.message) {
            chatsReference.document(chatModel.id).update(ChatModel::lastMessage.name, "")
                .addOnFailureListener {
                    handleException(it)
                }
            chatsReference.document(messageModel.receiverId + messageModel.senderId)
                .update(ChatModel::lastMessage.name, "").addOnFailureListener {
                    handleException(it)
                }

        }
    }
}