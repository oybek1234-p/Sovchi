package com.uz.sovchi.ad

import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.uz.sovchi.appContext

object AdMobApp {

    fun init(initialization: (r: InitializationStatus) -> Unit) {
        MobileAds.initialize(
            appContext
        ) {
            initialization.invoke(it)
        }
    }
}