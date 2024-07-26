package com.uz.sovchi.ui

import com.uz.sovchi.R
import com.uz.sovchi.databinding.FragmentSplashBinding
import com.uz.sovchi.ui.base.BaseFragment

class SplashFragment : BaseFragment<FragmentSplashBinding>() {

    override val layId: Int
        get() = R.layout.fragment_splash

    override fun viewCreated(bind: FragmentSplashBinding) {
        bind.apply {
            showBottomSheet = false
            start.setOnClickListener {
                navigate(R.id.authFragment)
            }
        }
    }
}