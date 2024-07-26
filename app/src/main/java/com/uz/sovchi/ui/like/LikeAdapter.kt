package com.uz.sovchi.ui.like

import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.like.LikeModelFull
import com.uz.sovchi.data.like.LikeState
import com.uz.sovchi.data.nomzod.KUYOV
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.databinding.LikeItemBinding
import com.uz.sovchi.ui.base.BaseAdapter
import jp.wasabeef.glide.transformations.BlurTransformation

class LikeAdapter(val next: () -> Unit) : BaseAdapter<LikeModelFull, LikeItemBinding>(
    R.layout.like_item,
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
                model.photos.firstOrNull()?.let { photo ->
                    var showPhoto = model.showPhotos
                    if (models.matched || likedMe) {
                        showPhoto = true
                    }
                    if (showPhoto) {
                        Glide.with(root).load(photo).into(photoView)
                    } else {
                        Glide.with(root).load(photo).transform(BlurTransformation(80))
                            .into(photoView)
                    }
                }
                root.setOnClickListener {
                    onClick.invoke(model)
                }
                if (models.matched) {
                    chatButton.isVisible = true
                    chatButton.setOnClickListener {
                        onChatClick.invoke(model)
                    }
                    infoButton.isVisible = false
                } else {
                    chatButton.isVisible = false
                    infoButton.isVisible = true
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
                    try {
                        if (models.matched) {
                            subtitleView.text = "Suxbatlashing"
                            subtitleView.isVisible = true
                        } else {
                            if (likedMe) {
                                subtitleView.isVisible = true
                                subtitleView.text = "Siz bilan tanishmoqchi"
                            } else {
                                subtitleView.text = "Ko'proq malumot uchun bosing"
                            }
                        }
                    } catch (e: Exception) {
                        //
                    }

                }
            }
        }
    }
}