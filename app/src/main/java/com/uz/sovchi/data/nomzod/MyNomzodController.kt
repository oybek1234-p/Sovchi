package com.uz.sovchi.data.nomzod

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.uz.sovchi.appContext
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.gson

object MyNomzodController {

    var nomzod: Nomzod = Nomzod()
    var nomzodLive = MutableLiveData<Nomzod>()
    var nomzodLoading = MutableLiveData(false)
    private var nomzodSharedPref = appContext.getSharedPreferences("nomzod", Context.MODE_PRIVATE)

    private const val NOMZOD_TAG = "nomzod"

    fun getNomzod() {
        getCached()
        loadMyNomzodServer()
    }

    fun clear() {
        nomzodSharedPref.edit().clear().apply()
        nomzod = Nomzod()
        nomzodLive.postValue(nomzod)
        listenerRegistration?.remove()
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
    private var listenerRegistration: ListenerRegistration? = null


    private fun loadMyNomzodServer() {
        if (userId.isEmpty()) return
        nomzodLoading.postValue(true)
        listenerRegistration = NomzodsController.nomzodlarReference.document(userId).addSnapshotListener(object : EventListener<DocumentSnapshot>{
            override fun onEvent(value: DocumentSnapshot?, error: FirebaseFirestoreException?) {
                val result = value?.toObject(Nomzod::class.java)
                nomzod = result ?: Nomzod()
                nomzodLive.postValue(nomzod)
                saveCache()
                nomzodLoading.postValue(false)
            }
        })
    }

    fun updateNomzod(nomzod: Nomzod, server: Boolean, onSuccess: (success: Boolean) -> Unit) {
        this.nomzod = nomzod
        nomzodLive.postValue(nomzod)
        saveCache()
        if (server) {
            NomzodsController.nomzodlarReference.document(nomzod.id).set(nomzod)
                .addOnSuccessListener {
                    onSuccess.invoke(true)
                }.addOnFailureListener {
                    onSuccess.invoke(false)
                }
        }

    }
}