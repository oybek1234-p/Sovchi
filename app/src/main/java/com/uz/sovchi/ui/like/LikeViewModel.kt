package com.uz.sovchi.ui.like

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uz.sovchi.data.like.LikeController
import com.uz.sovchi.data.like.LikeModelFull
import com.uz.sovchi.showToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LikeViewModel : ViewModel() {

    private var type: Int = -1
    var loading = MutableLiveData(false)
    var allList = arrayListOf<LikeModelFull>()
    var allListLive = MutableLiveData(allList)

    private var job: Job? = null

    fun setType(type: Int) {
        this.type = type
        job?.cancel()
        job = null
        allList.clear()
        allListLive.postValue(allList)
        loading.postValue(false)
        loading.value = false
        loadNext()
    }

    fun loadNext() {
        if (loading.value == true) return
        loading.postValue(true)
        job = viewModelScope.launch {
            val lastNomzod = allList.lastOrNull()
            LikeController.loadLikesFull(lastNomzod, type) {
                loading.postValue(false)
                allList.addAll(it)
                allListLive.postValue(allList)
            }
        }
    }

}