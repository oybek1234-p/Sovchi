package com.uz.sovchi.data.saved

import com.uz.sovchi.data.nomzod.Nomzod

data class SavedData(val id: String, val userId: String, val nomzod: Nomzod?,val userName: String,val hasNomzod: Boolean) {
    constructor() : this("", "", null,"",false)
}