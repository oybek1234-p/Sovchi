package com.uz.sovchi.data.nomzod

import android.text.Html
import android.text.Spanned
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.gson.annotations.SerializedName
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.valid

const val KELIN = 0
const val KUYOV = 1

val nomzodTypes = listOf(
    Pair(KELIN, appContext.getString(R.string.kelinlikga)),
    Pair(KUYOV, appContext.getString(R.string.kuyovlikga))
)

@Keep
enum class Talablar(val textId: Int) {
    IkkinchiRuzgorgaTaqiq(R.string.ruzgor_taqiq), OilaQurmagan(R.string.oila_qurmaganlar), OliyMalumotli(
        R.string.oliy_mal
    ),
    BuydoqlarTaqiq(R.string.bo_ydoqlar_yozmasin), FarzandiYoq(R.string.farzand_yoq), FaqatShaxarlik(
        R.string.faqat_shaxar
    ),
    FaqatViloyat(R.string.faqat_viloyat), AlohidaUyJoy(R.string.alohidauyjoy), Hijoblik(R.string.hijobda), QonuniyAjrashgan(
        R.string.qonuniy_ajrashgan
    )
}

fun getNomzodTypeText(id: Int) = nomzodTypes.find { it.first == id }?.second

@Keep
object NomzodState {
    const val VISIBLE = 1
    const val NOT_PAID = 2
    const val INVISIBLE = 3
    const val CHECKING = 4
    const val REJECTED = 5
}

@Keep
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

@Keep
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
    //Yashash
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
    //Talablar
    val talablarList: List<String> = listOf(),
    var joylaganOdam: String = "",
    var uploadDate: Any = System.currentTimeMillis(),
    var top: Boolean = false,
    var visibleDate: Any?,
    @SerializedName("likedMe") @Exclude var likedMe: Boolean = false,
    var acceptedCities: List<String> = listOf(),
    var verified: Boolean = false,
    var rejectType: Int = -1
) {
    constructor() : this(id = "", visibleDate = System.currentTimeMillis())

    companion object {
        const val KELIN_TEXT = "kelin"
        const val KUYOV_TEXT = "kuyov"
    }

    fun photos(): List<String> {
        if (photos.isEmpty()) {
            return if (type == KELIN) {
                listOf(KELIN_TEXT)
            } else {
                listOf(KUYOV_TEXT)
            }
        }
        return photos
    }

    fun isMatchToMe(): Boolean {
        val nomzod = MyNomzodController.nomzod
        var ageMatch = false
        val genderMatch = nomzod.type != type
        if (nomzod.tugilganYili in yoshChegarasiDan - 10..yoshChegarasiGacha + 10) {
            ageMatch = true
        }
        val locationMatch: Boolean = if (acceptedCities.isEmpty()) {
            true
        } else {
            acceptedCities.contains(nomzod.manzil)
        }
        if (LocalUser.user.hasNomzod.not() || LocalUser.user.valid.not() || MyNomzodController.nomzod.id.isEmpty()) {
            return true
        }
        return ageMatch && locationMatch && genderMatch
    }
}

fun Nomzod.showNeedVerifyInfo(): Boolean {
    return verified.not() && LocalUser.user.hasNomzod && MyNomzodController.nomzod.state != NomzodState.CHECKING
}

fun Nomzod.isVisible() = state == NomzodState.VISIBLE

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
    var text = "${appContext.getString(R.string.yosh_chegarasi)}  "
    if (yoshChegarasiGacha > 0 || yoshChegarasiDan > 0) {
        text += "<b>  "
        if (yoshChegarasiDan > 0) {
            text += "   $yoshChegarasiDan dan"
        }
        if (yoshChegarasiGacha > 0) {
            if (yoshChegarasiDan > 0) {
                text += "   -"
            }
            text += "   $yoshChegarasiGacha gacha"
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
