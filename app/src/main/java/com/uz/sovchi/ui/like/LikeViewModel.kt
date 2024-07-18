package com.uz.sovchi.ui.like

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.like.LikeController
import com.uz.sovchi.data.like.LikeModelFull
import com.uz.sovchi.data.like.LikeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
        if ((type == LikeState.LIKED_ME || type == LikeState.DISLIKED || type == LikeState.LIKED)
            && LocalUser.user.premium.not()) {
            return
        }

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
                    allListLive.postValue(allList)
                }
            }
        }
    }

}