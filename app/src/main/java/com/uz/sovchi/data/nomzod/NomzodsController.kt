package com.uz.sovchi.data.nomzod

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

object NomzodsController {

    val nomzodlarReference: CollectionReference by lazy {
        FirebaseFirestore.getInstance().collection("nomzodlar")
    }

}