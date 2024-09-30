package com.uz.sovchi.data.viewed

import com.uz.sovchi.data.nomzod.AppRoomDatabase
import com.uz.sovchi.data.nomzod.DislikedNomzod
import com.uz.sovchi.data.nomzod.ViewedNomzod
import com.uz.sovchi.handleException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ViewedNomzods {

    private var nomzods = mutableSetOf<ViewedNomzod>()
    var disliked = mutableSetOf<DislikedNomzod>()

    fun isViewed(id: String) = nomzods.firstOrNull { it.id == id } != null

    private var init = false
    private var initing = false

    private val database: AppRoomDatabase by lazy {
        AppRoomDatabase.getInstance()
    }

    suspend fun init() = withContext(Dispatchers.IO) {
        if (init || initing) return@withContext
        initing = true
        database.viewedNomzodsDao().getAll().apply {
            nomzods.addAll(this)
        }
        database.dislikedNomzodsDao().getAll().apply {
            disliked.addAll(this)
        }
        initing = false
        init = true
    }

    fun setViewed(id: String) {
        if (isViewed(id)) return
        try {
            val nomzod = ViewedNomzod(id)
            AppRoomDatabase.getInstance().viewedNomzodsDao().delete(nomzod)
            AppRoomDatabase.getInstance().viewedNomzodsDao().setViewed(nomzod)
            nomzods.add(nomzod)
        } catch (e: Exception) {
            handleException(e)
        }
    }

}