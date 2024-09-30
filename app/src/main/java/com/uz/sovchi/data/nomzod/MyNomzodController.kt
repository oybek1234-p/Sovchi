package com.uz.sovchi.data.nomzod

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.uz.sovchi.appContext
import com.uz.sovchi.data.ImageUploader
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.filter.MyFilter
import com.uz.sovchi.data.utils.FirebaseUtils.toObjectSafe
import com.uz.sovchi.data.verify.VerificationData
import com.uz.sovchi.gson
import com.uz.sovchi.handleException
import com.uz.sovchi.postVal
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.photo.PickPhotoFragment
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

object MyNomzodController {

    var nomzod: Nomzod = Nomzod()
    var nomzodLive = MutableLiveData<Nomzod>()
    var nomzodLoading = MutableLiveData(false)

    private val nomzodSharedPref: SharedPreferences by lazy {
        appContext.getSharedPreferences("nomzod", Context.MODE_PRIVATE)
    }

    private val verificationCollection: CollectionReference by lazy {
        FirebaseFirestore.getInstance().collection("verificationData")
    }

    private const val NOMZOD_TAG = "nomzod"

    fun getNomzod() {
        getCached()
        loadMyNomzodServer()
    }

    fun clear() {
        nomzodSharedPref.edit().clear().apply()
        nomzod = Nomzod()
        nomzodLive.postVal(nomzod)
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
            ImageUploader.uploadImage(
                PickPhotoFragment.Image(verificationData.passportPhoto!!),
                ImageUploader.UploadImageTypes.PASSPORT_PHOTO
            ) {
                passportUploaded = true
                verificationData.passportPhoto = it
                saveData()
            }
        }
        if (verificationData.selfiePhoto.isNullOrEmpty()) {
            selfieUploaded = true
        } else {
            ImageUploader.uploadImage(
                PickPhotoFragment.Image(verificationData.selfiePhoto!!),
                ImageUploader.UploadImageTypes.SELFIE_PHOTO
            ) {
                selfieUploaded = true
                verificationData.selfiePhoto = it
                saveData()
            }
        }
        if (verificationData.divorcePhoto.isNullOrEmpty()) {
            divorcedUploaded = true
        } else {
            ImageUploader.uploadImage(
                PickPhotoFragment.Image(verificationData.divorcePhoto!!), "divorce"
            ) {
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
            onSuccess.invoke(it.toObjectSafe(VerificationData::class.java))
        }.addOnFailureListener {
            onSuccess.invoke(null)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun saveCache() {
        GlobalScope.launch (Dispatchers.Default){
            val editor = nomzodSharedPref.edit()
            editor.putString(NOMZOD_TAG, gson!!.toJson(nomzod))
            editor.apply()
        }
    }

    private fun getCached() {
        val json = nomzodSharedPref.getString(NOMZOD_TAG, null)
        if (json != null) {
            nomzod = gson!!.fromJson(json, Nomzod::class.java)
            nomzodLive.postVal(nomzod)
        }
    }

    val userId: String get() = LocalUser.user.uid
    private var listenerRegistration: ListenerRegistration? = null

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadMyNomzodServer() {
        if (userId.isEmpty()) return
        nomzodLoading.postVal(true)
        listenerRegistration = NomzodsController.nomzodlarReference.document(userId)
            .addSnapshotListener { value, error ->
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val result = value?.toObjectSafe(Nomzod::class.java)
                        nomzod = result ?: Nomzod()
                        saveCache()
                        MainScope().launch {
                            nomzodLive.postVal(nomzod)
                            nomzodLoading.postVal(false)
                        }
                        MyFilter.apply {
                            if (result != null) {
                                filter.yoshChegarasiDan = nomzod.yoshChegarasiDan
                                filter.yoshChegarasiGacha = nomzod.yoshChegarasiGacha
                                filter.nomzodType = if (nomzod.type == KELIN) KUYOV else KELIN
                                update()
                            }
                        }
                    } catch (e: Exception) {
                        handleException(e)
                    }
                }
            }
    }

    fun updateNomzod(
        nomzod: Nomzod, server: Boolean, onSuccess: (success: Boolean) -> Unit
    ) {
        this.nomzod = nomzod
        nomzodLive.postVal(nomzod)
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