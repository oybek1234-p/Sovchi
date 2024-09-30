package com.uz.sovchi


import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import coil.load
import com.google.android.material.color.MaterialColors
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.UserRepository
import com.uz.sovchi.data.isAdmin
import com.uz.sovchi.data.like.LikeState
import com.uz.sovchi.data.nomzod.KELIN
import com.uz.sovchi.data.nomzod.KUYOV
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.NomzodRepository
import com.uz.sovchi.data.nomzod.NomzodState
import com.uz.sovchi.data.nomzod.getStatusText
import com.uz.sovchi.data.nomzod.showNeedVerifyInfo
import com.uz.sovchi.data.premium.getPremiumExpireDate
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

    private fun initUi(nomzod: Nomzod) {
        val isEmpty = nomzod.id.isEmpty()
        val photo = nomzod.photos.firstOrNull()

        binding?.apply {
            if (isEmpty.not()) {
                photoView.loadPhoto((photo ?: "").ifEmpty {
                    if (MyNomzodController.nomzod.type == KUYOV) Nomzod.KUYOV_TEXT else Nomzod.KELIN_TEXT
                })
                nameView.text = "${nomzod.name} ${nomzod.tugilganYili}"

                var statusText = nomzod.getStatusText()
                if (nomzod.state == NomzodState.VISIBLE) {
                    if (UserRepository.user.premium) {
                        statusText += " Premium"
                    }
                }
                statusView.setTextColor(
                    if (nomzod.state == NomzodState.VISIBLE || nomzod.state == NomzodState.CHECKING) MaterialColors.getColor(
                        statusView, com.google.android.material.R.attr.colorPrimary
                    ) else Color.RED
                )
                statusView.isVisible = statusText.trim().isNotEmpty()
                statusView.text = statusText
            } else {
                photoView.load(R.drawable.user_photo_placeholder)
                statusView.isVisible = false
                fillPercent.isVisible = false
                nameView.text = LocalUser.user.name
            }
            val hasPremium = LocalUser.user.premium
            premiumButton.isVisible = hasPremium.not() && LocalUser.user.hasNomzod
            premiumView.isVisible = hasPremium.not() && LocalUser.user.hasNomzod
            premiumActiveC.isVisible = hasPremium
            if (hasPremium) {
                premiumExpireDate.text = "${UserRepository.user.getPremiumExpireDate()} gacha"
            }
            val openEdit = {
                findNavController().navigate(R.id.addNomzodFragment, Bundle().apply {
                    putString("nId", nomzod.id)
                })
            }
            editInfoButton.text =
                if (isEmpty) getString(R.string.fill_profile) else getString(R.string.edit)
            editInfoButton.setOnClickListener {
                openEdit.invoke()
            }
            photoView.setOnClickListener {
                openEdit.invoke()
            }
            nameView.setOnClickListener {
                openEdit.invoke()
            }
            verifiedBadge.isVisible = MyNomzodController.nomzod.verified
            verifyInfoParent.isVisible = MyNomzodController.nomzod.showNeedVerifyInfo()
            verifyButton.setOnClickListener {
                navigate(R.id.addVerificationInfoFragment)
            }
        }
    }

    override fun viewCreated(bind: ProfileFragmentBinding) {
        showBottomSheet = true
        bind.apply {
            rateButton.setOnClickListener {
                mainActivity()?.requestReview()
            }
            authView.root.visibleOrGone(UserRepository.user.valid.not())
            deleteProfile.setOnClickListener {
                val dialog = AlertDialog.Builder(requireContext())
                dialog.setTitle(getString(R.string.profilni_o_chirish))
                dialog.setMessage("Profil malumotlaringiz o'chib ketadi, keyinchalik qayta yana ochsangiz bo'ladi")
                dialog.setPositiveButton(getString(R.string.o_chirish)) { _, _ ->
                    NomzodRepository.deleteNomzod(LocalUser.user.uid)
                    MyNomzodController.clear()
                    mainActivity()?.recreateUi()
                }
                dialog.setNegativeButton(getString(R.string.cancek)) { _, _ ->
                }
                dialog.create().show()
            }
            signOut.setOnClickListener {
                val dialog = AlertDialog.Builder(requireContext())
                dialog.setTitle(getString(R.string.logout))
                dialog.setPositiveButton(getString(R.string.close)) { _, _ ->
                    MyNomzodController.clear()
                    UserRepository.signOut()
                    mainActivity()?.recreate()
                }
                dialog.setNegativeButton(getString(R.string.cancek)) { _, _ ->
                }
                dialog.create().show()
            }
            settings.isVisible = LocalUser.user.isAdmin()
            authView.apply {
                authButton.setOnClickListener {
                    findNavController().navigate(R.id.authFragment)
                }
                authView.boglanishButtonV.setOnClickListener {
                    mainActivity()?.showSupportSheet()
                }
            }
            disliked.setOnClickListener {
                navigate(R.id.likedFragment, Bundle().apply {
                    putInt("type", LikeState.DISLIKED)
                })
            }
            settings.setOnClickListener {
                navigate(R.id.settingsFragment)
            }
            mainLayout.visibleOrGone(UserRepository.user.valid)
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