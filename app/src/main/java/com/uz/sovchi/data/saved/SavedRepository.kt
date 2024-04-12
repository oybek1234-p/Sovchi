package com.uz.sovchi.data.saved

import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.recombee.RecombeeDatabase

object SavedRepository {

    private var savedReference = FirebaseFirestore.getInstance().collection("saved")

    var savedList = arrayListOf<SavedData>()

    var savedLoading = MutableLiveData(false)

    fun loadSaved(done: () -> Unit) {
        savedLoading.postValue(true)
        savedReference.whereEqualTo(SavedData::userId.name, LocalUser.user.uid).get()
            .addOnCompleteListener {
                val list = it.result.toObjects(SavedData::class.java)
                savedLoading.postValue(false)
                savedList.addAll(list)
                done.invoke()
            }
    }

    fun isNomzodLiked(nomzodId: String) = savedList.find { it.nomzod?.id == nomzodId } != null

    fun clear() {
        savedList.clear()
    }

    fun addToSaved(nomzod: Nomzod) {
        val userId = LocalUser.user.uid
        val nomzodId = nomzod.id
        val savedId = userId + nomzodId
        val saved = SavedData(savedId, userId, nomzod)
        RecombeeDatabase.setNomzodProfileViewed(userId, nomzodId)
        savedReference.document(savedId).set(saved)
        savedList.add(saved)
    }

    fun removeFromSaved(nomzodId: String) {
        val userId = LocalUser.user.uid
        val id = userId + nomzodId
        savedReference.document(id).delete()
        savedList.removeIf { it.nomzod?.id == nomzodId }
    }
}