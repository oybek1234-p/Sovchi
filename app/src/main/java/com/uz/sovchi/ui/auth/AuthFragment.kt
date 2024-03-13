package com.uz.sovchi.ui.auth

import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.github.vacxe.phonemask.PhoneMaskManager
import com.uz.sovchi.PhoneUtils
import com.uz.sovchi.R
import com.uz.sovchi.databinding.FragmentAuthBinding
import com.uz.sovchi.showKeyboard
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.visibleOrGone


class AuthFragment : BaseFragment<FragmentAuthBinding>() {
    override val layId: Int
        get() = R.layout.fragment_auth

    private var phoneMask: PhoneMaskManager? = null
    private val phoneText: String? get() = phoneMask?.phone

    private var savedPhone: String? = null

    private val viewModel: AuthViewModel by viewModels()

    private fun phoneValid(): Boolean {
        return (phoneText?.length ?: 0) == 13
    }

    override fun onResume() {
        super.onResume()
        binding?.phoneView?.editText?.showKeyboard()
    }

    override fun onDestroyView() {
        savedPhone = phoneText
        super.onDestroyView()
    }

    override fun viewCreated(bind: FragmentAuthBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@AuthFragment)

            phoneView.apply {
                editText?.apply {
                    phoneMask = PhoneUtils.phoneMask(this)
                    addTextChangedListener {
                        viewModel.sendState.postValue(SendState.Input(phoneText))
                    }
                    savedPhone?.let { setText(it) }
                }
            }

            viewModel.apply {
                sendState.observe(viewLifecycleOwner) { state ->
                    val sending = state is SendState.Sending

                    progressBar.visibleOrGone(sending)
                    phoneView.isEnabled = sending.not()
                    continueButton.isEnabled = sending.not()

                    val isError = state is SendState.Error
                    phoneView.isErrorEnabled = isError
                    if (isError) {
                        phoneView.error = (state as SendState.Error).exception.message
                    }
                    if (state is SendState.Input) {
                        continueButton.isEnabled = phoneValid()
                    }
                    if (state is SendState.Success) {
                        val code = state.code
                        if (code.isNullOrEmpty().not()) {
                            val bundle = Bundle().apply {
                                putString(PHONE, phoneText)
                                putString(VERIFY_CODE, code)
                            }
                            navigate(
                                R.id.action_authFragment_to_verifyFragment,
                                bundle
                            )
                            sendState.postValue(SendState.Input(phoneText))
                        }
                    }
                }
            }
            continueButton.setOnClickListener {
                viewModel.sendCode(requireActivity(), phoneText!!)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopAutoCodeReceiver()
    }

    companion object {
        const val VERIFY_CODE = "verify_code"
        const val PHONE = "phone"
    }
}