package com.uz.sovchi.ui.chat

import android.content.res.ColorStateList
import android.view.Gravity
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.google.android.material.color.MaterialColors
import com.uz.sovchi.DateUtils
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.chat.ChatMessageModel
import com.uz.sovchi.databinding.ChatMessageBinding
import com.uz.sovchi.openImageViewer
import com.uz.sovchi.ui.base.BaseAdapter

class ChatMessagesAdapter : BaseAdapter<ChatMessageModel, ChatMessageBinding>(
    R.layout.chat_message, diff
) {
    companion object {
        val diff = object : DiffUtil.ItemCallback<ChatMessageModel>() {
            override fun areItemsTheSame(
                oldItem: ChatMessageModel, newItem: ChatMessageModel
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: ChatMessageModel, newItem: ChatMessageModel
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    var loadNext = {}

    override fun bind(holder: ViewHolder<*>, model: ChatMessageModel, pos: Int) {
        holder.apply {
            binding.apply {
                (this as ChatMessageBinding)
               // dateView.text = DateUtils.getHourMinuteDayMonth(model.date.toLongOrNull() ?: 0L)
                messageView.text = model.message
                val isMe = model.senderId == LocalUser.user.uid
                container.updateLayoutParams<FrameLayout.LayoutParams> {
                    gravity = if (isMe) Gravity.END else Gravity.START
                }
                if (pos == itemCount - 1) {
                    loadNext.invoke()
                }
                val color = MaterialColors.getColor(
                    root,
                    if (isMe) com.google.android.material.R.attr.colorPrimary
                    else com.google.android.material.R.attr.colorSurfaceContainerLowest
                )
                val textColor = MaterialColors.getColor(
                    root,
                    if (isMe) com.google.android.material.R.attr.colorOnPrimary
                    else com.google.android.material.R.attr.colorOnSurface
                )
                messageView.setTextColor(textColor)
                container.backgroundTintList = ColorStateList.valueOf(color)
                if (model.photo.isNotEmpty()) {
                    photoView.isVisible = true
                    Glide.with(photoView).load(model.photo).into(photoView)
                    container.isVisible = false
                    photoView.updateLayoutParams<FrameLayout.LayoutParams> {
                        gravity = if (isMe) Gravity.END else Gravity.START
                    }
                    photoView.setOnClickListener {
                        photoView.openImageViewer(listOf(model.photo))
                    }
                } else {
                    container.isVisible = true
                    photoView.isVisible = false
                }
            }
        }
    }
}