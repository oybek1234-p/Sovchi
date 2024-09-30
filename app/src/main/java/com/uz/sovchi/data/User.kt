package com.uz.sovchi.data

import androidx.annotation.Keep
import com.google.firebase.auth.FirebaseAuth

@Keep
data class User(
    var uid: String,
    var name: String,
    var phoneNumber: String,
    var gmail: String,
    var lastSeenTime: Long,
    var hasNomzod: Boolean,
    var unreadMessages: Int,
    var premium: Boolean,
    var premiumDate: Long,
    var unreadChats: Int,
    var blocked: Boolean,
    var liked: Int,
    var requests: Int,
    var pushToken: String
) {
    constructor() : this("", "", "", "", 0L, false, 0, false, 0L, 0, false, 0, 0, "")
}

fun User.isAdmin(): Boolean {
    return FirebaseAuth.getInstance().currentUser?.email == "samsunga7sm7000@gmail.com"
}

var REQUEST_MAX = 1
val User?.valid: Boolean get() = this?.uid?.isEmpty()?.not() ?: false
