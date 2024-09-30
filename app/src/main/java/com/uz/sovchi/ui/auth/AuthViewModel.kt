package com.uz.sovchi.ui.auth

import android.app.Activity
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.uz.sovchi.R
import com.uz.sovchi.data.AuthController
import com.uz.sovchi.postVal

sealed class SendState {
    data class Input(val input: String? = null) : SendState()
    data object Sending : SendState()
    data class Success(val code: String?) : SendState()
    data class Error(val exception: Exception) : SendState()
}

data class InputCode(val code: String?) {
    fun isValid() = code?.length == 6
}

data class SendInfo(val code: String, val phone: String)

sealed class VerifyState {
    data class Input(val input: InputCode? = null) : VerifyState()
    data object Verifying : VerifyState()
    data object Success : VerifyState()
    data class Error(val exception: Exception) : VerifyState()
}

class AuthViewModel : ViewModel() {

    val sendState = MutableLiveData<SendState>(SendState.Input(""))
    val verifyState = MutableLiveData<VerifyState>(VerifyState.Input(null))

    fun sendCode(activity: Activity, phone: String) {
        if (sendState.value is SendState.Sending) return
        sendState.postVal(SendState.Sending)

        AuthController.sendSms(activity, phone) { success, verificationCode, exception ->
            if (exception != null) {
                val message = when (exception) {
                    is FirebaseAuthInvalidCredentialsException -> activity.getString(R.string.parol_noturgri)
                    is FirebaseTooManyRequestsException -> activity.getString(R.string.keyinroq_urinib_koring)
                    else -> exception.message
                }
                sendState.postVal(SendState.Error(java.lang.Exception(message)))
            } else {
                sendState.postVal(SendState.Success(verificationCode))
            }
        }
    }

    fun signInGoogle(context: Context, credential: AuthCredential, verified: (done: Boolean) -> Unit) {
        verifyState.postVal(VerifyState.Verifying)
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnSuccessListener {
            verified.invoke(true)
        }.addOnFailureListener {
            verified.invoke(false)
        }
    }

    fun verifyCode(context: Context, code: String, verificationCode: String) {
        verifyState.postVal(VerifyState.Verifying)
        try {
            AuthController.verifyCode(code, verificationCode) { success, exception ->
                if (success) {
                    verifyState.postVal(VerifyState.Success)
                } else {
                    if (exception != null) {
                        val message = when (exception) {
                            is FirebaseAuthInvalidCredentialsException -> context.getString(R.string.parol_noturgri)
                            is FirebaseTooManyRequestsException -> context.getString(R.string.keyinroq_urinib_koring)
                            else -> context.getString(R.string.xatolik_yuz_berdi)
                        }
                        verifyState.postVal(VerifyState.Error(java.lang.Exception(message)))
                    }
                }
            }
        } catch (e: Exception) {
            //
        }
    }

    val autoCodeReceiverObserver = MutableLiveData<String>()
    private var smsRetriever: SmsRetriever? = null

    fun stopAutoCodeReceiver() {
        smsRetriever?.unregister()
    }

    fun startAutoCodeReceiver(context: Context) {
        smsRetriever = SmsRetriever(context) { code, errorMessage ->
            if (errorMessage == null && code.isNotEmpty()) {
                autoCodeReceiverObserver.postVal(code)
            }
        }.apply {
            start()
        }
    }
}
