package com.uz.sovchi.ui.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uz.sovchi.data.filter.MyFilter
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.NomzodRepository
import com.uz.sovchi.data.nomzod.NomzodState
import com.uz.sovchi.postVal
import com.uz.sovchi.showToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    companion object {
        const val FILTER_FOR_ME = 0
        const val FILTER_NEW = 1
    }

    var nomzodlar = arrayListOf<Nomzod>()
    val nomzodlarLive = MutableLiveData(nomzodlar)
    val nomzodlarLoading = MutableLiveData(false)

    private var lastNomzod: Nomzod? = null

    var nomzodTuri = MyFilter.filter.nomzodType
    var searchLocation = MyFilter.filter.manzil
    private var oilaviyHolati = MyFilter.filter.oilaviyHolati
    var yoshChegarasiDan = MyFilter.filter.yoshChegarasiDan
    var yoshChegarasiGacha = MyFilter.filter.yoshChegarasiGacha

    private var imkonChek = MyFilter.filter.imkonChek

    private var loadJob: Job? = null

    fun checkNeedRefresh() {
        MyFilter.filter.apply {
            val changed =
                (nomzodTuri != nomzodType || manzil != searchLocation || this@SearchViewModel.oilaviyHolati != oilaviyHolati || this@SearchViewModel.yoshChegarasiDan != yoshChegarasiDan || this@SearchViewModel.yoshChegarasiGacha != yoshChegarasiGacha || this@SearchViewModel.imkonChek != imkonChek)
            if (changed) {
                nomzodTuri = nomzodType
                searchLocation = manzil
                this@SearchViewModel.oilaviyHolati = oilaviyHolati
                this@SearchViewModel.yoshChegarasiDan = yoshChegarasiDan
                this@SearchViewModel.yoshChegarasiGacha = yoshChegarasiGacha
                this@SearchViewModel.imkonChek = imkonChek
                refresh()
            }
        }

    }

    var filterType = FILTER_FOR_ME

    fun applyFilterType(type: Int) {
        if (filterType != type) {
            filterType = type
            refresh()
        }
    }

    fun refresh() {
        loadJob?.cancel()
        recomId = ""
        lastNomzod = null
        nomzodlar.clear()
        nomzodlarLoading.value = false
        nomzodlarLive.postVal(null)

        loadNextNomzodlar()
    }

    private var recomId = ""
    var forVerify = false

    private var currentLoadingId = 0L

    fun loadNextNomzodlar(state: Int = NomzodState.VISIBLE): Boolean {
        if (nomzodlarLoading.value == true) return false
        currentLoadingId = System.currentTimeMillis()
        val currLoadingId = currentLoadingId
        loadJob?.cancel()
        loadJob = null
        nomzodlarLoading.postVal(true)
        nomzodlarLoading.value = true

        loadJob = viewModelScope.launch {
            if (!forVerify) {
                lastNomzod = nomzodlar.lastOrNull()
                NomzodRepository.loadNomzods(
                    viewModelScope,
                    lastNomzod = lastNomzod,
                    type = nomzodTuri,
                    manzil = searchLocation,
                    oilaviyHolati = oilaviyHolati,
                    yoshChegarasiDan = yoshChegarasiDan,
                    yoshChegarasiGacha = yoshChegarasiGacha,
                    userId = "",
                    state = NomzodState.VISIBLE,
                    onlyNew = false
                ) { it, _ ->
                    if (currLoadingId != currentLoadingId) return@loadNomzods
                    nomzodlar.addAll(it)
                    nomzodlarLive.postVal(nomzodlar)
                    nomzodlarLoading.postVal(false)
                }
            } else {
                lastNomzod = nomzodlar.lastOrNull()
                NomzodRepository.loadNomzods(
                    viewModelScope,
                    lastNomzod = lastNomzod,
                    type = if (forVerify) -1 else nomzodTuri,
                    manzil = if (forVerify) "" else searchLocation,
                    oilaviyHolati = if (forVerify) "" else oilaviyHolati,
                    yoshChegarasiGacha = 0,
                    yoshChegarasiDan = 0,
                    userId = "",
                    verify = forVerify,
                    state = state
                ) { it, _ ->
                    if (currLoadingId != currentLoadingId) return@loadNomzods
                    nomzodlar.addAll(it)
                    nomzodlarLive.postVal(nomzodlar)
                    nomzodlarLoading.postVal(false)
                }
            }
        }
        return true
    }
}