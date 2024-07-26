package com.uz.sovchi.ui.verification

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.uz.sovchi.R
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.getStatusText
import com.uz.sovchi.databinding.VerifyNomzodItemBinding
import com.uz.sovchi.toDp
import com.uz.sovchi.ui.base.BaseAdapter
import com.uz.sovchi.ui.base.EmptyDiffUtil
import com.uz.sovchi.ui.nomzod.setNomzod

class VerifyNomzodAdapter(
    private val loadNext: () -> Unit,
    private val click: (nomzod: Nomzod) -> Unit,
    private val verify: (nomzod: Nomzod, type: AdminVerificationFragment.VerifyTypes) -> Unit
) : BaseAdapter<Nomzod, VerifyNomzodItemBinding>(R.layout.verify_nomzod_item, EmptyDiffUtil()) {

    override fun bind(holder: ViewHolder<*>, model: Nomzod, pos: Int) {
        holder.apply {
            if (pos == currentList.size - 1) {
                loadNext.invoke()
            }
            binding.apply {
                (this as VerifyNomzodItemBinding)
                hasChek.isVisible = model.paymentCheckPhotoUrl?.isNotEmpty() ?: false
                statusView.text = model.getStatusText()
                nomzodItem.setNomzod(model, false, false, true)
                nomzodItem.photoView.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = toDp(280f, root.context)
                }
                userId.text = model.userId
                root.setOnClickListener {
                    click.invoke(model)
                }
                deleteButton.setOnClickListener {
                    verify.invoke(model, AdminVerificationFragment.VerifyTypes.DELETE)
                }
                verifyButton.setOnClickListener {
                    verify.invoke(model, AdminVerificationFragment.VerifyTypes.ACCEPTED)
                }
                verifyPremiumButton.setOnClickListener{
                    verify.invoke(model, AdminVerificationFragment.VerifyTypes.PREMIUM_ACCEPTED)
                }
                photoWrong.setOnClickListener {
                    verify.invoke(model, AdminVerificationFragment.VerifyTypes.PHOTO_WRONG)
                }
                infoWrong.setOnClickListener {
                    verify.invoke(model, AdminVerificationFragment.VerifyTypes.INFO_WRONG)
                }
            }
        }
    }
}