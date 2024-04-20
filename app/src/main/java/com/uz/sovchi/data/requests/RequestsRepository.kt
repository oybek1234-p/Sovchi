package com.uz.sovchi.data.requests

import com.google.firebase.firestore.FirebaseFirestore
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.messages.RequestModel
import com.uz.sovchi.data.messages.RequestStatus
import com.uz.sovchi.data.nomzod.Nomzod

class RequestsRepository {

    private var requestsCollection = FirebaseFirestore.getInstance().collection("requests")

    fun getReceivedRequest(result: (List<RequestModel>) -> Unit) {

    }

    fun getSentRequests(result: (List<RequestModel>) -> Unit) {

    }

    private fun getRequest(userId: String, result: (data: RequestModel?) -> Unit) {
        requestsCollection.whereEqualTo(RequestModel::userId.name, userId).get()
            .addOnCompleteListener {
                val first = it.result.toObjects(RequestModel::class.java).firstOrNull()
                result.invoke(first)
            }
    }

    fun sendRequest(nomzod: Nomzod, result: (send: Boolean, alreadySend: Boolean) -> Unit) {
        getRequest(nomzod.userId) {
            if (it == null) {
                val sendRequest = RequestModel(
                    id = System.nanoTime().toString(),
                    LocalUser.user.uid,
                    nomzod.userId,
                    RequestStatus.Requested.code,
                    nomzod.name,
                    nomzodId = nomzod.id,
                    System.currentTimeMillis()
                )
                requestsCollection.document(sendRequest.id).set(sendRequest)
                    .addOnCompleteListener { task ->
                        result.invoke(task.isSuccessful, false)
                    }
            } else {
                result.invoke(false, true)
            }
        }
    }
}