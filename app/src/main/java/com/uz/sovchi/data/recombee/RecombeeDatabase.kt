package com.uz.sovchi.data.recombee

import com.google.firebase.functions.FirebaseFunctions
import com.recombee.api_client.RecombeeClient
import com.recombee.api_client.api_requests.AddBookmark
import com.recombee.api_client.api_requests.AddDetailView
import com.recombee.api_client.api_requests.AddPurchase
import com.recombee.api_client.api_requests.DeleteBookmark
import com.recombee.api_client.api_requests.DeletePurchase
import com.recombee.api_client.api_requests.RecommendItemsToItem
import com.recombee.api_client.util.Region
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.filter.MyFilter
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.NomzodRepository
import com.uz.sovchi.data.nomzod.OilaviyHolati
import com.uz.sovchi.gson
import com.uz.sovchi.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object RecombeeDatabase {

    private val client = RecombeeClient(
        "yor-yor-dev", "RC6GCEOxqJG9cKwIqMDfyxXLX65RNFdoN3Mz19TjJqyqhtXFuQrdsXjRKPXkc5IR"
    ).apply {
        setRegion(Region.EU_WEST)
    }

    private val scope = CoroutineScope(SupervisorJob())

    fun getSimilarNomzods(nomzodId: String, count: Int, result: (list: List<Nomzod>) -> Unit) {
        try {
            scope.launch {
                try {
                    val filter = buildFilter(
                        MyFilter.filter.nomzodType,
                        "",
                        "",
                        MyFilter.filter.oilaviyHolati,
                        MyFilter.filter.yoshChegarasiGacha,
                        MyFilter.filter.yoshChegarasiDan
                    )
                    val list = arrayListOf<Nomzod>()
                    val response = client.send(
                        RecommendItemsToItem(
                            nomzodId, null, count.toLong()
                        ).setReturnProperties(true).setFilter("$filter AND 'nid' != $nomzodId")
                    )
                    response.forEach { it ->
                        val rec = it.values
                        rec.toNomzod()?.let {
                            list.add(it)
                        }
                    }
                    result.invoke(list)
                } catch (e: Exception) {
                    //
                }
            }
        } catch (e: Exception) {
            //
        }
    }

    private fun buildFilter(
        type: Int,
        manzil: String,
        userId: String,
        oilaviyHolati: String,
        yoshChegarasiGacha: Int = 0,
        yoshChegarasiDan: Int = 0,
        includeDisliked: Boolean = true,
        hasPhotoOnly: Boolean = false
    ): String {
        var filter = "'type' == $type"
        if (manzil.isNotEmpty() && manzil != City.Hammasi.name) {
            filter += " AND 'manzil' == \"$manzil\""
        }
        if (oilaviyHolati.isNotEmpty() && oilaviyHolati != OilaviyHolati.Aralash.name) {
            filter += " AND 'oilaviyHolati' == \"$oilaviyHolati\""
        }
        if (yoshChegarasiGacha > MyFilter.AGE_MIN) {
            filter += " AND '${Nomzod::tugilganYili.name}' <= $yoshChegarasiGacha"
        }
        if (hasPhotoOnly) {
            filter += " AND size('photos') > 0"
        }
        if (yoshChegarasiDan > MyFilter.AGE_MIN) {
            filter += " AND '${Nomzod::tugilganYili.name}' >= $yoshChegarasiDan"
        }
//        val disliked = LikeController.likes.map { it.nomzodId }
//        if (includeDisliked && disliked.isNotEmpty()) {
//            val dislikedIds = disliked.map { "\"${it}\"" }.joinToString(",") { it }
//
//            filter += " AND 'itemId' not in {${dislikedIds}}"
//        }
        return filter
    }

    fun deleteSaved(id: String) {
        try {
            client.send(DeleteBookmark(LocalUser.user.uid, id))
        } catch (e: Exception) {
            //
        }
    }

    fun setSaved(id: String) {
        try {
            client.send(AddBookmark(LocalUser.user.uid, id))
        } catch (e: Exception) {
            //
        }
    }

    fun deleteFromPurchase(id: String) {
        try {
            client.send(DeletePurchase(LocalUser.user.uid, id))
        } catch (e: Exception) {
            //
        }
    }

    private val likedModels = arrayListOf<String>()

    private fun getLikedModelsFor(nomzods: List<Nomzod>, result: (list: List<Nomzod>?) -> Unit) {

    }

    private var firebaseFunctions = FirebaseFunctions.getInstance()

    private fun getNomzods(
        recomId: String = "",
        type: Int = 0,
        manzil: String = "",
        userId: String = "",
        oilaviyHolati: String = "",
        yoshChegarasiGacha: Int = 0,
        yoshChegarasiDan: Int = 0,
        limit: Int = 6,
        result: (
            recomId: String, list: List<Nomzod>
        ) -> Unit
    ) {
        val data = hashMapOf<String, Any>(
            "type" to type,
            "manzil" to manzil,
            "userId" to userId,
            "recomId" to recomId,
            "oilaviyHolati" to oilaviyHolati,
            "yoshChegarasiGacha" to yoshChegarasiGacha,
            "yoshChegarasiDan" to yoshChegarasiDan,
            "limit" to limit
        )

        firebaseFunctions.getHttpsCallable("getNomzods").call(data).continueWith {
            if (it.result.data != null) {
                val response = gson!!.fromJson(
                    it.result.data.toString(), RecommendationModel::class.java
                )
                result.invoke(response.recommId, response.recomms.map { it.values }.also {
                    it.forEach {
                        NomzodRepository.cacheNomzods.put(it.id, it)
                    }
                })
            } else {
                showToast("Data null")
            }
        }
    }

    fun getRecommendForUser(
        recomId: String,
        type: Int,
        manzil: String,
        userId: String,
        oilaviyHolati: String,
        yoshChegarasiGacha: Int = 0,
        yoshChegarasiDan: Int = 0,
        limit: Int = 6,
        result: (
            recomId: String, list: List<Nomzod>
        ) -> Unit
    ) {
        scope.launch {
            getNomzods(
                recomId,
                type,
                manzil,
                userId,
                oilaviyHolati,
                yoshChegarasiGacha,
                yoshChegarasiDan,
                limit,
                result
            )
        }
    }

    private fun Map<String, Any>.toNomzod(): Nomzod? {
        return try {
            val json = gson!!.toJson(this)
            val model = gson!!.fromJson(json, Nomzod::class.java)
            model.id = this["nid"].toString()
            model.userId = this["userUid"].toString()
            model
        } catch (e: Exception) {
            null
        }
    }

    fun setConnectedToNomzod(userId: String, nomzodId: String) {
        if (userId.isEmpty()) return
        scope.launch {
            try {
                val detailView = AddPurchase(userId, nomzodId)
                client.send(detailView)
            } catch (e: Exception) {
                //
            }
        }
    }

    fun setNomzodViewed(userId: String, nomzodId: String) {
        if (userId.isEmpty()) return
        scope.launch {
            try {
                val detailView = AddDetailView(userId, nomzodId)
                client.send(detailView)
            } catch (e: Exception) {
                //
            }
        }
    }
}