package com.uz.sovchi.data.chat

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue

@Keep
data class ChatModel(
    var id: String,
    var myId: String,
    var lastMessage: String,
    var lastMessageByMe: Boolean,
    var unreadCount: Int,
    var userId: String,
    var userName: String,
    var userImage: String,
    var chatId: String,
    var lastDate: Long,
    var createDate: Long,
    var users: List<String> = emptyList()
) {
    constructor() : this(
        "", "", "", false, 0, "", "", "", "", 0, 0
    )
}

@Keep
data class ChatMessageModel(
    var id: String,
    var chatId: String,
    var message: String,
    var senderId: String,
    var receiverId: String,
    var date: Any? = FieldValue.serverTimestamp(),
    var photo: String,
    var voice: String,
    var deleted: Boolean = false,
    var users: List<String> = listOf(),
    @Exclude var uploading: Boolean = false
) {
    constructor() : this(
        "", "", "", "", "", "", "", ""
    )
}