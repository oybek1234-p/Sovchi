package com.uz.sovchi.ui

import android.os.Bundle
import android.view.View
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.isAdmin
import com.uz.sovchi.data.nomzod.NomzodState
import com.uz.sovchi.databinding.NomzodSettingsBinding
import com.uz.sovchi.ui.base.BaseFragment

class SettingsFragment : BaseFragment<NomzodSettingsBinding>() {

    override val layId: Int
        get() = R.layout.nomzod_settings

    override fun viewCreated(bind: NomzodSettingsBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@SettingsFragment)
            verifyButton.setOnClickListener {
                navigate(R.id.adminVerificationFragment, Bundle().apply {
                    putInt("state", NomzodState.CHECKING)
                })
            }
            aciveButton.setOnClickListener {
                navigate(R.id.adminVerificationFragment, Bundle().apply {
                    putInt("state", NomzodState.VISIBLE)
                })
            }
            rejectedButton.setOnClickListener {
                navigate(R.id.adminVerificationFragment, Bundle().apply {
                    putInt("state", NomzodState.REJECTED)
                })
            }
            allUsers.setOnClickListener {
                navigate(R.id.allUsersFragment)
            }
            if (LocalUser.user.isAdmin()) {
                allUsers.visibility = View.VISIBLE
                verifyButton.visibility = View.VISIBLE
            }
        }
    }
}