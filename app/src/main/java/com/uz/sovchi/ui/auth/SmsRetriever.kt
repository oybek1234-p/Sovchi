package com.uz.sovchi.ui.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.auth.api.phone.SmsRetriever.SMS_RETRIEVED_ACTION

class SmsRetriever(
    private val context: Context,
    private val result: (code: String, errorMessage: String?) -> Unit
) {

    private var broadcastReceiver: SmsRetrieverBroadcast? = null
    private val smsController = SmsRetrieverController(context)

    @SuppressLint("NewApi")
    fun start() {
        broadcastReceiver = SmsRetrieverBroadcast(result)
        context.registerReceiver(
            broadcastReceiver!!,
            IntentFilter(SMS_RETRIEVED_ACTION),
            SmsRetriever.SEND_PERMISSION, null,
            Context.RECEIVER_NOT_EXPORTED
        )
        smsController.start()
    }

    fun unregister() {
        try {
            context.unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            //
        }
    }
}