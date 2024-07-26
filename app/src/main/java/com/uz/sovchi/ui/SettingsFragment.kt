package com.uz.sovchi.ui

import android.app.AlertDialog
import android.view.View
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.data.nomzod.NomzodRepository
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
            deleteProfile.setOnClickListener {
                val dialog = AlertDialog.Builder(requireContext())
                dialog.setTitle(getString(R.string.profilni_o_chirish))
                dialog.setMessage("Profil malumotlaringiz o'chib ketadi, keyinchalik qayta yana ochsangiz bo'ladi")
                dialog.setPositiveButton(getString(R.string.o_chirish)) { _, _ ->
                    NomzodRepository.deleteNomzod(MyNomzodController.nomzod.id)
                    MyNomzodController.clear()
                    mainActivity()?.recreateUi()
                }
                dialog.setNegativeButton(getString(R.string.cancek)) { _, _ ->
                }
                dialog.create().show()
            }
            boglanishButton.setOnClickListener {
                mainActivity()?.showSupportSheet()
            }
            if (LocalUser.user.phoneNumber == "+998971871415") {
                allUsers.visibility = View.VISIBLE
                verifyButton.visibility = View.VISIBLE
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