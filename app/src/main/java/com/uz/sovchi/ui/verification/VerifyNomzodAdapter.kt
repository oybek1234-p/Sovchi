package com.uz.sovchi.ui.verification

import androidx.core.view.isVisible
import com.uz.sovchi.R
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.NomzodTarif
import com.uz.sovchi.data.nomzod.getStatusText
import com.uz.sovchi.databinding.VerifyNomzodItemBinding
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseAdapter
import com.uz.sovchi.ui.base.EmptyDiffUtil
import com.uz.sovchi.ui.nomzod.setNomzod

class VerifyNomzodAdapter(
    private val verify: (nomzod: Nomzod) -> Unit,
    private val loadNext: () -> Unit,
    private val click: (nomzod: Nomzod) -> Unit
) : BaseAdapter<Nomzod, VerifyNomzodItemBinding>(R.layout.verify_nomzod_item, EmptyDiffUtil()) {

    override fun bind(holder: ViewHolder<*>, model: Nomzod, pos: Int) {
        holder.apply {
            if (pos == currentList.size - 1) {
                loadNext.invoke()
            }
            binding.apply {
                (this as VerifyNomzodItemBinding)
                 hasChek.isVisible = model.paymentCheckPhotoUrl.isNotEmpty()
                statusView.text = model.getStatusText()
                nomzodItem.setNomzod(model, false, false)
                try {
                    tarifView.text =
                        root.context.getString(NomzodTarif.valueOf(model.tarif).nameRes)
                } catch (e: Exception) {
                    //
                }
                nomzodItem.likeButton.isVisible = false
                root.setOnClickListener {
                    click.invoke(model)
                }
                verifyButton.setOnClickListener {
                    verify.invoke(model)
                }
            }
        }
    }
}