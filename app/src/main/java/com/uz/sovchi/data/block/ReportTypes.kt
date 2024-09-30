package com.uz.sovchi.data.block

import androidx.annotation.Keep
import com.uz.sovchi.R
import com.uz.sovchi.appContext

@Keep
object ReportTypes {

    const val PUL_SURADI = 0
    const val QOPOL_MUOMALA = 1
    const val TOGRI_KELMADIK = 2
    const val HAQORAT = 3
    const val UYATSIZ = 4
    const val YOLGON_MALUMOT = 7
    const val YOLGONCHI = 5
    const val MAQSADI_OILA_QURISH_EMAS = 6

    fun getText(type: Int): String {
        return when (type) {
            PUL_SURADI -> appContext.getString(R.string.pul_so_radi)
            QOPOL_MUOMALA -> appContext.getString(R.string.muomalasi_qo_pol)
            TOGRI_KELMADIK -> appContext.getString(R.string.menga_to_gri_kelmadi)
            HAQORAT -> appContext.getString(R.string.haqorat_qildi)
            UYATSIZ -> appContext.getString(R.string.uyatsiz)
            YOLGON_MALUMOT -> appContext.getString(R.string.yolgon_ma_lumot)
            YOLGONCHI -> appContext.getString(R.string.yolg_onchi)
            MAQSADI_OILA_QURISH_EMAS -> appContext.getString(R.string.maqsadi_oila_qurish_emas)
            else -> ""
        }
    }
}