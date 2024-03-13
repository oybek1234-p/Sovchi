package com.uz.sovchi.notification

import com.google.firebase.messaging.FirebaseMessaging

object AppNotification {

    fun getToken(result: (token: String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            result.invoke(it.result)
        }
    }
}