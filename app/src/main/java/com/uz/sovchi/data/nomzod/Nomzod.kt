package com.uz.sovchi.data.nomzod

import android.text.Html
import android.text.Spanned
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.gson.annotations.SerializedName
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
    const val INVISIBLE = 3
    const val CHECKING = 4
    const val REJECTED = 5
}

enum class NomzodTarif(val nameRes: Int, val priceSum: Int, val infoRes: Int) {
    STANDART(R.string.standartname, 0, R.string.oddiy_joylash), TOP_3(
        R.string.top3name, 14500, R.string._3_kun_listni_tepasida_turadi
    ),
    TOP_7(R.string.top7name, 22500, R.string._7_kun_listni_tepasida_turadi);

    fun getPrice(type: Int): Int {
        if (this == STANDART && type == KUYOV) {
            return 9800
        }
        return priceSum
    }
}

@Entity
data class Nomzod(
    @SerializedName("id", alternate = ["nid"]) @PrimaryKey var id: String = "",
    @SerializedName("userId", alternate = ["userUid"]) var userId: String = "",
    var name: String = "",
    var type: Int = -1,
    var state: Int = NomzodState.CHECKING,
    var tarif: String? = NomzodTarif.STANDART.name,
    var paymentCheckPhotoUrl: String? = "",
    var photos: List<String> = listOf(),
    var tugilganYili: Int = 0,
    var tugilganJoyi: String = "",
    var manzil: String = "",
    var buyi: Int = 0,
    var vazni: Int = 0,
    var farzandlar: String = "",
    var hasChild: Boolean? = null,
    var millati: String = "",
    var oilaviyHolati: String = "",
    var oqishMalumoti: String = "",
    var ishJoyi: String = "",
    var yoshChegarasiDan: Int = 0,
    var yoshChegarasiGacha: Int = 0,
    //Qo'shimcha
    var talablar: String = "",
    var showPhotos: Boolean = true,
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
    var visibleDate: Long = 0,
    @SerializedName("likedMe") @Exclude var likedMe: Boolean = false
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
    val text =
        "<span style='font-size:12sp;'>" + appContext.getString(R.string.tugilgan) + ":</span> " + "<span style='font-size:15sp;'>" + tugilganJoyi + "</span>"
    return Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)
}

fun Nomzod.getManzilText(): Spanned {
    return Html.fromHtml(
        "Manzil: ${
            appContext.getString(
                City.valueOf(
                    manzil
                ).resId
            )
        }"
    )
}

fun Nomzod.getStatusText(): String {
    return when (state) {
        NomzodState.CHECKING -> appContext.getString(R.string.tekshirilmoqda)
        NomzodState.INVISIBLE -> appContext.getString(R.string.off)
        NomzodState.NOT_PAID -> appContext.getString(R.string.unpaid)
        NomzodState.VISIBLE -> appContext.getString(R.string.aktiv)
        NomzodState.REJECTED -> appContext.getString(R.string.rejected)
        else -> ""
    }
}

fun Nomzod.getYoshChegarasi(): Spanned {
    var text = "${appContext.getString(R.string.yosh_chegarasi)}:"
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
    Aralash(R.string.aralash), AJRASHGAN(R.string.ajrashgan), Buydoq(R.string.bo_ydoq), Beva(R.string.beva), Oilali(
        R.string.oilali
    )
}

enum class OqishMalumoti(val resId: Int) {
    Oliy(R.string.oliy), OrtaMaxsus(R.string.orta_maxsus)
}
