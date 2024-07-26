package com.uz.sovchi.ui.chat

import android.view.View
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.uz.sovchi.DateUtils
import com.uz.sovchi.R
import com.uz.sovchi.data.chat.ChatModel
import com.uz.sovchi.databinding.ChatItemBinding
import com.uz.sovchi.ui.base.BaseAdapter

class ChatsAdapter : BaseAdapter<ChatModel, ChatItemBinding>(R.layout.chat_item, diff) {

    var click: (ChatModel) -> Unit = {}

    companion object {
        val diff = object : DiffUtil.ItemCallback<ChatModel>() {
            override fun areContentsTheSame(oldItem: ChatModel, newItem: ChatModel): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(oldItem: ChatModel, newItem: ChatModel): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }

    override fun bind(holder: ViewHolder<*>, model: ChatModel, pos: Int) {
        super.bind(holder, model, pos)
        holder.apply {
            binding.apply {
                (this as ChatItemBinding)
                Glide.with(root).load(model.userImage).into(iconView)
                titleView.text = model.userName
                subtitleView.text = model.lastMessage

                countView.apply {
                    if (model.unreadCount > 0) {
                        visibility = View.VISIBLE
                        text = model.unreadCount.toString()
                    } else {
                        visibility = View.GONE
                    }
                }
                dateView.text = DateUtils.getHourMinuteDayMonth(model.lastDate)
                root.setOnClickListener {
                    click.invoke(model)
                }
            }
        }
    }
}