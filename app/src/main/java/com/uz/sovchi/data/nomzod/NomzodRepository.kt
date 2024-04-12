package com.uz.sovchi.data.nomzod

import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.uz.sovchi.data.ImageUploader
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.location.City
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.photo.PickPhotoFragment
import java.sql.Timestamp
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NomzodRepository {

    var myNomzods = arrayListOf<Nomzod>()

    val myNomzodsLoading = MutableLiveData(false)

    suspend fun getNomzodById(id: String, loadIfNotExists: Boolean): Nomzod? {
        val nomzod = myNomzods.find { it.id == id }
        if (nomzod != null) {
            return nomzod
        } else {
            if (loadIfNotExists) {
                return loadNomzod(id)
            }
        }
        return null
    }

    private var nomzodlarReference = FirebaseFirestore.getInstance().collection("nomzodlar")

    private suspend fun loadNomzod(id: String) = suspendCoroutine { sus ->
        nomzodlarReference.document(id).get().addOnCompleteListener {
            val nomzod = it.result.toObject(Nomzod::class.java)
            if (nomzod?.userId == LocalUser.user.uid) {
                myNomzods.add(nomzod)
            }
            sus.resume(nomzod)
        }
    }

    suspend fun uploadNewMyNomzod(nomzod: Nomzod, done: () -> Unit) {
        if (nomzod.id.isEmpty()) return
        val uploadNext = {
            myNomzods.removeIf { it.id == nomzod.id }
            myNomzods.add(0, nomzod)
            nomzodlarReference.document(nomzod.id).set(nomzod).addOnCompleteListener {
                done.invoke()
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

    fun clearMyNomzods() {
        myNomzods.clear()
        myNomzodsLoading.postValue(false)
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

    fun loadNomzods(
        type: Int,
        lastNomzod: Nomzod?,
        userId: String,
        manzil: String,
        oilaviyHolati: String,
        yoshChegarasi: Int = 0,
        imkonChek: Boolean = false,
        hasPhotoOnly: Boolean = false,
        loaded: (list: List<Nomzod>, count: Long) -> Unit
    ) {
        var task = nomzodlarReference.limit(12)
        task = if (yoshChegarasi == 0) {
            task.orderBy(Nomzod::id.name, Query.Direction.DESCENDING)
        } else {
            task.orderBy(Nomzod::tugilganYili.name)
                .orderBy(Nomzod::id.name, Query.Direction.DESCENDING)
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
        task.get().addOnCompleteListener { it ->
            val data = it.result.toObjects(Nomzod::class.java)
            loaded.invoke(data, 0)
        }
    }

    fun deleteNomzod(id: String) {
        nomzodlarReference.document(id).delete()
        myNomzods.removeIf { it.id == id }
    }

}