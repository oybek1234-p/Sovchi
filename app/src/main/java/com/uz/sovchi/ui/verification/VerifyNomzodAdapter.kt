package com.uz.sovchi.ui.verification

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.uz.sovchi.data.utils.DateUtils
import com.uz.sovchi.R
import com.uz.sovchi.data.UserRepository
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.premium.premiumExpired
import com.uz.sovchi.databinding.VerifyNomzodItemBinding
import com.uz.sovchi.toDp
import com.uz.sovchi.ui.base.BaseAdapter
import com.uz.sovchi.ui.base.EmptyDiffUtil
import com.uz.sovchi.ui.nomzod.setNomzod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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
                nomzodItem.setNomzod(model)
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
                GlobalScope.launch(Dispatchers.Main) {
                    val user = UserRepository.loadUser(model.userId) ?: return@launch
                    verifyPremiumButton.isVisible =
                        model.paymentCheckPhotoUrl.isNullOrEmpty().not() && user.premium.not()
                    disablePremiumButton.isVisible = user.premiumExpired() && user.premium
                    disablePremiumButton.setOnClickListener {
                        verify.invoke(model, AdminVerificationFragment.VerifyTypes.DISABLE_PREMIUM)
                    }
                    statusView.text = DateUtils.formatDate(model.uploadDate) + " -- ${
                        DateUtils.formatDate(user.lastSeenTime)
                    }"
                    verifyInfoButton.setOnClickListener {
                        verify.invoke(model, AdminVerificationFragment.VerifyTypes.FULL_ACCEPTED)
                    }
                    blockView.setOnClickListener {
                        verify.invoke(model, AdminVerificationFragment.VerifyTypes.BLOCK)
                    }
                    verifyInfoButton.isVisible = model.verified.not()
                    verifyButton.setOnClickListener {
                        verify.invoke(model, AdminVerificationFragment.VerifyTypes.ACCEPTED)
                    }

                    verifyPremiumButton.setOnClickListener {
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
}