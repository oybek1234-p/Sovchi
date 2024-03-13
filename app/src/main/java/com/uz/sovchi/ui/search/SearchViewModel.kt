package com.uz.sovchi.ui.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uz.sovchi.data.filter.MyFilter
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.NomzodRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    var nomzodlar = arrayListOf<Nomzod>()
    val nomzodlarLive = MutableLiveData(nomzodlar)
    val nomzodlarLoading = MutableLiveData(false)
    private val repo = NomzodRepository()
    private var lastNomzod: Nomzod? = null

    var nomzodTuri = MyFilter.filter.nomzodType
    var searchLocation = MyFilter.filter.manzil
    private var oilaviyHolati = MyFilter.filter.oilaviyHolati
    private var yoshChegarasi = MyFilter.filter.yoshChegarasi
    private var imkonChek = MyFilter.filter.imkonChek

    private var loadJob: Job? = null

    fun checkNeedRefresh() {
        MyFilter.filter.apply {
            val changed =
                (nomzodTuri != nomzodType || manzil != searchLocation || this@SearchViewModel.oilaviyHolati != oilaviyHolati ||
                        this@SearchViewModel.yoshChegarasi != yoshChegarasi || this@SearchViewModel.imkonChek != imkonChek)
            if (changed) {
                nomzodTuri = nomzodType
                searchLocation = manzil
                this@SearchViewModel.oilaviyHolati = oilaviyHolati
                this@SearchViewModel.yoshChegarasi = yoshChegarasi
                this@SearchViewModel.imkonChek = imkonChek
                refresh()
            }
        }

    }

    fun refresh() {
        lastNomzod = null
        nomzodlar.clear()
        nomzodlarLive.postValue(null)

        loadNextNomzodlar()
    }

    fun loadNextNomzodlar() {
        if (nomzodlarLoading.value == true) return
        loadJob?.cancel()
        nomzodlarLoading.postValue(true)
        lastNomzod = nomzodlar.lastOrNull()

        loadJob = viewModelScope.launch {
            repo.loadNomzods(
                lastNomzod = lastNomzod,
                type = nomzodTuri,
                manzil = searchLocation,
                oilaviyHolati = oilaviyHolati,
                yoshChegarasi = yoshChegarasi,
                userId = "",
                imkonChek = imkonChek
            ) {
                if (loadJob?.isCancelled == true) return@loadNomzods
                nomzodlar.addAll(it)
                nomzodlarLive.postValue(nomzodlar)
                nomzodlarLoading.postValue(false)
            }
        }
    }
}