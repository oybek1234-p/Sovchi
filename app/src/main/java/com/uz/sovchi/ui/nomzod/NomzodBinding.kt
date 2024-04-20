package com.uz.sovchi.ui.nomzod

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.view.isVisible
import coil.load
import com.google.android.material.color.MaterialColors
import com.uz.sovchi.DateUtils
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.nomzod.KUYOV
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.OilaviyHolati
import com.uz.sovchi.data.nomzod.OqishMalumoti
import com.uz.sovchi.data.nomzod.Talablar
import com.uz.sovchi.data.nomzod.getYoshChegarasi
import com.uz.sovchi.data.nomzod.paramsText
import com.uz.sovchi.data.saved.SavedRepository
import com.uz.sovchi.data.viewed.ViewedNomzods
import com.uz.sovchi.databinding.NomzodItemBinding
import com.uz.sovchi.visibleOrGone

fun NomzodItemBinding.setNomzod(nomzod: Nomzod, hasNomzod: Boolean, forDetails: Boolean = false) {
    typeView.apply {
        with(nomzod) {
            if (forDetails.not()) {
                val viewed = ViewedNomzods.isViewed(id)
                val new = DateUtils.isToday(uploadDate) && !viewed
//                container.setBackgroundColor(
//                    if (new) MaterialColors.getColor(
//                        typeView, com.google.android.material.R.attr.colorSurfaceVariant
//                    ) else MaterialColors.getColor(
//                        typeView, com.google.android.material.R.attr.colorSurface
//                    )
//                )
                newBadge.visibleOrGone(new)
            }
            val getString: (id: Int) -> String = { it ->
                context.getString(it)
            }
            val photoShown = photos.isNotEmpty()
            photoView.visibleOrGone(photoShown)
            if (photoShown) {
                photos.first().let {
                    photoView.load(it)
                }
            }
            var nameAgeText = "${name.trim().capitalize().ifEmpty {
                if (nomzod.type == KUYOV) getString(
                    R.string.kuyovlikga
                ) else getString(R.string.kelinlikga)
            }}"
            nameAgeText += "  ${tugilganYili}-yosh"
            nameAgeView.text = nameAgeText
//            nameAgeView.setCompoundDrawablesWithIntrinsicBounds(
//                container.context.getDrawable(if (type == KUYOV) R.drawable.man_ic else R.drawable.woman_ic),
//                null,
//                null,
//                null
//            )
            paramsView.text = paramsText()
            millatiView.text = "${getString(R.string.millati)}: $millati"

          //  rasmiBorView.isVisible = hasNomzod.not() && photos.isNotEmpty()

            val oilaviyHolatiText = try {
                appContext.getString(OilaviyHolati.valueOf(oilaviyHolati).resourceId)
            } catch (e: Exception) {
                oilaviyHolati
            }
            if (oilaviyHolati == OilaviyHolati.AJRASHGAN.name || oilaviyHolati == OilaviyHolati.Beva.name) {
                farzandlarView.apply {
                    visibleOrGone(true)
                    if (farzandlar.trim().isEmpty()) {
                        farzandlar = getString(R.string.yoq)
                    }
                    text = "${getString(R.string.farzandlar)} ${farzandlar.lowercase()}"
                }
            } else {
                farzandlarView.visibleOrGone(false)
            }
            oilaviyView.text = "$oilaviyHolatiText"

            val oqishText = try {
                getString(
                    OqishMalumoti.valueOf(
                        oqishMalumoti
                    ).resId
                )
            } catch (e: Exception) {
                oqishMalumoti
            }
            oqishView.text = "$oqishText"

            ishView.text = "${ishJoyi.trim().capitalize()}"

            manzilView.text = "${getString(City.valueOf(manzil).resId)}"
            qoshimchaView.text = "${talablar.trim().capitalize()}"
            dateView.text = DateUtils.formatDate(uploadDate)
            imkonChekBadgeTextView.visibleOrGone(imkoniyatiCheklangan)
            ishView.visibleOrGone(type == KUYOV)

            if (yoshChegarasiDan == 0 && yoshChegarasiGacha == 0) {
                yoshChegarasiView.visibleOrGone(false)
            } else {
                yoshChegarasiView.text =
                    getYoshChegarasi()
            }

            imkonChekInfo.visibleOrGone(imkoniyatiCheklangan)
            if (imkoniyatiCheklangan) {
                imkonChekInfo.text = "${getString(R.string.ma_lumot)}: $imkoniyatiCheklanganHaqida"
            }
            val isLiked = SavedRepository.isNomzodLiked(id)
            likeButton.imageTintList = ColorStateList.valueOf(
                if (isLiked) MaterialColors.getColor(
                    likeButton, androidx.appcompat.R.attr.colorPrimary
                ) else Color.LTGRAY
            )

            //Talablar
            if (talablarList.isNotEmpty()) {
                try {
                    talablarView.visibleOrGone(true)
                    val list = talablarList.map { Talablar.valueOf(it) }
                    var adapterTalablar: TalablarAdapter? = null
                    if (talablarView.adapter is TalablarAdapter) {
                        adapterTalablar = talablarView.adapter as TalablarAdapter
                    }
                    if (adapterTalablar == null) {
                        adapterTalablar = TalablarAdapter()
                    }
                    adapterTalablar.showCheckBox = false
                    adapterTalablar.submitList(null)
                    adapterTalablar.submitList(list)
                    talablarView.adapter = adapterTalablar
                } catch (e: Exception) {
                    //Ignore
                }
            } else {
                talablarView.visibleOrGone(false)
            }
        }

    }
}