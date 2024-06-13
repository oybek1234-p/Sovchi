package com.uz.sovchi.ui.nomzod

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.uz.sovchi.DateUtils
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.nomzod.KUYOV
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.OilaviyHolati
import com.uz.sovchi.data.viewed.ViewedNomzods
import com.uz.sovchi.databinding.NomzodItemBinding
import com.uz.sovchi.showToast
import com.uz.sovchi.visibleOrGone
import jp.wasabeef.glide.transformations.BlurTransformation

fun Nomzod.needShowPhotos(): Boolean {
    if (LocalUser.user.uid == id) {
        showPhotos = true
    } else {
        if (likedMe) {
            showPhotos = true
        }
    }
    return showPhotos
}

fun NomzodItemBinding.setNomzod(
    nomzod: Nomzod,
    hasNomzod: Boolean,
    forDetails: Boolean = false,
    forceShowPhotos: Boolean = false
) {
    with(nomzod) {
        if (forDetails.not()) {
            val viewed = ViewedNomzods.isViewed(id)
            val new = DateUtils.isToday(uploadDate) && !viewed
            newBadge.visibleOrGone(new)
        }
        val getString: (id: Int) -> String = { it ->
            root.context.getString(it)
        }
        photoView.visibleOrGone(true)
        val photo = photos.firstOrNull()
        val needShowPhotos = needShowPhotos()
        if (photo != null) {
            if (needShowPhotos) {
                Glide.with(photoView).load(photo).into(photoView)
            } else {
                Glide.with(photoView).load(photo).transform(BlurTransformation(50)).into(photoView)
            }
        }

        var nameAgeText = "${
            name.trim().capitalize().ifEmpty {
                if (nomzod.type == KUYOV) getString(
                    R.string.kuyovlikga
                ) else getString(R.string.kelinlikga)
            }
        }"
        nameAgeText += "  $tugilganYili"
        nameAgeView.text = nameAgeText

        var farzandText = ""
        if (oilaviyHolati == OilaviyHolati.AJRASHGAN.name || oilaviyHolati == OilaviyHolati.Beva.name) {
            var childrenInfo = farzandlar
            if (hasChild == null) {
                if (farzandlar.trim().isEmpty()) {
                    childrenInfo = getString(R.string.yoq)
                }
            } else {
                childrenInfo = if (hasChild!!) {
                    getString(R.string.bor)
                } else {
                    getString(R.string.yoq)
                }
            }
            farzandText = "${getString(R.string.farzandlar)} ${childrenInfo.lowercase()}"
        } else {
            farzandText = ""
        }
        topBadge.isVisible = top
        val manzilText = getString(City.valueOf(manzil).resId)

        val oilaviyHolati = getString(OilaviyHolati.valueOf(this.oilaviyHolati).resourceId)

        oilaviyView.text = oilaviyHolati
        farzandView.text = farzandText
        farzandView.isVisible = farzandText.isNotEmpty()
        manzilView.text = manzilText
        likedYou.isVisible = nomzod.likedMe
    }

}
