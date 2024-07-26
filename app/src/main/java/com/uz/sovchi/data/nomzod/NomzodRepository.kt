package com.uz.sovchi.data.nomzod

import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.uz.sovchi.data.ImageUploader
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.UserRepository
import com.uz.sovchi.data.like.LikeController
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.verify.VerificationData
import com.uz.sovchi.ui.photo.PickPhotoFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NomzodRepository {

    val myNomzodsLoading = MutableLiveData(false)

    val myNomzods = NomzodRepository.myNomzods
    suspend fun getNomzodById(id: String, loadIfNotExists: Boolean): Nomzod? {
        return loadNomzod(id)
    }

    suspend fun uploadPremiumData(
        chekPath: String, done: (success: Boolean) -> Unit
    ) {
        if (LocalUser.user.hasNomzod) {
            var url = ""
            val upload = {
                if (url.isEmpty().not()) {
                    nomzodlarReference.document(MyNomzodController.nomzod.id).update(
                        Nomzod::state.name,
                        NomzodState.CHECKING,
                        Nomzod::paymentCheckPhotoUrl.name,
                        url
                    ).addOnCompleteListener {
                        done.invoke(it.isSuccessful)
                    }
                }
            }
            try {
                ImageUploader.uploadImage(PickPhotoFragment.Image(chekPath)) {
                    if (it.isNullOrEmpty().not()) {
                        url = it!!
                        upload.invoke()
                    } else {
                        done.invoke(false)
                    }
                }
            } catch (e: Exception) {
                //
            }
        }
    }

    suspend fun uploadNewMyNomzod(
        nomzod: Nomzod, verificationData: VerificationData, done: () -> Unit
    ) {
        if (nomzod.id.isEmpty()) return
        val uploadNext = {
            val next = {
                MyNomzodController.updateNomzod(nomzod, true) {
                    done.invoke()
                }
            }
            GlobalScope.launch(Dispatchers.IO) {
                MyNomzodController.uploadVerificationData(nomzod.id, verificationData) {
                    next.invoke()
                }
            }
        }
        if (nomzod.photos.isNotEmpty()) {
            val list = arrayListOf<String?>()
            nomzod.photos.forEach { it ->
                ImageUploader.uploadImage(PickPhotoFragment.Image(it)) {
                    list.add(it)
                    if (list.size == nomzod.photos.size) {
                        nomzod.photos = list.filterNotNull()
                        uploadNext.invoke()
                    }
                }
            }
        } else {
            uploadNext.invoke()
        }
    }

    fun increaseNomzodViews(nomzodId: String) {
        if (nomzodId.isEmpty()) return
        try {
            nomzodlarReference.document(nomzodId)
                .update(Nomzod::views.name, FieldValue.increment(1))
        } catch (e: Exception) {
            //
        }
    }

    suspend fun loadMyNomzods() = suspendCoroutine { suspend ->
        myNomzodsLoading.postValue(true)
        loadNomzods(-1, null, LocalUser.user.uid, "", "", 0) { it, count ->
            myNomzodsLoading.postValue(false)
            myNomzods.addAll(it)
            suspend.resume(it)
        }
    }

    private var checkedFit: Boolean = false

    private val voiceStorage: StorageReference by lazy {
        FirebaseStorage.getInstance().getReference("voices")
    }

    private fun getRandomVoiceId() = "sovchiVoice${System.nanoTime()}"

    private fun uploadVoice(uri: String, done: (downloadUrl: String) -> Unit) {
        voiceStorage.child(getRandomVoiceId()).putFile(File(uri).toUri())
            .addOnCompleteListener { it ->
                it.result.storage.downloadUrl.addOnCompleteListener {
                    done.invoke(it.result.toString())
                }
            }
    }

    suspend fun checkFitMyNomzod(nomzod: Nomzod, done: (fits: Boolean) -> Unit) {
        var myNomzod = myNomzods.firstOrNull()
        if (myNomzod == null && checkedFit) {
            done.invoke(false)
            return
        }
        if (myNomzods.isEmpty()) {
            loadMyNomzods()
        }
        myNomzod = myNomzods.firstOrNull()
        var fits = false
        myNomzod?.let {
            var ageFits = false
            if (it.tugilganYili >= nomzod.yoshChegarasiDan && (it.tugilganYili <= nomzod.yoshChegarasiGacha || nomzod.yoshChegarasiGacha == 0)) {
                ageFits = true
            }
            fits = ageFits
        }
        done.invoke(fits)
    }

    companion object {

        var cacheNomzods: HashMap<String, Nomzod?> = hashMapOf()

        var myNomzods = arrayListOf<Nomzod>()
        var nomzodlarReference = FirebaseFirestore.getInstance().collection("nomzodlar")

        suspend fun loadNomzod(id: String) = suspendCoroutine { sus ->
            try {
                nomzodlarReference.document(id).get().addOnSuccessListener {
                    val nomzod = it.toObject(Nomzod::class.java)
                    if (nomzod != null) {
                        LikeController.getLikeInfo(nomzod) { iLiked, likedMe, dislikedMe, matched ->
                            if (nomzod.userId != LocalUser.user.uid) {
                                nomzod.likedMe = likedMe ?: false
                                myNomzods.add(nomzod)
                            }
                            cacheNomzods.put(nomzod.id, nomzod)
                            sus.resume(nomzod)
                        }
                    } else {
                        sus.resume(nomzod)
                    }
                }.addOnFailureListener {
                    sus.resume(null)
                }
            } catch (e: Exception) {
                //
            }
        }

        fun deleteNomzod(id: String): Boolean {
            try {
                nomzodlarReference.document(id).delete()
                myNomzods.removeIf { it.id == id }
                sendPlatformMessage(id, PlatformMessageType.DELETED, "")
            } catch (e: Exception) {
                //
            }
            return myNomzods.isEmpty()
        }

        private var firebaseFunctions =
            FirebaseFunctions.getInstance().getHttpsCallable("sendPlatformMessage")

        private fun sendPlatformMessage(nomzodId: String, type: Int, message: String) {
            if (nomzodId.isEmpty()) return
            firebaseFunctions.call(
                mapOf(
                    "userId" to nomzodId, "message" to message, "type" to type
                )
            )
        }

        fun loadNomzods(
            type: Int,
            lastNomzod: Nomzod?,
            userId: String,
            manzil: String,
            oilaviyHolati: String,
            yoshChegarasi: Int = 0,
            imkonChek: Boolean = false,
            hasPhotoOnly: Boolean = false,
            verify: Boolean = false,
            state: Int? = null,
            limit: Int = 12,
            loaded: (list: List<Nomzod>, count: Long) -> Unit
        ) {
            var task = nomzodlarReference.limit(limit.toLong())
            task = if (yoshChegarasi == 0) {
                task.orderBy(Nomzod::id.name, Query.Direction.DESCENDING)
            } else {
                task.orderBy(Nomzod::tugilganYili.name)
                    .orderBy(Nomzod::id.name, Query.Direction.DESCENDING)
            }
            if (verify) {
                task = task.whereNotEqualTo(Nomzod::state.name, NomzodState.VISIBLE)
            } else {
                if (state != null) {
                    task = task.whereEqualTo(Nomzod::state.name, NomzodState.VISIBLE)
                }
            }
            if (lastNomzod != null) {
                task = if (yoshChegarasi == 0) {
                    task.startAfter(lastNomzod.id)
                } else {
                    task.startAfter(lastNomzod)
                }
            }
            if (type != -1) {
                task = task.whereEqualTo(Nomzod::type.name, type)
            }
            if (userId.isNotEmpty()) {
                task = task.whereEqualTo(Nomzod::userId.name, userId)
            }
            if (hasPhotoOnly) {
                task = task.whereNotEqualTo(Nomzod::photos.name, emptyList<Any>())
            }
            if (imkonChek) {
                task = task.whereEqualTo(Nomzod::imkoniyatiCheklangan.name, imkonChek)
            }
            if (manzil.isNotEmpty() && manzil != City.Hammasi.name) {
                task = task.whereEqualTo(Nomzod::manzil.name, manzil)
            }
            if (oilaviyHolati.isNotEmpty() && oilaviyHolati != OilaviyHolati.Aralash.name) {
                task = task.whereEqualTo(Nomzod::oilaviyHolati.name, oilaviyHolati)
            }
            if (yoshChegarasi > 0) {
                task = task.whereLessThanOrEqualTo(Nomzod::tugilganYili.name, yoshChegarasi)
            }
            try {
                task.get().addOnCompleteListener { it ->
                    val data = it.result.toObjects(Nomzod::class.java)
                    data.forEach {
                        cacheNomzods.put(it.id, it)
                    }
                    loaded.invoke(data, 0)
                }
            } catch (e: Exception) {
                //
            }
        }

    }

    fun verify(nomzod: Nomzod, premium: Boolean) {
        if (nomzod.id.isEmpty()) return
        val updateMap = mutableMapOf<String, Any>().apply {
            put(
                Nomzod::state.name,
                NomzodState.VISIBLE,
            )
            put(
                Nomzod::visibleDate.name, System.currentTimeMillis()
            )
            put(Nomzod::verified.name, true)
            put(
                Nomzod::uploadDateString.name,
                java.sql.Timestamp(System.currentTimeMillis()).toString()
            )
            if (premium) {
                UserRepository.setPremium(nomzod.userId, true)
            }
        }
        sendPlatformMessage(nomzod.id, PlatformMessageType.ACCEPTED_PROFILE, "")
        nomzodlarReference.document(nomzod.id).update(
            updateMap
        )
    }


    fun rejectNomzod(id: String, type: Int) {
        sendPlatformMessage(id, type, "")
        nomzodlarReference.document(id).update(Nomzod::state.name, NomzodState.REJECTED)
    }


    fun deleteTop(nomzodId: String) {
        if (nomzodId.isEmpty()) return
        nomzodlarReference.document(nomzodId)
            .update(Nomzod::top.name, false, Nomzod::tarif.name, NomzodTarif.STANDART.name)
    }

}