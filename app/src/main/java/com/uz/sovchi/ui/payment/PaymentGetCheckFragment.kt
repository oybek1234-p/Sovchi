package com.uz.sovchi.ui.payment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import com.uz.sovchi.R
import com.uz.sovchi.data.nomzod.NomzodState
import com.uz.sovchi.data.nomzod.NomzodTarif
import com.uz.sovchi.data.payment.PaymentInfo
import com.uz.sovchi.databinding.PaymentCheckFragmentBinding
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.nomzod.NomzodViewModel
import com.uz.sovchi.ui.photo.PickPhotoFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class PaymentGetCheckFragment : BaseFragment<PaymentCheckFragmentBinding>() {

    override val layId: Int
        get() = R.layout.payment_check_fragment

    private var checkPath: String? = null

    private var nomzodId: String? = null
    private var tarif: NomzodTarif = NomzodTarif.TOP_3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nomzodId = arguments?.getString("value")
        val tarifName = arguments?.getString("tarif")
        if (tarifName.isNullOrEmpty().not()) {
            try {
                tarif = NomzodTarif.valueOf(tarifName.toString())
            } catch (e: Exception) {
                //
            }
        }
    }

    override fun viewCreated(bind: PaymentCheckFragmentBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@PaymentGetCheckFragment)
            textView10.text = tarif.priceSum.toString() + " sum"
            PaymentInfo.loadPaymentCard {
                cardNumber.text = it
            }
            checkButton.setOnClickListener {
                PickPhotoFragment(false) {
                    if (it.isNotEmpty()) {
                        checkPath = it.first().path
                        checkImage.load(checkPath)
                        checkImage.isVisible = true
                        nextButton.isEnabled = true
                    }
                }.open(mainActivity()!!)
            }
            copyButton.setOnClickListener {
                val clipboard: ClipboardManager? =
                    requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                val text = cardNumber.text.toString().replace(" ", "")
                val clip = ClipData.newPlainText("Sovchi karta", text)
                clipboard?.setPrimaryClip(clip)
            }
            nextButton.setOnClickListener {
                if (checkPath.isNullOrEmpty()) {
                    showToast("Chekni kiriting")
                    return@setOnClickListener
                }
                upload()
            }
        }
    }

    private val nomzodViewModel: NomzodViewModel by activityViewModels()

    private fun upload() {
        if (checkPath.isNullOrEmpty() || nomzodId.isNullOrEmpty()) return
        binding?.progressBar?.isVisible = true
        binding?.nextButton?.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            nomzodViewModel.repository.uploadPaymentData(checkPath!!, nomzodId!!) {
                lifecycleScope.launch(Dispatchers.Main) {
                    binding?.progressBar?.isVisible = false
                    nomzodViewModel.repository.myNomzods.find { it.id == nomzodId }?.apply {
                        state = NomzodState.CHECKING
                    }
                    if (it.not()) {
                        binding?.nextButton?.isEnabled = true
                    } else {
                        closeFragment()
                        navigate(R.id.nomzodUploadSuccessFragment)
                    }
                }
            }
        }
    }
}