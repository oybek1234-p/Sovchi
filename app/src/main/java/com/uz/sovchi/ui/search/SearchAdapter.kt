package com.uz.sovchi.ui.search

import com.uz.sovchi.R
import com.uz.sovchi.UserViewModel
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.nomzod.AppRoomDatabase
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.saved.SavedRepository
import com.uz.sovchi.data.valid
import com.uz.sovchi.databinding.NomzodItemBinding
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseAdapter
import com.uz.sovchi.ui.base.EmptyDiffUtil
import com.uz.sovchi.ui.nomzod.setNomzod

class SearchAdapter(
    val userViewModel: UserViewModel,
    val next: () -> Unit,
    private val onClick: (nomzod: Nomzod) -> Unit,
    private val onLiked: ((liked: Boolean, nomzodId: String) -> Unit)? = null
) :
    BaseAdapter<Nomzod, NomzodItemBinding>(R.layout.nomzod_item, EmptyDiffUtil()) {

    override fun onViewCreated(holder: ViewHolder<NomzodItemBinding>, viewType: Int) {
        super.onViewCreated(holder, viewType)
        holder.binding.apply {
            root.setOnClickListener {
                onClick.invoke(currentList[holder.adapterPosition])
            }
            likeButton.setOnClickListener {
                val nomzod = currentList[holder.adapterPosition]
                val isLiked = likeItem(nomzod)
                onLiked?.invoke(isLiked.not(), nomzod.id)
                notifyItemChanged(holder.adapterPosition)
            }
        }
    }

    companion object {
        fun likeItem(nomzod: Nomzod): Boolean {
            if (LocalUser.user.valid.not()) {
                showToast("Akkauntga kiring!")
                return false
            }
            val isLiked = SavedRepository.isNomzodLiked(nomzod.id)
            SavedRepository.apply {
                if (savedLoading.value == true) return false
                if (isLiked) {
                    removeFromSaved(nomzod.id)
                } else {
                    addToSaved(nomzod)
                }
            }
            return isLiked
        }
    }

    override fun bind(holder: ViewHolder<*>, model: Nomzod, pos: Int) {
        super.bind(holder, model, pos)
        holder.binding.apply {
            if (this is NomzodItemBinding) {
                setNomzod(model, userViewModel.user.hasNomzod)
                if (pos == currentList.lastIndex) {
                    next()
                }
            }
        }
    }
}