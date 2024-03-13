package com.uz.sovchi.data.nomzod

import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.location.City
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

    fun uploadNewMyNomzod(nomzod: Nomzod) {
        if (nomzod.id.isEmpty()) return
        myNomzods.removeIf { it.id == nomzod.id }
        myNomzods.add(0, nomzod)
        nomzodlarReference.document(nomzod.id).set(nomzod)
    }

    fun clearMyNomzods() {
        myNomzods.clear()
        myNomzodsLoading.postValue(false)
    }

    suspend fun loadMyNomzods() = suspendCoroutine { suspend ->
        myNomzodsLoading.postValue(true)
        loadNomzods(-1, null, LocalUser.user.uid, "", "", 0) {
            myNomzodsLoading.postValue(false)
            myNomzods.addAll(it)
            suspend.resume(it)
        }
    }

    fun loadNomzods(
        type: Int,
        lastNomzod: Nomzod?,
        userId: String,
        manzil: String,
        oilaviyHolati: String,
        yoshChegarasi: Int = 0,
        imkonChek: Boolean = false,
        loaded: (list: List<Nomzod>) -> Unit
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
                task.startAfter(lastNomzod.tugilganYili, lastNomzod.id)
            }
        }
        if (type != -1) {
            task = task.whereEqualTo(Nomzod::type.name, type)
        }
        if (userId.isNotEmpty()) {
            task = task.whereEqualTo(Nomzod::userId.name, userId)
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

        task.get().addOnCompleteListener {
            val data = it.result.toObjects(Nomzod::class.java)
            loaded.invoke(data)
        }
    }

    fun deleteNomzod(id: String) {
        nomzodlarReference.document(id).delete()
        myNomzods.removeIf { it.id == id }
    }

}