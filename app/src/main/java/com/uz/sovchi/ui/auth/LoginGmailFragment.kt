package com.uz.sovchi.ui.auth

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.data.UserRepository
import com.uz.sovchi.data.filter.MyFilter
import com.uz.sovchi.data.valid
import com.uz.sovchi.databinding.LoginGmailFragmentBinding
import com.uz.sovchi.postVal
import com.uz.sovchi.showKeyboard
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseFragment
import kotlinx.coroutines.launch

class LoginGmailFragment(override val layId: Int = R.layout.login_gmail_fragment) :
    BaseFragment<LoginGmailFragmentBinding>() {

    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    private var loading = MutableLiveData(false)

    override fun onResume() {
        super.onResume()
        binding?.gmailView?.editText?.showKeyboard()
    }

    private fun signInUser(email: String, password: String) {
        if (loading.value == true) return
        loading.postVal(true)
        auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
            signedIn()
        }.addOnFailureListener {
            when (it) {
                is FirebaseAuthInvalidUserException -> {
                    createNewUser(email, password)
                }

                is FirebaseAuthInvalidCredentialsException -> {
                    showToast(appContext.getString(R.string.email_or_pass_not_valid))
                }

                is FirebaseNetworkException -> {
                    showToast(appContext.getString(R.string.internet_invalid))
                }

                is FirebaseTooManyRequestsException -> {
                    showToast(appContext.getString(R.string.retry_later))
                }

                else -> {
                    showToast(appContext.getString(R.string.biz_bilan_bog_lanish))
                    binding?.boglanishButton?.isVisible = true
                }
            }
            loading.postVal(false)
        }
    }

    private fun createNewUser(email: String, password: String) {
        loading.postVal(true)

        auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
            loading.postVal(false)
            signedIn()
        }.addOnFailureListener {
            loading.postVal(false)
            when (it) {
                is FirebaseAuthWeakPasswordException -> {
                    showToast(appContext.getString(R.string.weak_password))
                }

                is FirebaseAuthInvalidUserException -> {
                    showToast(appContext.getString(R.string.email_or_pass_not_valid))
                }

                is FirebaseAuthInvalidCredentialsException -> {
                    showToast(appContext.getString(R.string.email_or_pass_not_valid))
                }

                is FirebaseNetworkException -> {
                    showToast(appContext.getString(R.string.internet_invalid))
                }

                is FirebaseTooManyRequestsException -> {
                    showToast(appContext.getString(R.string.retry_later))
                }

                else -> {
                    showToast(appContext.getString(R.string.biz_bilan_bog_lanish))
                    binding?.boglanishButton?.isVisible = true
                }
            }
        }
    }

    private fun signedIn() {
        loading.postVal(true)
        lifecycleScope.launch {
            if (activity == null) return@launch
            val user = UserRepository.authFirebaseUser()
            loading.postVal(false)
            if (user.valid.not()) return@launch
            closeFragment()
            MyFilter.update()
            if (user!!.name.isEmpty()) {
                navigate(R.id.getUserNameFragment)
            } else {
                mainActivity()?.recreateUi(true)
            }
        }
    }

    override fun viewCreated(bind: LoginGmailFragmentBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@LoginGmailFragment)

            loading.observe(viewLifecycleOwner) {
                continueButton.isEnabled = it.not()
                gmailView.isEnabled = it.not()
                passwordView.isEnabled = it.not()
                progressBar.isVisible = it
            }
            boglanishButton.setOnClickListener {
                mainActivity()?.showSupportSheet()
            }
            binding?.boglanishButton?.isVisible = false
            continueButton.setOnClickListener {
                val email = gmailView.editText?.text?.toString()
                val password = passwordView.editText?.text?.toString()
                if (email.isNullOrEmpty()) {
                    showToast(getString(R.string.insert_email))
                    return@setOnClickListener
                }
                if (password.isNullOrEmpty()) {
                    showToast(getString(R.string.insert_password))
                    return@setOnClickListener
                }
                if (password.length < 6) {
                    showToast(getString(R.string.invalid_password))
                    return@setOnClickListener
                }
                signInUser(email, password)
            }
        }
    }

}