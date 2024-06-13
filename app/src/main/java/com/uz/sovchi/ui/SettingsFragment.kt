package com.uz.sovchi.ui

import android.app.AlertDialog
import android.os.Bundle
import com.uz.sovchi.R
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.databinding.NomzodSettingsBinding
import com.uz.sovchi.ui.base.BaseFragment

class SettingsFragment : BaseFragment<NomzodSettingsBinding>() {

    override val layId: Int
        get() = R.layout.nomzod_settings

    override fun viewCreated(bind: NomzodSettingsBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@SettingsFragment)

            verifyButton.setOnClickListener {
                navigate(R.id.adminVerificationFragment)
            }
            allUsers.setOnClickListener {
                navigate(R.id.allUsersFragment)
            }
            rateButton.setOnClickListener {
                mainActivity()?.requestReview()
            }
            boglanishButton.setOnClickListener {
                mainActivity()?.showSupportSheet()
            }
            signOut.setOnClickListener {
                val dialog = AlertDialog.Builder(requireContext())
                dialog.setTitle(getString(R.string.logout))
                dialog.setPositiveButton(getString(R.string.close)) { _, _ ->
                    MyNomzodController.clear()
                    userViewModel.signOut()
                    mainActivity()?.recreateUi()
                }
                dialog.setNegativeButton(getString(R.string.cancek)) { _, _ ->
                }
                dialog.create().show()
            }
        }
    }
}