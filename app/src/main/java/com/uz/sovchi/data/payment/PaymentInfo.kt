package com.uz.sovchi.data.payment

import com.google.firebase.database.FirebaseDatabase
object PaymentInfo {

    private val reference = FirebaseDatabase.getInstance().getReference("paymentCard")
    private val tariffsReference = FirebaseDatabase.getInstance().getReference("tarifs")

    fun loadPaymentCard(result: (card: String) -> Unit) {
        reference.get().addOnCompleteListener {
            val card = it.result.value?.toString()
            result.invoke(card ?: "")
        }
    }

//    fun setTarifs() {
//        val listTarifs = listOf(
//            NomzodTarifModel("Standart", 0, ""), NomzodTarifModel("Top 3", 19000, ""),
//            NomzodTarifModel("Top 7",29000,""),NomzodTarifModel("Top 14",39000,"")
//
//        )
//        tariffsReference.setValue(listTarifs)
//    }
//
//    fun loadTarifs(result: (tarifs: List<NomzodTarifModel>) -> Unit) {
//        try {
//            tariffsReference.get().addOnCompleteListener { it ->
//                val tariffs =
//                    it.result.children.mapNotNull { it.getValue(NomzodTarifModel::class.java) }
//                result.invoke(tariffs)
//            }
//        } catch (e: Exception) {
//            //
//        }
//    }
}