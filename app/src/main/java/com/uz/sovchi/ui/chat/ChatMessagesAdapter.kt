package com.uz.sovchi.ui.chat

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.chat.ChatController
import com.uz.sovchi.data.chat.ChatMessageModel
import com.uz.sovchi.data.utils.DateUtils
import com.uz.sovchi.databinding.ChatDateHeaderBinding
import com.uz.sovchi.databinding.ChatMessageBinding
import com.uz.sovchi.loadPhoto
import com.uz.sovchi.openImageViewer
import com.uz.sovchi.ui.base.BaseAdapter
import com.uz.sovchi.ui.search.SearchAdapter
import okhttp3.internal.toLongOrDefault

class ChatMessagesAdapter : BaseAdapter<ChatMessageModel, ViewDataBinding>(
    R.layout.chat_message, diff
) {

    private var animatedItemIds = mutableSetOf("")
    var onChatClick: (message: ChatMessageModel) -> Unit = {}

    override fun getItemViewType(position: Int): Int {
        val model = getItem(position)

        if (model?.message == DATE_TYPE) {
            return R.layout.chat_date_header
        }
        return R.layout.chat_message
    }

    companion object {

        const val DATE_TYPE = "dateSovchi"

        val diff = object : DiffUtil.ItemCallback<ChatMessageModel>() {
            override fun areItemsTheSame(
                oldItem: ChatMessageModel, newItem: ChatMessageModel
            ): Boolean {
                return oldItem.id == newItem.id && oldItem.photo == newItem.photo
            }

            override fun areContentsTheSame(
                oldItem: ChatMessageModel, newItem: ChatMessageModel
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    var loadNext = {}

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.setRecycledViewPool(SearchAdapter.recyclerViewPool)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): ViewHolder<ViewDataBinding> {
        var binding: ViewDataBinding? = null
        binding = if (viewType == R.layout.chat_message) {
            ChatMessageBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        } else {
            ChatDateHeaderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        }
        val holder = ViewHolder(binding)
        binding.apply {

        }
        return holder
    }

    private fun animateItem(view: View, id: String) {
        if (animatedItemIds.contains(id).not()) {
            animatedItemIds.add(id)
            view.animate().cancel()
            view.apply {
                scaleX = 0.95f
                scaleY = 0.8f
                alpha = 0f
                view.animate().scaleX(1f).scaleY(1f).alpha(1f).withEndAction {
                    alpha = 1f
                    scaleY = 1f
                    scaleX = 1f
                }.setInterpolator(DecelerateInterpolator(2f)).setDuration(200).start()
            }
        }
    }

    override fun bind(holder: ViewHolder<*>, model: ChatMessageModel, pos: Int) {
        holder.apply {
            binding.apply {
                if (this is ChatMessageBinding) {
                    // dateView.text = DateUtils.getHourMinuteDayMonth(model.date.toLongOrNull() ?: 0L)
                    messageView.text = model.message
                    val isMe = model.senderId == LocalUser.user.uid
                    container.updateLayoutParams<FrameLayout.LayoutParams> {
                        gravity = if (isMe) Gravity.END else Gravity.START
                    }
                    photoContainer.updateLayoutParams<FrameLayout.LayoutParams> {
                        gravity = if (isMe) Gravity.END else Gravity.START
                    }
                    dateView.text = DateUtils.getDayHour(DateUtils.parseDateMillis(model.date ?: 0))
                    val color = MaterialColors.getColor(
                        root, if (isMe) com.google.android.material.R.attr.colorPrimary
                        else com.google.android.material.R.attr.colorSurfaceContainerLowest
                    )
                    val textColor = MaterialColors.getColor(
                        root, if (isMe) com.google.android.material.R.attr.colorOnPrimary
                        else com.google.android.material.R.attr.colorOnSurface
                    )
                    val loadingPhoto = ChatController.uploadingPhotosMessageIds.contains(model.id)
                    photoLoading.isVisible = loadingPhoto
                    messageView.setTextColor(textColor)
                    container.backgroundTintList = ColorStateList.valueOf(color)
                    dateView.setTextColor(
                        if (isMe) Color.WHITE else MaterialColors.getColor(
                            root, com.google.android.material.R.attr.colorOnSurfaceVariant
                        )
                    )
                    if (model.photo.isNotEmpty()) {
                        photoView.isVisible = true
                        photoView.loadPhoto(model.photo)
                        container.isVisible = false
                        photoView.updateLayoutParams<FrameLayout.LayoutParams> {
                            gravity = if (isMe) Gravity.END else Gravity.START
                        }
                        photoView.setOnClickListener {
                            photoView.openImageViewer(listOf(model.photo))
                        }
                        photoView.setOnLongClickListener {
                            if (loadingPhoto) return@setOnLongClickListener true
                            onChatClick.invoke(model)
                            return@setOnLongClickListener true
                        }
                    } else {
                        container.isVisible = true
                        photoView.isVisible = false
                    }
                    root.setOnClickListener {
                        if (loadingPhoto) return@setOnClickListener
                        onChatClick.invoke(model)
                    }
                }
                if (this is ChatDateHeaderBinding) {
                    dateView.text = DateUtils.getDateDay((model.date.toString().toLongOrDefault(0)))
                }
                if (pos >= itemCount - 2) {
                    loadNext.invoke()
                }
            }
        }
    }
}