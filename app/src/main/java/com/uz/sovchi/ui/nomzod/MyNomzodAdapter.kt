package com.uz.sovchi.ui.nomzod

import androidx.core.view.isVisible
import com.uz.sovchi.R
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.NomzodState
import com.uz.sovchi.data.nomzod.NomzodTarif
import com.uz.sovchi.data.nomzod.getStatusText
import com.uz.sovchi.databinding.NomzodMyItemBinding
import com.uz.sovchi.ui.base.BaseAdapter
import com.uz.sovchi.ui.base.EmptyDiffUtil

class MyNomzodAdapter(
    private val pay: (nomzod: Nomzod) -> Unit,
    private val loadNext: () -> Unit,
    private val click: (nomzod: Nomzod,settings:Boolean) -> Unit,
) : BaseAdapter<Nomzod, NomzodMyItemBinding>(R.layout.nomzod_my_item, EmptyDiffUtil()) {

    override fun bind(holder: ViewHolder<*>, model: Nomzod, pos: Int) {
        holder.apply {
            if (pos == currentList.size - 1) {
                loadNext.invoke()
            }
            binding.apply {
                (this as NomzodMyItemBinding)
                settingsButton.setOnClickListener {
                    click.invoke(model,true)
                }
                statusView.text = model.getStatusText()
                nomzodId.setNomzod(model, false, false)
                nomzodId.qoshimchaView.isVisible = false
                try {
                    tarifView.text =
                        root.context.getString(NomzodTarif.valueOf(model.tarif).nameRes)
                } catch (e: Exception) {
                    //
                }
                nomzodId.likeButton.isVisible = false
                if (model.state == NomzodState.NOT_PAID) {
                    payButton.isVisible = true
                }
                viewsView.text = model.views.toString()
                root.setOnClickListener {
                    click.invoke(model,false)
                }
                payButton.setOnClickListener {
                    pay.invoke(model)
                }
            }
        }
    }
}