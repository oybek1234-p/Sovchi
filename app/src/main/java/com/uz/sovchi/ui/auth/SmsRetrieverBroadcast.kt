package com.uz.sovchi.ui.auth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.uz.sovchi.ExceptionHandler
import com.uz.sovchi.PhoneUtils

class SmsRetrieverBroadcast(private val result: (code: String, errorMessage: String?) -> Unit) :
    BroadcastReceiver() {

    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1 != null)
            try {
                if (SmsRetriever.SMS_RETRIEVED_ACTION == p1.action) {
                    val extras = p1.extras ?: return
                    val status: Status = extras[SmsRetriever.EXTRA_STATUS] as Status
                    when (status.statusCode) {
                        CommonStatusCodes.SUCCESS -> {
                            val message: String? = extras[SmsRetriever.EXTRA_SMS_MESSAGE] as String?
                            val code = PhoneUtils.getCodeFromSms(message ?: "")
                            result.invoke(code, null)
                        }

                        CommonStatusCodes.TIMEOUT -> {
                            result.invoke("", CommonStatusCodes.TIMEOUT.toString())
                        }
                    }
                }
            } catch (e: Exception) {
                ExceptionHandler.handle(e)
            }
    }

}
