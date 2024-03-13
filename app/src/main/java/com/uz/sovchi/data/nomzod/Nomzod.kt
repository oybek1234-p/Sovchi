package com.uz.sovchi.data.nomzod

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.data.location.City

const val KELIN = 0
const val KUYOV = 1

val nomzodTypes = listOf(
    Pair(KELIN, appContext.getString(R.string.kelinlikga)),
    Pair(KUYOV, appContext.getString(R.string.kuyovlikga))
)

fun getNomzodTypeText(id: Int) = nomzodTypes.find { it.first == id }?.second

@Entity
data class Nomzod(
    @PrimaryKey var id: String,
    var userId: String,
    var name: String,
    var type: Int,
    var tugilganYili: Int,
    var tugilganJoyi: String,
    var manzil: String = City.Toshkent.name,
    var buyi: Int,
    var vazni: Int,
    var farzandlar: String,
    var millati: String,
    var oilaviyHolati: String,
    var oqishMalumoti: String,
    var ishJoyi: String,
    var yoshChegarasi: String,
    var talablar: String,
    var imkoniyatiCheklangan: Boolean,
    var imkoniyatiCheklanganHaqida: String,
    var ikkinchisiga: Boolean,
    var telegramLink: String,
    var joylaganOdam: String,
    var mobilRaqam: String,
    var uploadDate: Long = System.currentTimeMillis()
) {
    constructor() : this(
        id = "",
        userId = "",
        name = "",
        -1,
        0,
        "",
        "",
        0,
        0,
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        imkoniyatiCheklangan = false,
        imkoniyatiCheklanganHaqida = "",
        ikkinchisiga = false,
        "",
        "",
        ""
    )
}

enum class OilaviyHolati(val resourceId: Int) {
    Aralash(R.string.aralash), AJRASHGAN(R.string.ajrashgan), Buydoq(R.string.bo_ydoq), Beva(R.string.beva)
}

enum class OqishMalumoti(val resId: Int) {
    Oliy(R.string.oliy), OrtaMaxsus(R.string.orta_maxsus)
}