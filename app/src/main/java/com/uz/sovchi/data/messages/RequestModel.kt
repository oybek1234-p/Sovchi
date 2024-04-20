package com.uz.sovchi.data.messages

enum class RequestStatus(val code: Int) {
    Requested(0),
    Rejected(1),
    Approved(2)
}

data class RequestModel(
    var id: String,
    var userId: String,
    var requestForUserId: String,
    var status: Int,
    var nomzodName: String,
    var nomzodId: String,
    var date: Long
)