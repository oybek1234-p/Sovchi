package com.uz.sovchi.ui.search

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
import com.uz.sovchi.databinding.NomzodItemNewBinding
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
) : BaseAdapter<Nomzod, NomzodItemNewBinding>(R.layout.nomzod_item_new, NOMZOD_DIFF_UTIL) {

    override fun onViewCreated(holder: ViewHolder<NomzodItemNewBinding>, viewType: Int) {
        super.onViewCreated(holder, viewType)
        holder.binding.apply {

            cardView.setOnClickListener {
                try {
                    onClick.invoke(currentList[holder.adapterPosition])
                } catch (e: Exception) {
                    //
                }
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
            if (this is NomzodItemNewBinding) {
                setNomzod(model, userViewModel.user.hasNomzod)
                if (pos == currentList.lastIndex) {
                    next()
                }
            }
        }
    }
}