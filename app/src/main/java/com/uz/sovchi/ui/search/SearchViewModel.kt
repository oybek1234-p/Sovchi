package com.uz.sovchi.ui.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.filter.MyFilter
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.NomzodRepository
import com.uz.sovchi.data.nomzod.NomzodState
import com.uz.sovchi.data.nomzod.OilaviyHolati
import com.uz.sovchi.data.recombee.RecombeeDatabase
import com.uz.sovchi.data.viewed.ViewedNomzods
import com.uz.sovchi.showToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    companion object {
        const val FILTER_FOR_ME = 0
        const val FILTER_NEW = 1
        const val FILTER_PHOTO = 2
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

    var mainFilter = FILTER_PHOTO

    fun refresh() {
        loadJob?.cancel()
        recomId = ""
        lastNomzod = null
        nomzodlar.clear()
        nomzodlarLoading.value = false
        nomzodlarLive.postValue(null)

        loadNextNomzodlar()
    }

    private var recomId = ""
    var forVerify = false

    fun loadNextNomzodlar() {
        if (nomzodlarLoading.value == true) return
        loadJob?.cancel()
        nomzodlarLoading.postValue(true)
        nomzodlarLoading.value = true
        loadJob = viewModelScope.launch {
            if (!forVerify) {
                RecombeeDatabase.getRecommendForUser(
                    recomId,
                    type = nomzodTuri,
                    manzil = if (searchLocation == City.Hammasi.name) "" else searchLocation,
                    LocalUser.user.uid,
                    if (oilaviyHolati == OilaviyHolati.Aralash.name) "" else oilaviyHolati,
                    yoshChegarasiGacha,
                    yoshChegarasiDan,
                    3
                ) { recom, list ->
                    if (loadJob?.isCancelled == true) return@getRecommendForUser
                    recomId = recom
                    nomzodlarLoading.value = false
                    nomzodlarLoading.postValue(false)
                    nomzodlar.addAll(list)
                    nomzodlarLive.postValue(nomzodlar)
                }
            } else {
                lastNomzod = nomzodlar.lastOrNull()
                NomzodRepository.loadNomzods(
                    lastNomzod = lastNomzod,
                    type = if (forVerify) -1 else nomzodTuri,
                    manzil = if (forVerify) "" else searchLocation,
                    oilaviyHolati = if (forVerify) "" else oilaviyHolati,
                    yoshChegarasi = if (forVerify) 0 else yoshChegarasiGacha,
                    userId = "",
                    verify = forVerify,
                    hasPhotoOnly = false,
                    imkonChek = imkonChek,
                    state = NomzodState.VISIBLE
                ) { it, _ ->
                    if (loadJob?.isCancelled == true) return@loadNomzods
                    nomzodlar.addAll(it)
                    nomzodlarLive.postValue(nomzodlar)
                    nomzodlarLoading.postValue(false)
                }
            }
        }
    }
}