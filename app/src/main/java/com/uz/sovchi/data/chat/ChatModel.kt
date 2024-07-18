package com.uz.sovchi.data.chat

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
    var createDate: Long
) {
    constructor() : this(
        "",
        "",
        "",
        false,
        0,
        "",
        "",
        "",
        "",
        0,
        0
    )
}

data class ChatMessageModel(
    var id: String,
    var chatId: String,
    var message: String,
    var senderId: String,
    var receiverId: String,
    var date: String,
    var photo: String
) {
    constructor(): this(
        "",
        "",
        "",
        "",
        "",
        "",
        ""
    )
}