package com.uz.sovchi.data.nomzod

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.uz.sovchi.R
import com.uz.sovchi.appContext

const val KELIN = 0
const val KUYOV = 1

val nomzodTypes = listOf(
    Pair(KELIN, appContext.getString(R.string.kelinlikga)),
    Pair(KUYOV, appContext.getString(R.string.kuyovlikga))
)

enum class Talablar(val textId: Int) {
    IkkinchiRuzgorgaTaqiq(R.string.ruzgor_taqiq),
    OilaQurmagan(R.string.oila_qurmaganlar),
    OliyMalumotli(R.string.oliy_mal),
    BuydoqlarTaqiq(R.string.bo_ydoqlar_yozmasin),
    FarzandiYoq(R.string.farzand_yoq),
    FaqatShaxarlik(R.string.faqat_shaxar),
    FaqatViloyat(R.string.faqat_viloyat),
    AlohidaUyJoy(R.string.alohidauyjoy),
    Hijoblik(R.string.hijobda),
    QonuniyAjrashgan(R.string.qonuniy_ajrashgan),
}

fun getNomzodTypeText(id: Int) = nomzodTypes.find { it.first == id }?.second

@Entity
data class Nomzod(
    @PrimaryKey var id: String = "",
    var userId: String = "",
    var name: String = "",
    var type: Int = -1,
    var photos: List<String> = listOf(),
    var tugilganYili: Int = 0,
    var tugilganJoyi: String = "",
    var manzil: String = "",
    var buyi: Int = 0,
    var vazni: Int = 0,
    var farzandlar: String = "",
    var millati: String = "",
    var oilaviyHolati: String = "",
    var oqishMalumoti: String = "",
    var ishJoyi: String = "",
    var yoshChegarasiDan: Int = 0,
    var yoshChegarasiGacha: Int = 0,
    //Qo'shimcha
    var talablar: String = "",
    var imkoniyatiCheklangan: Boolean = false,
    var imkoniyatiCheklanganHaqida: String = "",
    //Talablar
    val talablarList: List<String> = listOf(),
    var telegramLink: String = "",
    var joylaganOdam: String = "",
    var mobilRaqam: String = "",
    var uploadDate: Long = System.currentTimeMillis()
) {
    constructor() : this(id = "")
}

fun Nomzod.paramsText(): String {
    var parmText = ""
    if (buyi > 0) {
        parmText += "$buyi-sm"
    }
    if (vazni > 0) {
        parmText += "  $vazni-kg"
    }
    return parmText

}

fun Nomzod.getYoshChegarasi(): String {
    var text =
        "${if (type == KUYOV) appContext.getString(R.string.kelinlikga) else appContext.getString(R.string.kuyovlikga)} yosh chegarasi:"
    if (yoshChegarasiGacha > 0 || yoshChegarasiDan > 0) {
        if (yoshChegarasiDan > 0) {
            text += " $yoshChegarasiDan dan"
        }
        if (yoshChegarasiGacha > 0) {
            if (yoshChegarasiDan > 0) {
                text += " -"
            }
            text += " $yoshChegarasiGacha gacha"
        }
    } else {
        text += " ${appContext.getString(R.string.taqdir)}"
    }
    return text
}

enum class OilaviyHolati(val resourceId: Int) {
    Aralash(R.string.aralash), AJRASHGAN(R.string.ajrashgan), Buydoq(R.string.bo_ydoq), Beva(R.string.beva)
}

enum class OqishMalumoti(val resId: Int) {
    Oliy(R.string.oliy), OrtaMaxsus(R.string.orta_maxsus)
}
