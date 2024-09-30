package com.uz.sovchi.ui.nomzod

import com.uz.sovchi.R
import com.uz.sovchi.data.nomzod.Talablar
import com.uz.sovchi.databinding.TalablarItemBinding
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseAdapter
import com.uz.sovchi.ui.base.EmptyDiffUtil
import com.uz.sovchi.visibleOrGone

class TalablarAdapter : BaseAdapter<Talablar,TalablarItemBinding>(R.layout.talablar_item,EmptyDiffUtil<Talablar>()) {

    var showCheckBox = true
    val selectedTalablar = mutableSetOf<Talablar>()
    
    override fun onViewCreated(holder: ViewHolder<TalablarItemBinding>, viewType: Int) {
        super.onViewCreated(holder, viewType)
        holder.apply {
            binding.apply {
                checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                    val item = currentList[holder.adapterPosition]
                    if (isChecked) {
                        if (item == Talablar.OilaQurmagan && selectedTalablar.contains(Talablar.BuydoqlarTaqiq)) {
                            showToast("Faqat ajrashgan... o'chiring")
                            checkbox.isChecked = false
                            return@setOnCheckedChangeListener
                        }
                        if (item == Talablar.BuydoqlarTaqiq && selectedTalablar.contains(Talablar.OilaQurmagan)) {
                            showToast("Oila qurmagani bo'l... o'chiring")
                            checkbox.isChecked = false
                            return@setOnCheckedChangeListener
                        }
                    }
                    if (selectedTalablar.contains(item)) {
                        selectedTalablar.remove(item)
                    } else {
                        selectedTalablar.add(item)
                    }
                }

            }
        }
    }

    override fun bind(holder: ViewHolder<*>, model: Talablar, pos: Int) {
        holder.binding.apply {
            (this as TalablarItemBinding)
            if (showCheckBox) {
                checkbox.visibleOrGone(true)
                textView.visibleOrGone(false)
                checkbox.isChecked = selectedTalablar.contains(model)
                checkbox.text = root.context.getString(model.textId)
            } else {
                checkbox.visibleOrGone(false)
                textView.visibleOrGone(true)
                textView.text = root.context.getString(model.textId)
            }
        }
    }
}