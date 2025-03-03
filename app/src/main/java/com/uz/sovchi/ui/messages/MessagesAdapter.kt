package com.uz.sovchi.ui.messages

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.google.android.material.color.MaterialColors
import com.uz.sovchi.DateUtils
import com.uz.sovchi.R
import com.uz.sovchi.data.messages.MESSAGE_TYPE_NOMZOD_FOR_YOU
import com.uz.sovchi.data.messages.MESSAGE_TYPE_NOMZOD_LIKED
import com.uz.sovchi.data.messages.MESSAGE_TYPE_NOMZOD_REQUEST
import com.uz.sovchi.data.messages.MESSAGE_TYPE_PLATFORM
import com.uz.sovchi.data.messages.Message
import com.uz.sovchi.data.messages.NomzodForYouModel
import com.uz.sovchi.data.messages.NomzodLikedModel
import com.uz.sovchi.data.messages.PlatformMessage
import com.uz.sovchi.data.nomzod.NomzodRepository
import com.uz.sovchi.databinding.MessageItemBinding
import com.uz.sovchi.ui.base.BaseAdapter
import jp.wasabeef.glide.transformations.BlurTransformation

class MessagesAdapter(
    val fragment: MessagesFragment, val next: () -> Unit, val nomzodRepository: NomzodRepository
) : BaseAdapter<Message, MessageItemBinding>(R.layout.message_item,
    object : DiffUtil.ItemCallback<Message>() {
        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }

        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.date == newItem.date
        }
    }) {

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun bind(holder: ViewHolder<*>, model: Message, pos: Int) {
        holder.apply {
            if (pos == currentList.lastIndex - 1) {
                next.invoke()
            }
            binding.apply {
                if (this is MessageItemBinding) {
                    val type = model.type

                    when (type) {
                        MESSAGE_TYPE_PLATFORM -> {
                            if (model.data is PlatformMessage) {
                                val message = model.data as PlatformMessage
                                Glide.with(root).load(R.drawable.sovchi_logo).into(iconView)
                                titleView.text = message.title
                                subtitleView.text = message.message
                                showNomzod.isVisible = false
                                dateView.text = ""
                            }
                        }

                        MESSAGE_TYPE_NOMZOD_FOR_YOU -> {
                            if (model.data is NomzodForYouModel) {
                                val forYouModel = model.data as NomzodForYouModel
                                iconView.apply {
                                    if (forYouModel.photo.isNullOrEmpty()
                                            .not() && forYouModel.photo != "null"
                                    ) {
                                        if (forYouModel.showPhoto == false) {
                                            Glide.with(root).load(forYouModel.photo)
                                                .transform(BlurTransformation(80)).into(this)
                                        } else {
                                            Glide.with(root).load(forYouModel.photo).into(this)
                                        }
                                        scaleType = ImageView.ScaleType.CENTER_CROP
                                        imageTintList = null
                                    } else {
                                        setImageResource(R.drawable.smile_emoji)
                                        scaleType = ImageView.ScaleType.CENTER
                                        imageTintList = ColorStateList.valueOf(
                                            MaterialColors.getColor(
                                                root, androidx.appcompat.R.attr.colorPrimary
                                            )
                                        )
                                    }
                                }
                                titleView.text =
                                    forYouModel.nomzodName + " " + forYouModel.nomzodAge.toString()
                                subtitleView.text = iconView.context.getString(R.string.siz_uchun)

                                dateView.text = DateUtils.formatDate(model.date)
                                showNomzod.isVisible = forYouModel.nomzodId.isNullOrEmpty().not()
                                root.setOnClickListener {
                                    fragment.navigate(R.id.nomzodDetailsFragment, Bundle().apply {
                                        putString("nomzodId", forYouModel.nomzodId)
                                    })
                                }
                            }
                        }

                        MESSAGE_TYPE_NOMZOD_REQUEST -> {

                        }

                        MESSAGE_TYPE_NOMZOD_LIKED -> {
                            val likeModel = model.data as NomzodLikedModel
                            iconView.apply {
                                if (likeModel.photo.trim()
                                        .isNotEmpty() && likeModel.photo != "null"
                                ) {
                                    Glide.with(context).load(likeModel.photo).into(this)
                                    imageTintList = null
                                    scaleType = ImageView.ScaleType.CENTER_CROP
                                } else {
                                    setImageResource(R.drawable.like_ic)
                                    scaleType = ImageView.ScaleType.CENTER
                                    imageTintList =
                                        ColorStateList.valueOf(context.getColor(R.color.likeColor))
                                }
                            }
                            if (likeModel.liked) {
                                titleView.text = likeModel.likedUserName.trim().ifEmpty {
                                    "Foydalanuvchi"
                                } + " siz bilan tanishmoqchi"
                                subtitleView.text = "So'rovni qabul qiling va tanishing"
                            } else {
                                titleView.text = likeModel.likedUserName.trim().ifEmpty {
                                    "Foydalanuvchi"
                                } + " so'rovingizni rad etdi"
                                subtitleView.text = "Afsuski nomzodga yoqmadingiz"
                            }
                            dateView.text = DateUtils.formatDate(model.date)
                            showNomzod.isVisible = likeModel.hasNomzod
                            root.setOnClickListener {
                                NomzodRepository.loadNomzods(
                                    -1, null, likeModel.likedUserId, "", "", limit = 1
                                ) { list, count ->
                                    if (list.isEmpty().not()) {
                                        val item = list.first()
                                        fragment.navigate(
                                            R.id.nomzodDetailsFragment,
                                            Bundle().apply {
                                                putString("nomzodId", item.id)
                                            })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}