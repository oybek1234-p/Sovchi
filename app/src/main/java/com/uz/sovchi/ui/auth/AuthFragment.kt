package com.uz.sovchi.ui.auth

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.vacxe.phonemask.PhoneMaskManager
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.uz.sovchi.PermissionController
import com.uz.sovchi.PhoneUtils
import com.uz.sovchi.R
import com.uz.sovchi.data.filter.MyFilter
import com.uz.sovchi.data.valid
import com.uz.sovchi.databinding.FragmentAuthBinding
import com.uz.sovchi.showKeyboard
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.visibleOrGone
import kotlinx.coroutines.launch


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

    private var signInRequest: BeginSignInRequest? = null
    private var oneTapClient: SignInClient? = null

    private fun loginGoogle() {
        PermissionController.getInstance()
            .doOnActivityResult(PermissionController.ANY_REQUEST_CODE) { res, resultOk ->
                if (resultOk) {
                    if (res is Intent) {
                        try {
                            val credential = oneTapClient?.getSignInCredentialFromIntent(res)
                            val idToken = credential?.googleIdToken
                            if (idToken != null) {
                                val firebaseCredential =
                                    GoogleAuthProvider.getCredential(idToken, null)
                                if (context != null) {
                                    viewModel.sendState.postValue(SendState.Sending)
                                    viewModel.signInGoogle(requireContext(), firebaseCredential) {
                                        if (it) {
                                            verified()
                                        } else {
                                            viewModel.sendState.postValue(SendState.Input())
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            //
                        }
                    }
                }
            }
        signInRequest?.let { it ->
            oneTapClient?.beginSignIn(it)?.addOnSuccessListener {
                try {
                    startIntentSenderForResult(
                        it.pendingIntent.intentSender, 2, null, 0, 0, 0, null
                    )
                } catch (e: IntentSender.SendIntentException) {
                    showToast(e.message ?: "")
                }
            }?.addOnFailureListener {
                showToast(it.message ?: "")
            }
        }
    }

    private fun verified() {
        viewModel.sendState.postValue(SendState.Sending)
        lifecycleScope.launch {
            val user = userViewModel.authFirebaseUser()
            if (user.valid.not()) return@launch
            closeFragment()
            MyFilter.update()
            if (user!!.name.isEmpty()) {
                navigate(R.id.getUserNameFragment)
            } else {
                mainActivity()?.recreateUi()
            }
        }
    }

    private fun initGoogleAuth() {
        oneTapClient = Identity.getSignInClient(requireActivity())
        signInRequest = BeginSignInRequest.builder().setGoogleIdTokenRequestOptions(
            BeginSignInRequest.GoogleIdTokenRequestOptions.builder().setSupported(true)
                .setServerClientId(getString(R.string.your_web_client_id))
                .setFilterByAuthorizedAccounts(false).build()
        ).build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initGoogleAuth()
    }

    override fun viewCreated(bind: FragmentAuthBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@AuthFragment)

            gmailButton.setOnClickListener {
                loginGoogle()
            }
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
                        val error = (state as SendState.Error).exception.message
                        if (error != null) {
                            phoneView.error = getString(R.string.xatolik_yuz_berdi)
                            if (error.startsWith("An internal error")){
                                phoneView.error = "Gmail orqali kiring"
                                loginGoogle()
                            }
                        }
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
                                R.id.verifyFragment, bundle
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