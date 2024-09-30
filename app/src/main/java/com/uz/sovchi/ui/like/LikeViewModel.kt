package com.uz.sovchi.ui.like

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uz.sovchi.data.like.LikeController
import com.uz.sovchi.data.like.LikeModelFull
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.postVal
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LikeViewModel : ViewModel() {

    var type: Int = -1
    fun typeTab() = type
    var loading = MutableLiveData(false)

    var allList = LikeViewModel.allList
    var allListLive = LikeViewModel.allListLive

    companion object {
        var allList = arrayListOf<LikeModelFull>()
        var allListLive = MutableLiveData(allList)
    }

    var selectedTabPos = 0

    fun removeNomzod(nomzod: Nomzod) {
        allList.removeIf { it.likedUserNomzod?.id == nomzod.id }
        allListLive.postVal(allList)
    }

    private var job: Job? = null

    fun stopLoading() {
        job?.cancel()
        job = null
        allList.clear()
    }

    fun applyType(type: Int) {
        if (this.type == type) return
        this.type = type
        job?.cancel()
        job = null
        allList.clear()
        allListLive.postVal(allList)
        loading.postVal(false)
        loading.value = false
        loadNext()
    }


    private var newLoading = false
    var newAdded = false

    fun loadNew() {
        val end = allList.firstOrNull() ?: return
        newLoading = true
        loading.postVal(true)
        LikeController.loadLikesFull(viewModelScope, null, end, type) {
            newLoading = false
            loading.postVal(false)
            if (it.isNotEmpty()) {
                newAdded = true
                allList.addAll(0, it)
                allListLive.postVal(allList)
            }
        }
    }

    fun loadNext() {
        if (loading.value == true) return
        loading.postVal(true)
        job = viewModelScope.launch {
            val lastNomzod = allList.lastOrNull()
            LikeController.loadLikesFull(viewModelScope, lastNomzod, null, type) {
                loading.postVal(false)
                allList.addAll(it)
                allListLive.postVal(allList)
            }
        }
    }

}