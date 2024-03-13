package com.uz.sovchi.ui.nomzod

import androidx.lifecycle.ViewModel
import com.uz.sovchi.data.nomzod.NomzodRepository

class NomzodViewModel : ViewModel() {

    val repository = NomzodRepository()
}