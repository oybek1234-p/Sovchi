package com.uz.sovchi

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import coil.load
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.stfalcon.imageviewer.StfalconImageViewer

inline fun String?.ifNotNullOrEmpty(method: (string: String) -> Unit) {
    if (isNullOrEmpty().not()) {
        method.invoke(this!!)
    }
}

fun ImageView.openImageViewer(urls: List<String>, startPos: Int = 0) {
    StfalconImageViewer.Builder(context, urls) { view, image ->
        Glide.with(view).load(image).into(view)
    }.allowSwipeToDismiss(true).withTransitionFrom(this).withStartPosition(startPos)
        .withHiddenStatusBar(false).show()
}

var gson: Gson? = null
    get() {
        if (field == null) {
            field = Gson().apply {
            }
        }
        return field
    }

fun openPhoneCall(activity: Activity, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL)
    intent.data = Uri.parse("tel:$phone") // Create a URI with the phone number
    activity.startActivity(intent)
}
