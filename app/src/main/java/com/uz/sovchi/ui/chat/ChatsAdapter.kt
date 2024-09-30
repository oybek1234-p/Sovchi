package com.uz.sovchi.ui.chat

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.data.chat.ChatModel
import com.uz.sovchi.data.nomzod.KELIN
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.utils.DateUtils
import com.uz.sovchi.databinding.ChatItemBinding
import com.uz.sovchi.loadPhoto
import com.uz.sovchi.ui.base.BaseAdapter
import com.uz.sovchi.ui.search.SearchAdapter

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

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.setRecycledViewPool(SearchAdapter.recyclerViewPool)
    }

    override fun bind(holder: ViewHolder<*>, model: ChatModel, pos: Int) {
        super.bind(holder, model, pos)
        holder.apply {
            binding.apply {
                (this as ChatItemBinding)
                iconView.loadPhoto(model.userImage.ifEmpty {
                    if (MyNomzodController.nomzod.type == KELIN) Nomzod.KUYOV_TEXT else Nomzod.KELIN_TEXT
                })
                titleView.text = model.userName.ifEmpty { appContext.getString(R.string.o_chirilgan) }
                subtitleView.text = model.lastMessage
                onlineIc.isVisible =
                    (ChatsViewModel.lastSeenTimesObserves[model.userId] ?: -1) == 0L
                countView.apply {
                    if (model.unreadCount > 0) {
                        visibility = View.VISIBLE
                        text = model.unreadCount.toString()
                    } else {
                        visibility = View.GONE
                    }
                }
                dateView.text = DateUtils.formatDate(model.lastDate)
                root.setOnClickListener {
                    click.invoke(model)
                }
            }
        }
    }
}