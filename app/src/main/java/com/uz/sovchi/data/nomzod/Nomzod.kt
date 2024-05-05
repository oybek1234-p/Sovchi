package com.uz.sovchi.data.nomzod

import android.text.Html
import android.text.Spanned
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

enum class Talablar(val textId: Int) {
    IkkinchiRuzgorgaTaqiq(R.string.ruzgor_taqiq), OilaQurmagan(R.string.oila_qurmaganlar), OliyMalumotli(
        R.string.oliy_mal
    ),
    BuydoqlarTaqiq(R.string.bo_ydoqlar_yozmasin), FarzandiYoq(R.string.farzand_yoq), FaqatShaxarlik(
        R.string.faqat_shaxar
    ),
    FaqatViloyat(R.string.faqat_viloyat), AlohidaUyJoy(R.string.alohidauyjoy), Hijoblik(R.string.hijobda), QonuniyAjrashgan(
        R.string.qonuniy_ajrashgan
    ),
}

fun getNomzodTypeText(id: Int) = nomzodTypes.find { it.first == id }?.second

object NomzodState {
    const val VISIBLE = 1
    const val NOT_PAID = 2
    const val DELETED = 3
    const val CHECKING = 4
}

enum class NomzodTarif(val nameRes: Int, val priceSum: Int, val infoRes: Int) {
    STANDART(R.string.standartname, 0, 0), TOP_3(
        R.string.top3name, 29000, 0
    ),
    TOP_7(R.string.top7name, 39000, 0), PREMIUM(R.string.premium, 69000, 0)
}

@Entity
data class Nomzod(
    @PrimaryKey var id: String = "",
    var userId: String = "",
    var name: String = "",
    var type: Int = -1,
    var state: Int = NomzodState.CHECKING,
    var tarif: String = NomzodTarif.STANDART.name,
    var paymentCheckPhotoUrl: String = "",
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
    var uploadDate: Long = System.currentTimeMillis(),
    var uploadDateString: String = "",
    var views: Int = 0,
    var top: Boolean = false,
    var visibleDate: Long = 0
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

fun Nomzod.getTugilganJoyi(): Spanned {
    return Html.fromHtml(appContext.getString(R.string.tugilgan_joyi) + ": <b>${tugilganJoyi}<\b>")
}

fun Nomzod.getManzilText(): Spanned {
    return Html.fromHtml("Manzil" + ": <b>${appContext.getString(City.valueOf(manzil).resId)}<\b>")
}

fun Nomzod.getStatusText(): String {
    return when (state) {
        NomzodState.CHECKING -> "Tekshirilmoqda"
        NomzodState.DELETED -> "O'chirilgan"
        NomzodState.NOT_PAID -> "To'lanmagan"
        NomzodState.VISIBLE -> "Aktiv"
        else -> ""
    }
}

fun Nomzod.getYoshChegarasi(): Spanned {
    var text =
        "${if (type == KUYOV) appContext.getString(R.string.kelinlikga) else appContext.getString(R.string.kuyovlikga)} yosh chegarasi:"
    if (yoshChegarasiGacha > 0 || yoshChegarasiDan > 0) {
        text += "<b>"
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
        text += ""
    }
    text += "<\b>"
    return Html.fromHtml(text)
}

enum class OilaviyHolati(val resourceId: Int) {
    Aralash(R.string.aralash), AJRASHGAN(R.string.ajrashgan), Buydoq(R.string.bo_ydoq), Beva(R.string.beva)
}

enum class OqishMalumoti(val resId: Int) {
    Oliy(R.string.oliy), OrtaMaxsus(R.string.orta_maxsus)
}
