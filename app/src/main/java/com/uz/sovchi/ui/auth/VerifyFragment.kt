package com.uz.sovchi.ui.auth

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.uz.sovchi.R
import com.uz.sovchi.data.filter.MyFilter
import com.uz.sovchi.data.valid
import com.uz.sovchi.databinding.FragmentVerifyBinding
import com.uz.sovchi.ifNotNullOrEmpty
import com.uz.sovchi.showKeyboard
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.visibleOrGone
import kotlinx.coroutines.launch

class VerifyFragment : BaseFragment<FragmentVerifyBinding>() {

    override val layId: Int
        get() = R.layout.fragment_verify

    private val viewModel: AuthViewModel by viewModels()

    private var sendInfo: SendInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val code = arguments?.getString(AuthFragment.VERIFY_CODE) ?: return
        val phone = arguments?.getString(AuthFragment.PHONE) ?: return
        sendInfo = SendInfo(code, phone)
    }

    private val inputCode: InputCode get() = InputCode(binding?.phoneView?.editText?.text?.toString())

    @SuppressLint("SetTextI18n")
    override fun viewCreated(bind: FragmentVerifyBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@VerifyFragment)
            if (sendInfo == null) {
                return
            }
            textView.text = "${sendInfo?.phone} ${getString(R.string.sms_kod_yuborildi)}"
            viewModel.apply {

                verifyState.observe(viewLifecycleOwner) { state ->
                    val verifying = state is VerifyState.Verifying
                    val isError = state is VerifyState.Error
                    progressBar.visibleOrGone(verifying)
                    phoneView.isEnabled = verifying.not()
                    continueButton.isEnabled = verifying.not()
                    if (state is VerifyState.Input) {
                        continueButton.isEnabled = inputCode.isValid()
                    }
                    phoneView.isErrorEnabled = isError
                    if (state is VerifyState.Error) {
                        phoneView.error = state.exception.message
                        phoneView.editText?.showKeyboard()
                    }
                    if (state is VerifyState.Success) {
                        verified()
                    }
                }

                autoCodeReceiverObserver.observe(viewLifecycleOwner) { code ->
                    phoneView.editText?.setText(code)
                    code.ifNotNullOrEmpty {
                        viewModel.verifyCode(requireContext(), it, sendInfo?.code!!)
                    }
                }

                startAutoCodeReceiver(requireContext())
            }

            phoneView.editText?.addTextChangedListener {
                viewModel.verifyState.postValue(VerifyState.Input(inputCode))
            }
            continueButton.setOnClickListener {
                viewModel.verifyCode(requireContext(), inputCode.code!!, sendInfo?.code!!)
            }
        }
    }

    private fun verified() {
        viewModel.verifyState.postValue(VerifyState.Verifying)
        lifecycleScope.launch {
            val user = userViewModel.authFirebaseUser()
            viewModel.verifyState.postValue(VerifyState.Input(inputCode))
            if (user.valid.not()) return@launch
            MyFilter.update()
            if (user!!.name.isEmpty()) {
                navigate(R.id.getUserNameFragment)
            } else {
                mainActivity()?.recreateUi()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding?.phoneView?.editText?.showKeyboard()
    }
}