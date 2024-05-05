package com.uz.sovchi.ui.nomzod

import androidx.navigation.fragment.findNavController
import com.uz.sovchi.R
import com.uz.sovchi.databinding.NomzodUploadSuccessFragmentBinding
import com.uz.sovchi.ui.base.BaseFragment

class NomzodUploadSuccessFragment : BaseFragment<NomzodUploadSuccessFragmentBinding>() {

    override val layId: Int
        get() = R.layout.nomzod_upload_success_fragment

    override fun viewCreated(bind: NomzodUploadSuccessFragmentBinding) {
        bind.apply {
            nextButton.setOnClickListener {
                findNavController().popBackStack()
            }
        }
    }
}