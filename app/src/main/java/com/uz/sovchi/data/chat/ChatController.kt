package com.uz.sovchi.data.chat

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.uz.sovchi.data.ImageUploader
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.data.valid
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.photo.PickPhotoFragment

object ChatController {

    private val chatsReference = FirebaseFirestore.getInstance().collection("chats")
    private val messagesReference = FirebaseFirestore.getInstance().collection("chatMessages")
    private val blockedUsersReference = FirebaseFirestore.getInstance().collection("blockUsers")

    var chats = arrayListOf<ChatModel>()
    val cacheMessages = hashMapOf<String, ArrayList<ChatMessageModel>>()
    val cacheChats = hashMapOf<String,ChatModel>()

    fun deleteMessage(messageId: String) {

    }

    private fun newId() = System.currentTimeMillis().toString()

    suspend fun sendMessage(
        chatModel: ChatMessageModel, done: (success: Boolean) -> Unit
    ) {
        val uploadModel = {
            messagesReference.document(chatModel.id).set(chatModel).addOnSuccessListener {
                done.invoke(true)
            }.addOnFailureListener {
                done.invoke(false)
            }
        }
        if (chatModel.photo.isNotEmpty()) {
            ImageUploader.uploadImage(PickPhotoFragment.Image(chatModel.photo)) {
                chatModel.photo = it ?: ""
                uploadModel.invoke()
            }
        } else {
            uploadModel.invoke()
        }
    }

    fun blockUser(nomzodId: String) {
        if (LocalUser.user.valid.not()) return
        val blockModel = BlockModel().apply {
            userId = LocalUser.user.uid
            this.nomzodId = nomzodId
        }
        blockedUsersReference.document(LocalUser.user.uid + nomzodId).set(blockModel)
        chatsReference.document(LocalUser.user.uid + nomzodId).delete()
    }

    fun checkBlocked(nomzodId: String, block: (blocked: Boolean) -> Unit): ListenerRegistration {
        return blockedUsersReference.document(nomzodId + LocalUser.user.uid)
            .addSnapshotListener { value, _ ->
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

    fun loadMessages(
        chatId: String,
        startMessage: ChatMessageModel?,
        endMessage: ChatMessageModel?,
        done: (messages: List<ChatMessageModel>) -> Unit
    ) {
        var query =
            messagesReference.orderBy(ChatMessageModel::date.name, Query.Direction.DESCENDING)
                .limit(14).whereEqualTo(ChatMessageModel::chatId.name, chatId)
        if (startMessage != null) {
            query = query.startAfter(startMessage.date)
        }
        if (endMessage != null) {
            query = query.endBefore(endMessage.date)
        }
        query.get().addOnSuccessListener {
            val messages = it.toObjects(ChatMessageModel::class.java)
            done.invoke(messages)
        }
    }

    fun uploadPhoto() {
        MyNomzodController
    }

    fun loadChatModel(nomzodId: String, done: (model: ChatModel?) -> Unit) {
        if (nomzodId.isEmpty()) return
        chatsReference
            .whereEqualTo(ChatModel::myId.name, LocalUser.user.uid)
            .whereEqualTo(ChatModel::userId.name, nomzodId).get().addOnSuccessListener {
                val model = it.toObjects(ChatModel::class.java).firstOrNull()
                done.invoke(model)
            }.addOnFailureListener {
                done.invoke(null)
            }
    }

    fun loadChatModelById(chatId: String, done: (model: ChatModel?) -> Unit) {
        chatsReference
            .whereEqualTo(ChatModel::chatId.name, chatId)
            .whereEqualTo(ChatModel::myId.name,LocalUser.user.uid)
            .get().addOnSuccessListener {
                val model = it.toObjects(ChatModel::class.java).firstOrNull()
                if (model != null) {
                    cacheChats[model.id] = model
                }
                done.invoke(model)
            }.addOnFailureListener {
                done.invoke(null)
            } }

    fun observeChats(eventListener: EventListener<QuerySnapshot>): ListenerRegistration {
        val register = chatsReference.orderBy(ChatModel::lastDate.name, Query.Direction.DESCENDING)
            .whereEqualTo(ChatModel::myId.name, LocalUser.user.uid)
            .addSnapshotListener(eventListener)
        return register
    }
}