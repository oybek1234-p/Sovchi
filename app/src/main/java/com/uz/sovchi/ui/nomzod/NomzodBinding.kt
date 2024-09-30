package com.uz.sovchi.ui.nomzod

import androidx.core.view.isVisible
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.nomzod.KUYOV
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.OilaviyHolati
import com.uz.sovchi.data.utils.DateUtils
import com.uz.sovchi.data.viewed.ViewedNomzods
import com.uz.sovchi.databinding.NomzodItemBinding
import com.uz.sovchi.databinding.NomzodItemNewBinding
import com.uz.sovchi.loadPhoto
import com.uz.sovchi.ui.photo.PhotoAdapter
import com.uz.sovchi.ui.photo.PickPhotoFragment
import com.uz.sovchi.visibleOrGone
import java.util.Locale

fun Nomzod.needShowPhotos(): Boolean {
    if (LocalUser.user.uid == id) {
        showPhotos = true
    }
    return showPhotos
}

fun NomzodItemBinding.setNomzod(
    nomzod: Nomzod
) {
    with(nomzod) {
        val viewed = ViewedNomzods.isViewed(id)
        val new = DateUtils.isToday(DateUtils.parseDateMillis(nomzod.uploadDate)) && !viewed
        newBadge.visibleOrGone(new)

        val getString: (id: Int) -> String = { it ->
            root.context.getString(it)
        }
        photoView.visibleOrGone(true)
        val photo = photos.firstOrNull()
        val needShowPhotos = needShowPhotos()
        photoView.loadPhoto(photo, needShowPhotos.not())

        var nameAgeText = name
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
        val manzilText = getString(City.valueOf(manzil).resId)

        val oilaviyHolati = getString(OilaviyHolati.valueOf(this.oilaviyHolati).resourceId)

        oilaviyView.text = oilaviyHolati
        farzandView.text = farzandText
        farzandView.isVisible = farzandText.isNotEmpty()
        manzilView.text = manzilText
    }

}

fun NomzodItemNewBinding.setNomzod(
    nomzod: Nomzod
) {
    with(nomzod) {
        val viewed = ViewedNomzods.isViewed(id)
        val new = DateUtils.isToday(DateUtils.parseDateMillis(uploadDate)) && !viewed
        newBadge.visibleOrGone(new)

        val getString: (id: Int) -> String = { it ->
            appContext.getString(it)
        }

        val photo = photos().firstOrNull()
        val hasPhoto = photo.isNullOrEmpty().not()
        photoView.visibleOrGone(true)
        verifiedBadge.isVisible = verified && hasPhoto
        val needShowPhotos = needShowPhotos()

        if (photo != null) {
            val photoAdapter: PhotoAdapter
            if (photoView.adapter != null) {
                photoAdapter = photoView.adapter as PhotoAdapter
            } else {
                photoAdapter = PhotoAdapter { _, _, _, _ ->
                    root.performClick()
                }
                photoView.adapter = photoAdapter
            }
            photoAdapter.deleteShown = false
            photoAdapter.matchParent = true

            photoAdapter.submitList(photos().map { PickPhotoFragment.Image(it) })
            photoAdapter.showPhotos = needShowPhotos
            if (photos.size > 1) {
                springDotsIndicator.attachTo(photoView)
                springDotsIndicator.isVisible = true
            } else {
                springDotsIndicator.isVisible = false
            }
        }
        var nameAgeText = buildString {
            append(name.trim()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
        }
        if (nameAgeText.isEmpty()) {
            nameAgeText = if (nomzod.type == KUYOV) getString(
                R.string.kuyovlikga
            ) else getString(R.string.kelinlikga)
        }
        nameAgeText += "  $tugilganYili"
        nameAgeView.text = nameAgeText

        val manzilText = try {
            getString(City.valueOf(manzil).resId) + ""
        } catch (e: Exception) {
            ""
        }

        val oilaviyHolati = try {
            getString(OilaviyHolati.valueOf(this.oilaviyHolati).resourceId)
        } catch (e: Exception) {
            ""
        }

        photoCountView.text = photos.size.toString()
        photoCountView.isVisible = photos.size > 1

        val subtitleText = oilaviyHolati
        oilaviyView.text = subtitleText
        cityView.text = manzilText

        if (talablar.isNotEmpty()) {
            aboutView.isVisible = true
            aboutView.text = talablar
            if (hasPhoto) {
                aboutView.maxLines = 4
            } else {
                aboutView.maxLines = 6
            }
        } else {
            aboutView.isVisible = false
        }
    }
}
