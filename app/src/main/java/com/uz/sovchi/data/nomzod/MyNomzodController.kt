package com.uz.sovchi.data.nomzod

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.uz.sovchi.appContext
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.gson

object MyNomzodController {

    var nomzod: Nomzod = Nomzod()
    var nomzodLive = MutableLiveData<Nomzod>()
    var nomzodLoading = MutableLiveData(false)
    private var nomzodSharedPref = appContext.getSharedPreferences("nomzod", Context.MODE_PRIVATE)

    const val NOMZOD_TAG = "nomzod"

    fun getNomzod() {
        getCached()
        loadMyNomzodServer()
    }

    fun clear() {
        nomzodSharedPref.edit().clear().apply()
        nomzod = Nomzod()
        nomzodLive.postValue(nomzod)
    }
    fun saveCache() {
        val editor = nomzodSharedPref.edit()
        editor.putString(NOMZOD_TAG, gson!!.toJson(nomzod))
        editor.apply()
    }

    private fun getCached() {
        val json = nomzodSharedPref.getString(NOMZOD_TAG, null)
        if (json != null) {
            nomzod = gson!!.fromJson(json, Nomzod::class.java)
            nomzodLive.value = nomzod
        }
    }

    val userId: String get() = LocalUser.user.uid

    private fun loadMyNomzodServer() {
        if (userId.isEmpty()) return
        nomzodLoading.postValue(true)
        NomzodsController.nomzodlarReference.document(userId).get().addOnSuccessListener {
            val result = it.toObject(Nomzod::class.java)
            nomzod = result ?: Nomzod()
            nomzodLive.postValue(nomzod)
            saveCache()
            nomzodLoading.postValue(false)
        }.addOnFailureListener {
            nomzodLoading.postValue(false)
        }
    }

    fun updateNomzod(nomzod: Nomzod, server: Boolean, onSuccess: (success: Boolean) -> Unit) {
        this.nomzod = nomzod
        nomzodLive.postValue(nomzod)
        saveCache()
        if (server) {
            NomzodsController.nomzodlarReference.document(userId).set(nomzod).addOnSuccessListener {
                onSuccess.invoke(true)
            }.addOnFailureListener {
                onSuccess.invoke(false)
            }
        }

    }
}