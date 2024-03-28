package com.uz.sovchi

import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.uz.sovchi.data.saved.SavedRepository
import com.uz.sovchi.data.valid
import com.uz.sovchi.databinding.ProfileFragmentBinding
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.nomzod.NomzodViewModel

class ProfileFragment : BaseFragment<ProfileFragmentBinding>() {

    override val layId: Int
        get() = R.layout.profile_fragment

    private val nomzodViewModel: NomzodViewModel by activityViewModels<NomzodViewModel>()

    override fun viewCreated(bind: ProfileFragmentBinding) {
        showBottomSheet = true
        bind.apply {
            authView.root.visibleOrGone(userViewModel.user.valid.not())
            authView.apply {
                authButton.setOnClickListener {
                    findNavController().navigate(R.id.auth_graph)
                }
            }

            mainLayout.visibleOrGone(userViewModel.user.valid)
            val user = userViewModel.user
            nameView.text = user.name
            phoneView.text = PhoneUtils.formatPhoneNumber(user.phoneNumber)

            userViewModel.user.phoneNumber.let {
                allUsers.isVisible = it == "+998971871415"
            }
            allUsers.setOnClickListener {
                navigate(R.id.allUsersFragment)
            }
            rateButton.setOnClickListener {
                mainActivity()?.requestReview()
            }
            boglanishButton.setOnClickListener {
                SocialMedia.openLink(
                    requireActivity(),
                    SocialMedia.parseTelegramLink("@oybek_tech")
                )
            }
            signOut.setOnLongClickListener {
                SavedRepository.clear()
                nomzodViewModel.repository.clearMyNomzods()
                userViewModel.signOut()

                mainActivity()?.recreateUi()
                return@setOnLongClickListener true
            }
        }
    }
}