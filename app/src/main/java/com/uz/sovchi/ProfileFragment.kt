package com.uz.sovchi


import android.os.Bundle
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import coil.load
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.NomzodState
import com.uz.sovchi.data.nomzod.getStatusText
import com.uz.sovchi.data.valid
import com.uz.sovchi.databinding.ProfileFragmentBinding
import com.uz.sovchi.ui.base.BaseFragment

class ProfileFragment : BaseFragment<ProfileFragmentBinding>() {

    override val layId: Int
        get() = R.layout.profile_fragment

    private fun setLoading(loading: Boolean) {
        binding?.apply {
            val showProgress = loading && MyNomzodController.nomzod.id.isEmpty()
            progressBar.visibleOrGone(showProgress)
            editInfoButton.visibleOrGone(showProgress.not())
        }
    }

    private fun loadAd() {
        binding?.adView?.loadAd()
    }

    override fun onResume() {
        super.onResume()
        loadAd()
    }


    private fun getProfileFillPercent(nomzod: Nomzod): Int {
        var percent = 0
        if (nomzod.photos.isNotEmpty()) {
            percent += 25
        }
        if (nomzod.name.isNotEmpty()) {
            percent += 25
        }
        if (nomzod.tugilganYili != 0) {
            percent += 5
        }
        if (nomzod.talablar.isNotEmpty()) {
            percent += 5
        }
        if (nomzod.mobilRaqam.isNotEmpty()) {
            percent += 10
        }
        if (nomzod.telegramLink.isNotEmpty()) {
            percent += 5
        }
        if (nomzod.ishJoyi.isNotEmpty()) {
            percent += 15
        }
        if (nomzod.buyi > 0 || nomzod.vazni > 0) {
            percent += 10
        }

        return percent
    }

    private fun initFillPercent(nomzod: Nomzod) {
        binding?.fillPercent?.apply {
            isVisible = true
            val percent = getProfileFillPercent(nomzod)
            text = "${getString(R.string.toldirilgan)} $percent%"
        }
    }

    private fun initUi(nomzod: Nomzod) {
        val isEmpty = nomzod.id.isEmpty()
        val photo = nomzod.photos.firstOrNull()
        binding?.apply {
            if (isEmpty.not()) {
                Glide.with(photoView).load(photo).into(photoView)
                nameView.text = "${nomzod.name} ${nomzod.tugilganYili}"

                var statusText = nomzod.getStatusText()
                if (nomzod.state == NomzodState.VISIBLE) {
                    if (userViewModel.user.premium) {
                        statusText += " Premium"
                    }
                }
                statusView.isVisible = statusText.trim().isNotEmpty()
                statusView.text = statusText
                initFillPercent(nomzod)
            } else {
                photoView.load(R.drawable.user_photo_placeholder)
                statusView.isVisible = false
                fillPercent.isVisible = false
                nameView.text = LocalUser.user.name
            }
            editInfoButton.text =
                if (isEmpty) getString(R.string.fill_profile) else getString(R.string.edit)
            editInfoButton.setOnClickListener {
                findNavController().navigate(R.id.addNomzodFragment, Bundle().apply {
                    putString("nId", nomzod.id)
                })
            }
        }
    }

    override fun viewCreated(bind: ProfileFragmentBinding) {
        showBottomSheet = true
        bind.apply {
            rateButton.setOnClickListener {
                mainActivity()?.requestReview()
            }
            authView.root.visibleOrGone(userViewModel.user.valid.not())
            authView.apply {
                authButton.setOnClickListener {
                    findNavController().navigate(R.id.authFragment)
                }
                authView.boglanishButton.setOnClickListener {
                    mainActivity()?.showSupportSheet()
                }
            }
            settings.setOnClickListener {
                navigate(R.id.settingsFragment)
            }
            mainLayout.visibleOrGone(userViewModel.user.valid)
            val hasPremium = LocalUser.user.premium
            premiumButton.isVisible = hasPremium.not()
            premiumButton.setOnClickListener {
                mainActivity()?.showPremiumSheet()
            }
            boglanishButton.setOnClickListener {
                mainActivity()?.showSupportSheet()
            }
            MyNomzodController.apply {
                nomzodLive.observe(viewLifecycleOwner) {
                    if (it != null) {
                        initUi(it)
                    }
                }
                nomzodLoading.observe(viewLifecycleOwner) {
                    setLoading(it)
                }
            }
        }
    }
}