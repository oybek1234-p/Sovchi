package com.uz.sovchi.ui.payment

import androidx.core.view.isVisible
import com.uz.sovchi.R
import com.uz.sovchi.data.nomzod.NomzodTarif
import com.uz.sovchi.databinding.PaymentItemBinding
import com.uz.sovchi.ui.base.BaseAdapter
import com.uz.sovchi.ui.base.EmptyDiffUtil

class PaymentAdapter :
    BaseAdapter<NomzodTarif, PaymentItemBinding>(R.layout.payment_item, EmptyDiffUtil()) {

    var selected: NomzodTarif = NomzodTarif.STANDART

    override fun bind(holder: ViewHolder<*>, model: NomzodTarif, pos: Int) {
        holder.apply {
            binding.apply {
                (this as PaymentItemBinding)
                nameView.text = root.context.getString(model.nameRes)
                val priceValue = model.priceSum
                val priceText =
                    if (priceValue == 0) root.context.getString(R.string.bepul) else "$priceValue sum"
                priceView.text = priceText
                infoView.text = model.infoRes.let {
                    try {
                        root.context.getString(it)
                    }catch (e: Exception) {
                        ""
                    }
                     }
                infoView.isVisible = model.infoRes != 0
                val isChecked = selected == model
                checkBox.isChecked = isChecked
                root.setOnClickListener {
                    selected = model
                    notifyDataSetChanged()
                }
            }
        }
    }
}