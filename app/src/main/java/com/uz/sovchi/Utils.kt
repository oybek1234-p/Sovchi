package com.uz.sovchi

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat.startActivity
import com.google.gson.Gson

inline fun String?.ifNotNullOrEmpty(method: (string: String) -> Unit) {
    if (isNullOrEmpty().not()) {
        method.invoke(this!!)
    }
}

var gson: Gson? = null
    get() {
        if (field == null) {
            field = Gson().apply {
            }
        }
        return field
    }

fun openPhoneCall(activity: Activity,phone: String) {
    val intent = Intent(Intent.ACTION_DIAL)
    intent.data = Uri.parse("tel:$phone") // Create a URI with the phone number
    activity.startActivity(intent)
}
