package com.uz.sovchi.data.messages

data class Chat(
    var id: String,
    var firstUserId: String,
    var firstUserName: String,
    var secondUserId: String,
    var secondUserName: String,
    var lastMessage: String
)