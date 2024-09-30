package com.uz.sovchi.ui.auth

import android.content.Context
import com.google.android.gms.auth.api.phone.SmsRetriever

class SmsRetrieverController(val context: Context) {

    private val smsRetriever by lazy {
        SmsRetriever.getClient(context)
    }

    fun start() {
        start {}
    }

    private fun start(done: (success: Boolean) -> Unit) {
        val task = smsRetriever.startSmsRetriever()
        task.addOnSuccessListener {
            done.invoke(true)
        }
    }

}