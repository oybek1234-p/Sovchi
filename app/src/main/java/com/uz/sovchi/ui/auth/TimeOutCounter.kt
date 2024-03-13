package com.uz.sovchi.ui.auth

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimeOutCounter {
    private var timeout = TIME_OUT_SEC
    var timeOutLive = MutableLiveData(timeout)
    private var countJob: Job? = null

    fun cancel() {
        countJob?.cancel()
        timeout = 0L
        timeOutLive.postValue(0)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun start() {
        timeout = TIME_OUT_SEC
        countJob = GlobalScope.launch(Dispatchers.Default) {
            for (i in TIME_OUT_SEC downTo 1) {
                if (isActive.not()) return@launch
                timeout--
                timeOutLive.postValue(timeout)
                delay(1000)
            }
        }
    }

    companion object {
        const val TIME_OUT_SEC = 60L
    }
}