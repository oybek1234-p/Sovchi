package com.uz.sovchi.ui.nomzod

import android.content.res.ColorStateList
import android.graphics.Color
import com.google.android.material.color.MaterialColors
import com.uz.sovchi.DateUtils
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.nomzod.KUYOV
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.OilaviyHolati
import com.uz.sovchi.data.nomzod.OqishMalumoti
import com.uz.sovchi.data.saved.SavedRepository
import com.uz.sovchi.data.viewed.ViewedNomzods
import com.uz.sovchi.databinding.NomzodItemBinding
import com.uz.sovchi.visibleOrGone

fun NomzodItemBinding.setNomzod(nomzod: Nomzod, forDetails: Boolean = false) {
    typeView.apply {
        with(nomzod) {
            if (forDetails.not()) {
                val viewed = ViewedNomzods.isViewed(id)
                val new = DateUtils.isToday(uploadDate) && !viewed
                container.setBackgroundColor(
                    if (new) MaterialColors.getColor(
                        typeView,
                        com.google.android.material.R.attr.colorSurfaceVariant
                    ) else
                        MaterialColors.getColor(
                            typeView,
                            com.google.android.material.R.attr.colorSurface
                        )
                )
                newBadge.visibleOrGone(new)
            }
//            if (nomzod.type == KUYOV) {
//                setText(R.string.kuyovlikga_nomzod)
//            } else {
//                setText(R.string.kelinlikga_nomzod)
//            }
            val getString: (id: Int) -> String = { it ->
                context.getString(it)
            }
            var parmText =
                (if (nomzod.type == KUYOV) "\uD83E\uDD35\u200D♂\uFE0F" else "\uD83D\uDC70\uD83C\uDFFB") + " ${
                    if (nomzod.type == KUYOV) getString(
                        R.string.kuyovlikga
                    ) else getString(R.string.kelinlikga)
                }"
            parmText += " 📊  ${tugilganYili}-yosh"
            if (buyi > 0) {
                parmText += "  $buyi-sm"
            }
            if (vazni > 0) {
                parmText += "  $vazni-kg"
            }
            paramsView.text = parmText
            millatiView.text = "${getString(R.string.millati)}: $millati"

            val oilaviyHolatiText = try {
                appContext.getString(OilaviyHolati.valueOf(oilaviyHolati).resourceId)
            } catch (e: Exception) {
                oilaviyHolati
            }
            if (oilaviyHolati == OilaviyHolati.AJRASHGAN.name || oilaviyHolati == OilaviyHolati.Beva.name) {
                farzandlarView.apply {
                    visibleOrGone(true)
                    if (farzandlar.isEmpty()) {
                        farzandlar = getString(R.string.yoq)
                    }
                    text = "${getString(R.string.farzandlar)}:  $farzandlar"
                }
            } else {
                farzandlarView.visibleOrGone(false)
            }
            oilaviyView.text =
                "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66  $oilaviyHolatiText"

            val oqishText = try {
                getString(
                    OqishMalumoti.valueOf(
                        oqishMalumoti
                    ).resId
                )
            } catch (e: Exception) {
                oqishMalumoti
            }
            oqishView.text =
                "${getString(R.string.o_qish_malumoti)}: $oqishText"

            ishView.text = "${getString(R.string.ish_joyi)}: $ishJoyi"

            manzilView.text = "\uD83D\uDCCD ${getString(City.valueOf(manzil).resId)}"
            tgjView.text = "${getString(R.string.tugilgan_joyi)}: $tugilganJoyi"
            talablarView.text = "${getString(R.string.talablar)}: $talablar"
            dateView.text = DateUtils.formatDate(uploadDate)
            imkonChekBadgeTextView.visibleOrGone(imkoniyatiCheklangan)

            if (forDetails) {
                ismiView.visibleOrGone(name.isNotEmpty())
                if (name.isNotEmpty()) {
                    ismiView.text = "${getString(R.string.ismi)}: $name"
                }
                likeButton.visibleOrGone(false)
                talablarView.maxLines = Int.MAX_VALUE
                yoshChegarasiView.text = "${getString(R.string.yosh_chegarasi)}: $yoshChegarasi"
                imkonChekInfo.visibleOrGone(imkoniyatiCheklangan)
                if (imkoniyatiCheklangan) {
                    imkonChekInfo.text =
                        "${getString(R.string.ma_lumot)}: $imkoniyatiCheklanganHaqida"
                }
            } else {
                val isLiked = SavedRepository.isNomzodLiked(id)
                likeButton.imageTintList =
                    ColorStateList.valueOf(
                        if (isLiked) MaterialColors.getColor(
                            likeButton,
                            androidx.appcompat.R.attr.colorPrimary
                        ) else Color.LTGRAY
                    )
                oqishView.visibleOrGone(false)
                yoshChegarasiView.visibleOrGone(false)
                ismiView.visibleOrGone(false)
                tgjView.visibleOrGone(false)
                millatiView.visibleOrGone(false)
                ishView.visibleOrGone(type == KUYOV)
            }
        }

    }
}