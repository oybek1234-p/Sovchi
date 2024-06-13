package com.uz.sovchi.data.viewed

import com.uz.sovchi.data.nomzod.AppRoomDatabase
import com.uz.sovchi.data.nomzod.DislikedNomzod
import com.uz.sovchi.data.nomzod.ViewedNomzod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ViewedNomzods {

    private var nomzods = mutableSetOf<ViewedNomzod>()
    var disliked = mutableSetOf<DislikedNomzod>()

    fun isViewed(id: String) = nomzods.firstOrNull { it.id == id } != null

    fun isDisliked(id: String) = disliked.firstOrNull { it.id == id } != null

    private var init = false
    private var initing = false

    suspend fun init() = withContext(Dispatchers.IO) {
        if (init || initing) return@withContext
        initing = true
        AppRoomDatabase.getInstance().viewedNomzodsDao().getAll().apply {
            nomzods.addAll(this)
        }
        AppRoomDatabase.getInstance().dislikedNomzodsDao().getAll().apply {
            disliked.addAll(this)
        }
        initing = false
        init = true
    }

    fun setViewed(id: String) {
        if (isViewed(id)) return
        val nomzod = ViewedNomzod(id)
        AppRoomDatabase.getInstance().viewedNomzodsDao().delete(nomzod)
        AppRoomDatabase.getInstance().viewedNomzodsDao().setViewed(nomzod)
        nomzods.add(nomzod)
    }

    suspend fun removeDisliked(id: String){
        val nomzod = disliked.firstOrNull { it.id == id } ?: return
        AppRoomDatabase.getInstance().dislikedNomzodsDao().delete(nomzod)
        disliked.remove(nomzod)
    }

    fun setDisliked(id: String) {
        if (isDisliked(id)) return
        val nomzod = DislikedNomzod(id)
        AppRoomDatabase.getInstance().dislikedNomzodsDao().delete(nomzod)
        AppRoomDatabase.getInstance().dislikedNomzodsDao().setViewed(nomzod)
        disliked.add(nomzod)
    }
}