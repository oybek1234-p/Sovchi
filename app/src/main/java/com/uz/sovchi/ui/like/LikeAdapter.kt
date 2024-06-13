package com.uz.sovchi.ui.like

import coil.load
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.data.nomzod.KUYOV
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.OilaviyHolati
import com.uz.sovchi.databinding.LikeItemBinding
import com.uz.sovchi.ui.base.BaseAdapter
import com.uz.sovchi.ui.search.NOMZOD_DIFF_UTIL

class LikeAdapter(val next: () -> Unit) :
    BaseAdapter<Nomzod, LikeItemBinding>(R.layout.like_item, NOMZOD_DIFF_UTIL) {
        var onClick: (nomzod: Nomzod?) -> Unit = {}

    override fun bind(holder: ViewHolder<*>, model: Nomzod, pos: Int) {
        holder.apply {
            if (pos == currentList.size - 1) {
                next.invoke()
            }
            binding.apply {
                (this as LikeItemBinding)
                model.photos.firstOrNull()?.let {
                    photoView.load(it)
                }
                root.setOnClickListener {
                    onClick.invoke(model)
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
                    subtitleView.text =
                        appContext.getString(OilaviyHolati.valueOf(model.oilaviyHolati).resourceId)

                }
            }
        }
    }
}