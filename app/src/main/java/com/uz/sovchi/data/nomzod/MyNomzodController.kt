package com.uz.sovchi.data.nomzod

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.uz.sovchi.appContext
import com.uz.sovchi.data.ImageUploader
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.verify.VerificationData
import com.uz.sovchi.gson
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.photo.PickPhotoFragment

object MyNomzodController {

    var nomzod: Nomzod = Nomzod()
    var nomzodLive = MutableLiveData<Nomzod>()
    var nomzodLoading = MutableLiveData(false)
    private var nomzodSharedPref = appContext.getSharedPreferences("nomzod", Context.MODE_PRIVATE)
    private var verificationCollection =
        FirebaseFirestore.getInstance().collection("verificationData")

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

    suspend fun uploadVerificationData(
        nomzodId: String, verificationData: VerificationData, done: () -> Unit
    ) {
        var divorcedUploaded = false
        var passportUploaded = false
        var selfieUploaded = false

        val saveData = {
            if (divorcedUploaded && passportUploaded && selfieUploaded) {
                verificationCollection.document(nomzodId).set(verificationData)
                done.invoke()
            }
        }
        if (verificationData.passportPhoto.isNullOrEmpty()) {
            passportUploaded = true
        } else {
            ImageUploader.uploadImage(PickPhotoFragment.Image(verificationData.passportPhoto!!)) {
                passportUploaded = true
                verificationData.passportPhoto = it
                saveData()
            }
        }
        if (verificationData.selfiePhoto.isNullOrEmpty()) {
            selfieUploaded = true
        } else {
            ImageUploader.uploadImage(PickPhotoFragment.Image(verificationData.selfiePhoto!!)) {
                selfieUploaded = true
                verificationData.selfiePhoto = it
                saveData()
            }
        }
        if (verificationData.divorcePhoto.isNullOrEmpty()) {
            divorcedUploaded = true
        } else {
            ImageUploader.uploadImage(PickPhotoFragment.Image(verificationData.divorcePhoto!!)) {
                divorcedUploaded = true
                verificationData.divorcePhoto = it
                saveData()
            }
        }
        saveData()
    }

    fun loadVerificationInfo(
        nomzodId: String, onSuccess: (result: VerificationData?) -> Unit
    ) {
        if (nomzodId.isEmpty()) return
        verificationCollection.document(nomzodId).get().addOnSuccessListener {
            onSuccess.invoke(it.toObject(VerificationData::class.java))
        }.addOnFailureListener {
            onSuccess.invoke(null)
        }
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
        listenerRegistration = NomzodsController.nomzodlarReference.document(userId)
            .addSnapshotListener(object : EventListener<DocumentSnapshot> {
                override fun onEvent(
                    value: DocumentSnapshot?, error: FirebaseFirestoreException?
                ) {
                    val result = value?.toObject(Nomzod::class.java)
                    nomzod = result ?: Nomzod()
                    nomzodLive.postValue(nomzod)
                    saveCache()
                    nomzodLoading.postValue(false)
                }
            })
    }

    fun updateNomzod(
        nomzod: Nomzod, server: Boolean, onSuccess: (success: Boolean) -> Unit
    ) {
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