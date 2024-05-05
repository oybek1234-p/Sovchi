package com.uz.sovchi.data.payment

import com.google.firebase.database.FirebaseDatabase

object PaymentInfo {

    private val reference = FirebaseDatabase.getInstance().getReference("paymentCard")

    fun loadPaymentCard(result: (card: String) -> Unit) {
        reference.get().addOnCompleteListener {
            val card = it.result.value?.toString()
            result.invoke(card ?: "")
        }
    }
}