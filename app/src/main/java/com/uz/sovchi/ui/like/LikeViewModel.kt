package com.uz.sovchi.ui.like

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.like.LikeController
import com.uz.sovchi.data.like.LikeModelFull
import com.uz.sovchi.data.like.LikeState
import com.uz.sovchi.showToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LikeViewModel : ViewModel() {

    private var type: Int = -1
    fun typeTab() = type
    var loading = MutableLiveData(false)
    var allList = arrayListOf<LikeModelFull>()
    var allListLive = MutableLiveData(allList)
    var cachedList = hashMapOf<Int, List<LikeModelFull>>()

    var selectedTabPos = 0

    private var job: Job? = null

    fun setType(type: Int) {
        if (type == this.type) return
        this.type = type
        job?.cancel()
        job = null
        allList.clear()
        if (cachedList.get(type).isNullOrEmpty().not()) {
            cachedList.get(type)?.let {
                allList.addAll(it)
            }
        }
        allListLive.postValue(allList)
        loading.postValue(false)
        loading.value = false
        loadNext()
    }

    fun loadNext() {
        if (loading.value == true) return
        loading.postValue(true)
        job = viewModelScope.launch {
            delay(200)
            val currentType = type
            val lastNomzod = allList.lastOrNull()
            LikeController.loadLikesFull(lastNomzod, type) {
                if (currentType == type) {
                    loading.postValue(false)
                    allList.addAll(it)
                    cachedList[type] = allList.toMutableList()
                    allListLive.postValue(allList)
                }
            }
        }
    }

}