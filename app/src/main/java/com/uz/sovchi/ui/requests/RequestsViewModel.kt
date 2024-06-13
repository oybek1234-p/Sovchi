package com.uz.sovchi.ui.requests

import androidx.lifecycle.ViewModel
import com.uz.sovchi.data.messages.RequestModel
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.requests.RequestsRepository

class RequestsViewModel : ViewModel() {

    var requestsRepository = RequestsRepository()

    val requests = requestsRepository.requests
    val requestsLive = requestsRepository.requestsLive
    val requestsLoading = requestsRepository.requestsLoading

    fun isRequested(nomzodId: String, result: (isRequested: Boolean) -> Unit) {
        getRequest(nomzodId) {
            result.invoke(it != null)
        }
    }

    fun getRequests(my: Boolean) {
        requestsRepository.getRequests(my) {}
    }

    fun sendRequest(
        myNomzod: Nomzod,
        nomzod: Nomzod,
        result: (send: Boolean, alreadySend: Boolean) -> Unit
    ) {
        requestsRepository.sendRequest(myNomzod, nomzod, result)
    }

    fun getRequest(nomzodId: String, result: (data: RequestModel?) -> Unit) {
        requestsRepository.getMyRequest(nomzodId, result)
    }
}