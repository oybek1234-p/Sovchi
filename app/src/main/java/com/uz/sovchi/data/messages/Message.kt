package com.uz.sovchi.data.messages

data class Message(
    var id: String, var data: Any?, var type: Int, var userId: String,var date: Long
){
    constructor(): this("",null,-1,"",System.currentTimeMillis())
}



