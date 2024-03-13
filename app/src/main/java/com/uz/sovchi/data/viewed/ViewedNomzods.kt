package com.uz.sovchi.data.viewed

import com.uz.sovchi.data.nomzod.AppRoomDatabase
import com.uz.sovchi.data.nomzod.ViewedNomzod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ViewedNomzods {

    private var nomzods = mutableSetOf<ViewedNomzod>()

    fun isViewed(id: String) = nomzods.firstOrNull { it.id == id } != null

    suspend fun init() = withContext(Dispatchers.IO) {
        AppRoomDatabase.getInstance().viewedNomzodsDao().getAll().apply {
            nomzods.addAll(this)
        }
    }


    fun setViewed(id: String) {
        if (isViewed(id)) return
        val nomzod = ViewedNomzod(id)
        AppRoomDatabase.getInstance().viewedNomzodsDao().delete(nomzod)
        AppRoomDatabase.getInstance().viewedNomzodsDao().setViewed(nomzod)
        nomzods.add(nomzod)
    }
}