package com.uz.sovchi.data.filter

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.nomzod.KELIN
import com.uz.sovchi.data.nomzod.OilaviyHolati

@Entity
data class SavedFilterUser(
    @PrimaryKey var userId: String,
    @PrimaryKey var phoneNumber: String,
    var manzil: String,
    var nomzodType: Int,
    var oilaviyHolati: String,
    var yoshChegarasiDan: Int,
    var yoshChegarasiGacha: Int,
    var imkonChek: Boolean,
    var date: Long
) {
    constructor() : this(
        LocalUser.user.uid,
        "",
        City.Hammasi.name,
        KELIN,
        OilaviyHolati.Aralash.name,
        18,
        90,
        false,
        0
    )
}