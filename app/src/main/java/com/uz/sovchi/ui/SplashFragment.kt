package com.uz.sovchi.ui

import com.bumptech.glide.Glide
import com.uz.sovchi.R
import com.uz.sovchi.databinding.FragmentSplashBinding
import com.uz.sovchi.ui.base.BaseFragment

class SplashFragment : BaseFragment<FragmentSplashBinding>() {

    override val layId: Int
        get() = R.layout.fragment_splash

    override fun viewCreated(bind: FragmentSplashBinding) {
        bind.apply {
            showBottomSheet = false
            Glide.with(requireContext()).load(R.drawable.splash_photo).into(photo)
            start.setOnClickListener {
                navigate(R.id.authFragment)
            }
        }
    }
}