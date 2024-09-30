package com.uz.sovchi.data.payment

import com.google.firebase.database.FirebaseDatabase

object PaymentInfo {

    private val reference = FirebaseDatabase.getInstance().getReference("paymentCard")

    fun loadPaymentCard(result: (card: String) -> Unit) {
        reference.get().addOnSuccessListener {
            val card = it.value?.toString()
            result.invoke(card ?: "")
        }.addOnFailureListener {
            result.invoke("5614688707870480")
        }
    }
}