package com.uz.sovchi.data.messages

import com.uz.sovchi.data.nomzod.Nomzod

object RequestStatus {
    const val requested = 0
    const val accepted = 1
    const val rejected = 2
}

data class RequestModel(
    var id: String,
    var requestedUserId: String,
    var requestedNomzod: Nomzod,
    var nomzod: Nomzod,
    var nomzodUserId: String,
    var nomzodId: String,
    var status: Int
){
    constructor() : this("", "", Nomzod(),Nomzod(), "", "", 0)
}