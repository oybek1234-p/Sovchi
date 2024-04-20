package com.uz.sovchi.data.messages

data class Chat(
    var id: String,
    var userId: String,
    var chattingUserId: String,
    var chatName: String,
    var lastMessage: Any,
    var lastUpdateDate: Long
)