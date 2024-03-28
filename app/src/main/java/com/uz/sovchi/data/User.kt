package com.uz.sovchi.data

data class User(
    var uid: String,
    var name: String,
    var phoneNumber: String,
    var lastSeenTime: Long,
    var hasNomzod: Boolean
) {
    constructor() : this("", "", "", 0L,false)
}

val User?.valid: Boolean get() = this?.uid?.isEmpty()?.not() ?: false
