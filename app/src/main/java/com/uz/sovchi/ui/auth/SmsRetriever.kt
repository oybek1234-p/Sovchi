package com.uz.sovchi.ui.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.auth.api.phone.SmsRetriever.SMS_RETRIEVED_ACTION

class SmsRetriever(
    private val context: Context,
    private val result: (code: String, errorMessage: String?) -> Unit
) {

    private var broadcastReceiver: SmsRetrieverBroadcast? = null
    private val smsController = SmsRetrieverController(context)

    fun start() {
        try {
            broadcastReceiver = SmsRetrieverBroadcast(result)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.registerReceiver(
                    broadcastReceiver!!,
                    IntentFilter(SMS_RETRIEVED_ACTION),
                    SmsRetriever.SEND_PERMISSION, null,
                    Context.RECEIVER_NOT_EXPORTED
                )
                smsController.start()
            }
        }catch (e: Exception) {
            //Ignore
        }
    }

    fun unregister() {
        try {
            context.unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            //
        }
    }
}