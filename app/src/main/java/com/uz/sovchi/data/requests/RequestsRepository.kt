package com.uz.sovchi.data.requests

import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.messages.RequestModel
import com.uz.sovchi.data.messages.RequestStatus
import com.uz.sovchi.data.nomzod.Nomzod

class RequestsRepository {

    private var requestsCollection = FirebaseFirestore.getInstance().collection("requests")

    var requests = ArrayList<RequestModel>()
    var requestsLive = MutableLiveData(requests)
    var requestsLoading = MutableLiveData(false)

    fun getRequests(my: Boolean, result: (data: ArrayList<RequestModel>) -> Unit) {
        if (requestsLoading.value == true) return
        requestsLoading.postValue(true)
        requests.clear()
        requestsLive.postValue(requests)
        val request = if (my) {
            requestsCollection.whereEqualTo(RequestModel::requestedUserId.name, LocalUser.user.uid)
        } else {
            requestsCollection.whereEqualTo(RequestModel::nomzodUserId.name, LocalUser.user.uid)
        }
        request.orderBy(RequestModel::id.name, Query.Direction.DESCENDING).get()
            .addOnCompleteListener {
                requests.clear()
                requests.addAll(it.result.toObjects(RequestModel::class.java))
                requestsLive.postValue(requests)
                result.invoke(requests)
                requestsLoading.postValue(false)
            }
    }

    fun getMyRequest(nomzodId: String, result: (data: RequestModel?) -> Unit) {
        val request = requests.firstOrNull { it.nomzodId == nomzodId }
        if (request != null) {
            result.invoke(request)
            return
        }
        requestsCollection.whereEqualTo(RequestModel::nomzodId.name, nomzodId)
            .whereEqualTo(RequestModel::requestedUserId.name, LocalUser.user.uid).get()
            .addOnCompleteListener {
                val first = it.result.toObjects(RequestModel::class.java).firstOrNull()
                result.invoke(first)
            }
    }

    fun updateRequestStatus(requestId: String, requestStatus: Int) {
        requestsCollection.document(requestId).update(RequestModel::status.name, requestStatus)
        requests.find { it.id == requestId }?.status = requestStatus
    }

    fun sendRequest(
        myNomzod: Nomzod, nomzod: Nomzod, result: (send: Boolean, alreadySend: Boolean) -> Unit
    ) {
        getMyRequest(nomzod.id) {
            if (it != null) {
                result.invoke(false, true)
                return@getMyRequest
            }
            val sendRequest = RequestModel(
                id = System.currentTimeMillis().toString(),
                LocalUser.user.uid,
                myNomzod,
                nomzod,
                nomzod.userId,
                nomzod.id,
                RequestStatus.requested
            )
            requestsCollection.document(sendRequest.id).set(sendRequest)
                .addOnCompleteListener { task ->
                    result.invoke(task.isSuccessful, false)
                }
        }
    }
}