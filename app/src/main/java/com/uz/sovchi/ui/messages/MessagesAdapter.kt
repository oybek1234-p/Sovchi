package com.uz.sovchi.ui.messages

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.color.MaterialColors
import com.uz.sovchi.DateUtils
import com.uz.sovchi.R
import com.uz.sovchi.data.messages.MESSAGE_TYPE_NOMZOD_FOR_YOU
import com.uz.sovchi.data.messages.MESSAGE_TYPE_NOMZOD_LIKED
import com.uz.sovchi.data.messages.Message
import com.uz.sovchi.data.messages.NomzodForYouModel
import com.uz.sovchi.data.messages.NomzodLikedModel
import com.uz.sovchi.data.nomzod.NomzodRepository
import com.uz.sovchi.databinding.MessageItemBinding
import com.uz.sovchi.ui.base.BaseAdapter

class MessagesAdapter(val fragment: MessagesFragment, val next: () -> Unit,val nomzodRepository: NomzodRepository) :
    BaseAdapter<Message, MessageItemBinding>(R.layout.message_item,
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
                        MESSAGE_TYPE_NOMZOD_FOR_YOU -> {
                            iconView.apply {
                                setImageResource(R.drawable.smile_emoji)
                                scaleType = ImageView.ScaleType.CENTER
                                imageTintList = ColorStateList.valueOf(
                                    MaterialColors.getColor(
                                        root,
                                        androidx.appcompat.R.attr.colorPrimary
                                    )
                                )
                            }
                            if (model.data is NomzodForYouModel) {
                                val forYouModel = model.data as NomzodForYouModel
                                titleView.text = forYouModel.title
                                subtitleView.text = forYouModel.body
                                dateView.text = DateUtils.formatDate(model.date)
                                showNomzod.isVisible = forYouModel.nomzodId.isNullOrEmpty().not()
                                root.setOnClickListener {
                                    fragment.navigate(R.id.nomzodDetailsFragment, Bundle().apply {
                                        putString("nomzodId", forYouModel.nomzodId)
                                    })
                                }
                            }
                        }

                        MESSAGE_TYPE_NOMZOD_LIKED -> {
                            iconView.apply {
                                setImageResource(R.drawable.like_ic)
                                scaleType = ImageView.ScaleType.CENTER
                                imageTintList =
                                    ColorStateList.valueOf(context.getColor(R.color.likeColor))
                            }
                            val likeModel = model.data as NomzodLikedModel
                            titleView.text = likeModel.likedUserName.trim().ifEmpty {
                                "Foydalanuvchi"
                            } + " sizga like bosdi!"
                            subtitleView.text = "Sizning nomzodingizni saqlashdi!"

                            dateView.text = DateUtils.formatDate(model.date)
                            showNomzod.isVisible = likeModel.hasNomzod
                            showNomzod.setOnClickListener {
                                nomzodRepository.loadNomzods(-1,null,likeModel.likedUserId,"","") { list,count->
                                    if (list.isEmpty().not()) {
                                        val item = list.first()
                                        fragment.navigate(R.id.nomzodDetailsFragment, Bundle().apply {
                                            putString("nomzodId", item.id)
                                        })
                                    }
                                }
                            }
                            root.setOnClickListener {
                                fragment.navigate(R.id.nomzodDetailsFragment, Bundle().apply {
                                    putString("nomzodId", likeModel.nomzodId)
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}