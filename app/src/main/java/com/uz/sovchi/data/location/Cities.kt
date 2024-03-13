package com.uz.sovchi.data.location

import com.uz.sovchi.R
import com.uz.sovchi.appContext

enum class City(val resId: Int) {
    Hammasi(R.string.hammasi),
    Toshkent(R.string.toshkent),
    Samarqand(R.string.samarqand),
    Nukus(R.string.nukus),
    Qashqadaryo(R.string.qashqa),
    Sirdaryo(R.string.sirdaryo),
    Surxondaryo(R.string.surxon),
    Termiz(R.string.termiz),
    Buxoro(R.string.buxoro),
    Andijon(R.string.andi),
    Navoiy(R.string.navoiy),
    Jizax(R.string.jizzah),
    Namangan(R.string.naman),
    Fargona(R.string.farg_ona),
    Xorazm(R.string.xorazm);

    companion object {
        fun asListNames() = City.entries.map { appContext.getString(it.resId) }
    }
}