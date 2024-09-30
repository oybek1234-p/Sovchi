package com.uz.sovchi.data.nomzod

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.uz.sovchi.data.ImageUploader
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.UserRepository
import com.uz.sovchi.data.like.LikeController
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.utils.FirebaseUtils.toObjectSafe
import com.uz.sovchi.data.utils.FirebaseUtils.toObjectsSafe
import com.uz.sovchi.data.verify.VerificationData
import com.uz.sovchi.handleException
import com.uz.sovchi.ui.photo.PickPhotoFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NomzodRepository {

    suspend fun getNomzodById(id: String): Pair<Nomzod?, LikeController.LikeInfo?> {
        return loadNomzod(id)
    }

    suspend fun uploadPremiumData(
        chekPath: String, done: (success: Boolean) -> Unit
    ) {
        if (LocalUser.user.hasNomzod) {
            var url = ""
            val upload = {
                if (url.isEmpty().not()) {
                    nomzodlarReference.document(LocalUser.user.uid).update(
                        Nomzod::state.name,
                        NomzodState.CHECKING,
                        Nomzod::paymentCheckPhotoUrl.name,
                        url
                    ).addOnSuccessListener {
                        done.invoke(true)
                    }.addOnFailureListener {
                        done.invoke(false)
                    }
                }
            }
            try {
                ImageUploader.uploadImage(
                    PickPhotoFragment.Image(chekPath),
                    ImageUploader.UploadImageTypes.PREMIUM_CHECK_PHOTO
                ) {
                    if (it.isNullOrEmpty().not()) {
                        url = it!!
                        upload.invoke()
                    } else {
                        done.invoke(false)
                    }
                }
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun uploadNewMyNomzod(
        nomzod: Nomzod, verificationData: VerificationData?, done: (success: Boolean) -> Unit
    ) {
        if (nomzod.id.isEmpty()) return
        val uploadNext = {
            val next = {
                MyNomzodController.updateNomzod(nomzod, true) {
                    done.invoke(it)
                }
            }
            if (verificationData != null) {
                GlobalScope.launch(Dispatchers.IO) {
                    MyNomzodController.uploadVerificationData(nomzod.id, verificationData) {
                        next.invoke()
                    }
                }
            } else {
                next.invoke()
            }
        }
        if (nomzod.photos.isNotEmpty()) {
            val list = arrayListOf<String?>()
            nomzod.photos.forEach { it ->
                ImageUploader.uploadImage(
                    PickPhotoFragment.Image(it), ImageUploader.UploadImageTypes.NOMZOD_PHOTO
                ) {
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

    companion object {

        var cacheNomzods: HashMap<String, Nomzod?> = hashMapOf()

        var myNomzods = arrayListOf<Nomzod>()
        var nomzodlarReference = FirebaseFirestore.getInstance().collection("nomzodlar")

        suspend fun loadNomzod(id: String) = suspendCoroutine { sus ->
            try {
                var likeLoaded = false
                var nomzodLoaded = false
                var likeInfo: LikeController.LikeInfo? = null
                var nomzod: Nomzod? = null
                val doIt = {
                    if (nomzodLoaded && likeLoaded) {
                        if (nomzod != null && likeInfo != null) {
                            if (nomzod!!.userId != LocalUser.user.uid) {
                                nomzod!!.likedMe = likeInfo!!.likedMe ?: false
                                myNomzods.add(nomzod!!)
                            }
                            cacheNomzods.put(nomzod!!.id, nomzod)
                            sus.resume(Pair(nomzod, likeInfo))
                        } else {
                            sus.resume(Pair(null, null))
                        }
                    }
                }
                LikeController.getLikeInfo(id) { info ->
                    likeInfo = info
                    likeLoaded = true
                    doIt.invoke()
                }
                nomzodlarReference.document(id).get().addOnSuccessListener {
                    nomzod = it.toObjectSafe(Nomzod::class.java)
                    nomzodLoaded = true
                    doIt.invoke()
                }.addOnFailureListener {
                    nomzodLoaded = true
                    doIt.invoke()
                }
            } catch (e: Exception) {
                handleException(e)
                sus.resume(Pair(null, null))
            }
        }

        fun deleteNomzod(id: String): Boolean {
            try {
                nomzodlarReference.document(id).delete()
            } catch (e: Exception) {
                handleException(e)
            }
            try {
                myNomzods.removeIf { it.id == id }
                if (id == LocalUser.user.uid) {
                    UserRepository.setHasNomzod(false)
                } else {
                    UserRepository.setHasNomzod(id, false)
                }
                sendPlatformMessage(id, PlatformMessageType.DELETED, "")
            } catch (e: Exception) {
                handleException(e)
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
            scope: CoroutineScope,
            type: Int,
            lastNomzod: Nomzod?,
            userId: String,
            manzil: String,
            oilaviyHolati: String,
            yoshChegarasiDan: Int = 0,
            yoshChegarasiGacha: Int = 0,
            onlyNew: Boolean = false,
            verify: Boolean = false,
            state: Int? = null,
            limit: Int = 12,
            loaded: (list: List<Nomzod>, count: Long) -> Unit
        ) {
            var task = nomzodlarReference.limit(limit.toLong())
                .orderBy(Nomzod::uploadDate.name, Query.Direction.DESCENDING)
            if (lastNomzod != null) {
                task = task.startAfter(lastNomzod.uploadDate)
            }
            if (verify) {
                task = task.whereEqualTo(Nomzod::state.name, state)
            } else {
                if (state != null) {
                    task = task.whereEqualTo(Nomzod::state.name, state)
                }
            }
            if (type != -1) {
                task = task.whereEqualTo(Nomzod::type.name, type)
            }
            if (onlyNew) {
                task = task.whereEqualTo(Nomzod::state.name, state)
            } else {
                if (userId.isNotEmpty()) {
                    task = task.whereEqualTo(Nomzod::userId.name, userId)
                }
                if (manzil.isNotEmpty() && manzil != City.Hammasi.name) {
                    task = task.whereEqualTo(Nomzod::manzil.name, manzil)
                }
                if (oilaviyHolati.isNotEmpty() && oilaviyHolati != OilaviyHolati.Aralash.name) {
                    task = task.whereEqualTo(Nomzod::oilaviyHolati.name, oilaviyHolati)
                }
                if (yoshChegarasiDan > 0 && yoshChegarasiGacha > 0) {
                    task =
                        task.whereLessThanOrEqualTo(Nomzod::tugilganYili.name, yoshChegarasiGacha)
                    task =
                        task.whereGreaterThanOrEqualTo(Nomzod::tugilganYili.name, yoshChegarasiDan)
                }
            }
            task.get().addOnSuccessListener {
                scope.launch(Dispatchers.Default) {
                    val data = it.toObjectsSafe(Nomzod::class.java)
                    data.forEach {
                        cacheNomzods[it.id] = it
                    }
                    launch (Dispatchers.Main){
                        loaded.invoke(data, 0)
                    }
                }
            }.addOnFailureListener {
                loaded.invoke(arrayListOf(), 0)
            }
        }
    }

    fun blockProfile(nomzod: Nomzod) {
        deleteNomzod(nomzod.id)
        FirebaseDatabase.getInstance().getReference("users").child(nomzod.userId).child("blocked")
            .setValue(true)
    }

    fun verify(nomzod: Nomzod, premium: Boolean? = null, verifyInfo: Boolean? = null) {
        if (nomzod.id.isEmpty()) return
        val updateMap = mutableMapOf<String, Any>().apply {
            put(
                Nomzod::state.name,
                NomzodState.VISIBLE,
            )
            put(
                Nomzod::visibleDate.name, System.currentTimeMillis()
            )
            if (verifyInfo != null) {
                put(Nomzod::verified.name, verifyInfo)
            }
            if (premium != null) {
                UserRepository.setPremium(nomzod.userId, premium)
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
}