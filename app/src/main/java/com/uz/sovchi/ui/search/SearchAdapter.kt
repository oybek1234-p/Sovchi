package com.uz.sovchi.ui.search

import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.color.MaterialColors
import com.uz.sovchi.R
import com.uz.sovchi.UserViewModel
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.like.LikeController
import com.uz.sovchi.data.like.LikeState
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.valid
import com.uz.sovchi.databinding.NomzodItemBinding
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseAdapter
import com.uz.sovchi.ui.nomzod.setNomzod

val NOMZOD_DIFF_UTIL = object : DiffUtil.ItemCallback<Nomzod>() {
    override fun areContentsTheSame(oldItem: Nomzod, newItem: Nomzod): Boolean {
        return oldItem == newItem
    }

    override fun areItemsTheSame(oldItem: Nomzod, newItem: Nomzod): Boolean {
        return oldItem.id == newItem.id
    }
}

class SearchAdapter(
    val userViewModel: UserViewModel,
    val next: () -> Unit,
    private val onClick: (nomzod: Nomzod) -> Unit,
    private val onLiked: ((liked: Boolean, nomzodId: String) -> Unit)? = null,
    private val isBackGray: Boolean = false,
    private val disliked: (id: String, position: Int) -> Unit = { i, p -> },
    private val onChatClick: (nomzod: Nomzod) -> Unit
) : BaseAdapter<Nomzod, NomzodItemBinding>(R.layout.nomzod_item, NOMZOD_DIFF_UTIL) {

    override fun onViewCreated(holder: ViewHolder<NomzodItemBinding>, viewType: Int) {
        super.onViewCreated(holder, viewType)
        holder.binding.apply {

            cardView.setOnClickListener {
                try {
                    onClick.invoke(currentList[holder.adapterPosition])
                } catch (e: Exception) {
                    //
                }
            }
            likeButton.setOnClickListener {
                val nomzod = currentList[holder.adapterPosition]
                onLiked?.invoke(true, nomzod.id)
            }
            dislikeButton.setOnClickListener {
                try {
                    val nomzod = currentList[holder.adapterPosition]
                    disliked.invoke(nomzod.id, holder.adapterPosition)
                } catch (e: Exception) {
                    //
                }
            }
            chatButton.setOnClickListener {
                val nomzod = currentList[holder.adapterPosition]
                onChatClick.invoke(nomzod)
            }
        }
    }

    companion object {
        fun likeOrDislike(nomzod: Nomzod, like: Boolean): Boolean {
            if (LocalUser.user.valid.not()) {
                showToast("Akkauntga kiring!")
                return false
            }
            if (like.not()) {
                LikeController.likeOrDislikeNomzod(
                    LocalUser.user.uid, nomzod, LikeState.DISLIKED
                )
            } else {
                LikeController.likeOrDislikeNomzod(
                    LocalUser.user.uid, nomzod, LikeState.LIKED
                )
            }
            return true
        }
    }

    override fun bind(holder: ViewHolder<*>, model: Nomzod, pos: Int) {
        super.bind(holder, model, pos)
        holder.binding.apply {
            if (this is NomzodItemBinding) {
                setNomzod(model, userViewModel.user.hasNomzod)
                likeButtonThumb.isVisible = false
                dislikeButtonThumb.isVisible = false
                if (isBackGray) {
                    container.setBackgroundColor(
                        MaterialColors.getColor(
                            root, com.google.android.material.R.attr.backgroundColor
                        )
                    )
                }
                if (pos == currentList.lastIndex) {
                    next()
                }
            }
        }
    }
}