package com.uz.sovchi

import android.app.Application
import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize

lateinit var appContext: Context

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this
        initFirebase()
    }

    private fun initFirebase() {
        Firebase.initialize(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
    }
}