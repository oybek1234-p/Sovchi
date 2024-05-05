package com.uz.sovchi.ui.auth

import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.databinding.FragmentUserNameBinding
import com.uz.sovchi.showKeyboard
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.visibleOrGone
import kotlinx.coroutines.launch

class GetUserNameFragment : BaseFragment<FragmentUserNameBinding>() {
    override val layId: Int
        get() = R.layout.fragment_user_name

    private var backEnabled = false

    companion object {
        const val BACK_ENABLED = "back"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backEnabled = arguments?.getBoolean(BACK_ENABLED) ?: false
    }

    private fun nameInput() = binding?.nameView?.editText?.text?.toString()

    private fun nameValid() = (nameInput()?.length ?: 0) > 2

    override fun onResume() {
        super.onResume()
        binding?.nameView?.editText?.showKeyboard()
    }

    private var set = false

    override fun viewCreated(bind: FragmentUserNameBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@GetUserNameFragment)
            toolbar.showArrowBack = true

            nameView.editText?.addTextChangedListener {
                continueButton.isEnabled = nameValid()
            }

            continueButton.setOnClickListener {
                if (set) return@setOnClickListener
                set = true
                progressBar.visibleOrGone(true)
                lifecycleScope.launch {
                    viewLifecycleOwner.lifecycleScope.launch {
                        progressBar.visibleOrGone(false)
                    }
                    userViewModel.updateUser(LocalUser.user.apply {
                        name = nameInput()!!
                    })
                    mainActivity()?.recreateUi()
                }
            }
        }
    }
}