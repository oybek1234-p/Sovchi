package com.uz.sovchi.data

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.uz.sovchi.PhoneUtils
import com.uz.sovchi.showToast
import java.util.concurrent.TimeUnit

object AuthController {

    private const val LANGUAGE_CODE = "uz"

    fun verifyCode(
        code: String,
        verificationCode: String,
        done: (success: Boolean, exception: Exception?) -> Unit
    ) {
        val credential = PhoneAuthProvider.getCredential(verificationCode, code)
//        FirebaseAuth.getInstance().signInWithCredential(credential).addOnSuccessListener {
//            done.invoke(it.isSuccessful, it.exception)
//        }
    }

    fun sendSms(
        activity: Activity,
        phone: String,
        result: (success: Boolean, verificationCode: String?, exception: FirebaseException?) -> Unit
    ) {
        val number = PhoneUtils.formatPhoneNumber(phone)
        FirebaseAuth.getInstance().setLanguageCode(LANGUAGE_CODE)
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(number)
            .setTimeout(60, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                    showToast("Verification complete!")
                }

                override fun onVerificationFailed(p0: FirebaseException) {
                    result.invoke(false, null, p0)
                }

                override fun onCodeSent(p0: String, p1: PhoneAuthProvider.ForceResendingToken) {
                    super.onCodeSent(p0, p1)
                    result.invoke(true, p0, null)
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

}