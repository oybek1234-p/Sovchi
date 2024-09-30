package com.uz.sovchi.ui.like

import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.like.LikeModelFull
import com.uz.sovchi.data.like.LikeState
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.nomzod.KUYOV
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.OilaviyHolati
import com.uz.sovchi.databinding.LikeItemBinding
import com.uz.sovchi.handleException
import com.uz.sovchi.loadPhoto
import com.uz.sovchi.ui.base.BaseAdapter

class LikeAdapter(val likeDislike: (like: Boolean, nomzod: Nomzod) -> Unit, val next: () -> Unit) :
    BaseAdapter<LikeModelFull, LikeItemBinding>(R.layout.like_item,
        object : DiffUtil.ItemCallback<LikeModelFull>() {
            override fun areContentsTheSame(
                oldItem: LikeModelFull, newItem: LikeModelFull
            ): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(oldItem: LikeModelFull, newItem: LikeModelFull): Boolean {
                return oldItem.id == newItem.id
            }
        }) {
    var onClick: (nomzod: Nomzod?) -> Unit = {}
    var onChatClick: (nomzod: Nomzod?) -> Unit = {}

    var type: Int = LikeState.LIKED_ME

    override fun bind(holder: ViewHolder<*>, models: LikeModelFull, pos: Int) {
        holder.apply {
            if (pos == currentList.size - 1) {
                next.invoke()
            }
            val model = if (type == LikeState.LIKED_ME) models.likedUserNomzod else models.nomzod
            if (model == null) return
            binding.apply {
                (this as LikeItemBinding)
                val likedMe =
                    models.userId != LocalUser.user.uid && models.likeState == LikeState.LIKED
                model.photos().firstOrNull()?.let { photo ->
                    var showPhoto = model.showPhotos
                    if (models.matched == true || likedMe) {
                        showPhoto = true
                    }
                    photoView.loadPhoto(photo, showPhoto.not())
                }
                try {
                    subtitleView.text =
                        root.context.getString(City.valueOf(model.manzil).resId) + ", " + root.context.getString(
                            OilaviyHolati.valueOf(
                                model.oilaviyHolati
                            ).resourceId
                        ).toLowerCase() + "\n" + model.talablar
                } catch (e: Exception) {
                    handleException(e)
                }
                root.setOnClickListener {
                    onClick.invoke(model)
                }
                (type == LikeState.LIKED_ME).let {
                    likeView.isVisible = it
                    dislikeView.isVisible = it
                }
                likeView.setOnClickListener {
                    likeDislike.invoke(true, model)
                }
                dislikeView.setOnClickListener {
                    likeDislike.invoke(false, model)
                }
                model.apply {
                    var nameAgeText = "${
                        name.trim().capitalize().ifEmpty {
                            if (type == KUYOV) appContext.getString(
                                R.string.kuyovlikga
                            ) else appContext.getString(R.string.kelinlikga)
                        }
                    }"
                    nameAgeText += "  $tugilganYili"
                    titleView.text = nameAgeText
                }
            }
        }
    }
}