package com.uz.sovchi

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import androidx.core.view.isVisible
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.gson.Gson
import com.stfalcon.imageviewer.StfalconImageViewer
import com.uz.sovchi.data.LocalUser

inline fun String?.ifNotNullOrEmpty(method: (string: String) -> Unit) {
    if (isNullOrEmpty().not()) {
        method.invoke(this!!)
    }
}

fun ImageView.openImageViewer(urls: List<String>, startPos: Int = 0, show: Boolean = true) {
    StfalconImageViewer.Builder(context, urls) { view, image ->
        view.loadPhoto(image, show.not())
    }.allowSwipeToDismiss(true).withTransitionFrom(this).withStartPosition(startPos)
        .withHiddenStatusBar(false).show()
}

var gson: Gson? = null
    get() {
        if (field == null) {
            field = Gson()
        }
        return field
    }

fun AdView.loadAd() {
    if (LocalUser.user.premium) {
        isVisible = false
        return
    }
    isVisible = true
    val adRequest = AdRequest.Builder().build()
    loadAd(adRequest)
}

fun openPhoneCall(activity: Activity, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL)
    intent.data = Uri.parse("tel:$phone") // Create a URI with the phone number
    activity.startActivity(intent)
}
