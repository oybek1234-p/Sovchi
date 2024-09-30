package com.uz.sovchi.ui.auth

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.github.vacxe.phonemask.PhoneMaskManager
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.GoogleAuthProvider
import com.uz.sovchi.PermissionController
import com.uz.sovchi.PhoneUtils
import com.uz.sovchi.R
import com.uz.sovchi.data.UserRepository
import com.uz.sovchi.data.filter.MyFilter
import com.uz.sovchi.data.valid
import com.uz.sovchi.databinding.FragmentAuthBinding
import com.uz.sovchi.handleException
import com.uz.sovchi.postVal
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

    private var splash = true


    override fun onDestroyView() {
        savedPhone = phoneText
        super.onDestroyView()
    }

    private var signInRequest: BeginSignInRequest? = null
    private var oneTapClient: SignInClient? = null

    private var isSigning = false

    private val intentSenderLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null) {
                    try {
                        val credential = oneTapClient?.getSignInCredentialFromIntent(data)
                        val idToken = credential?.googleIdToken
                        if (idToken != null) {
                            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                            if (context != null && activity != null) {
                                viewModel.sendState.postVal(SendState.Sending)
                                viewModel.signInGoogle(requireContext(), firebaseCredential) {
                                    if (it) {
                                        verified()
                                    } else {
                                        viewModel.sendState.postVal(SendState.Input())
                                        isSigning = false
                                    }
                                }
                            } else {
                                isSigning = false
                            }
                        }
                    } catch (e: Exception) {
                        // Handle exception
                        handleException(e)
                        isSigning = false
                    }
                } else {
                    isSigning = false
                }
            } else {
                isSigning = false
                // Handle failure or cancellation
                //  showToast("Sign in failed or was cancelled")
            }
        }

    private fun loginGoogle() {
        if (activity == null) return
        if (isSigning) return
        isSigning = true

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
                                if (context != null && activity != null) {
                                    viewModel.sendState.postVal(SendState.Sending)
                                    viewModel.signInGoogle(requireContext(), firebaseCredential) {
                                        if (it) {
                                            verified()
                                        } else {
                                            viewModel.sendState.postVal(SendState.Input())
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            handleException(e)
                        }
                    }
                }
            }

        signInRequest?.let { request ->
            oneTapClient?.beginSignIn(request)?.addOnSuccessListener { result ->
                try {
                    if (isDetached || isRemoving || isVisible.not()) return@addOnSuccessListener
                    intentSenderLauncher.launch(
                        IntentSenderRequest.Builder(result.pendingIntent).build()
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
        viewModel.sendState.postVal(SendState.Sending)
        isSigning = true
        lifecycleScope.launch {
            if (activity == null) {
                isSigning = false
                return@launch
            }
            val user = UserRepository.authFirebaseUser()
            if (isRemoving || user.valid.not()) {
                isSigning = false
                return@launch
            }
            isSigning = false
            closeFragment()
            MyFilter.update()
            isSigning = false
            if (user!!.name.isEmpty()) {
                navigate(R.id.getUserNameFragment)
            } else {
                mainActivity()?.recreateUi(true)
            }
        }
    }

    private fun initGoogleAuth() {
        if (activity == null) return
        oneTapClient = Identity.getSignInClient(requireActivity())
        signInRequest = BeginSignInRequest.builder().setGoogleIdTokenRequestOptions(
            BeginSignInRequest.GoogleIdTokenRequestOptions.builder().setSupported(true)
                .setServerClientId(getString(R.string.your_web_client_id))
                .setFilterByAuthorizedAccounts(false).build()
        ).build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        splash = arguments?.getBoolean("splash", false) ?: false
        initGoogleAuth()
        loginGoogle()
    }

    override fun viewCreated(bind: FragmentAuthBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@AuthFragment)
            toolbar.isVisible = false
            Glide.with(requireContext()).load(R.drawable.splash_photo).into(photo)
            gmailButton.setOnClickListener {
                loginGoogle()
            }
            phoneView.apply {
                editText?.apply {
                    phoneMask = PhoneUtils.phoneMask(this)
                    addTextChangedListener {
                        viewModel.sendState.postVal(SendState.Input(phoneText))
                    }
                    savedPhone?.let { setText(it) }
                }
            }
            skipButton.setOnClickListener {
                navigate(R.id.searchFragment)
            }
            emailButton.setOnClickListener {
                navigate(R.id.loginGmailFragment)
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
                            if (error.startsWith("An internal error")) {
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
                            sendState.postVal(SendState.Input(phoneText))
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
        intentSenderLauncher.unregister()
        PermissionController.getInstance().removeCallback(PermissionController.ANY_REQUEST_CODE)
    }

    companion object {
        const val VERIFY_CODE = "verify_code"
        const val PHONE = "phone"
    }
}