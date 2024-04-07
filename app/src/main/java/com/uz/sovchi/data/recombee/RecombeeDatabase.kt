package com.uz.sovchi.data.recombee

import com.recombee.api_client.RecombeeClient
import com.recombee.api_client.api_requests.AddDetailView
import com.recombee.api_client.api_requests.AddPurchase
import com.recombee.api_client.api_requests.RecommendItemsToUser
import com.recombee.api_client.api_requests.RecommendNextItems
import com.recombee.api_client.util.Region
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.OilaviyHolati
import com.uz.sovchi.gson
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

    fun getRecommendForUser(
        recomId: String,
        type: Int,
        manzil: String,
        userId: String,
        oilaviyHolati: String,
        yoshChegarasiGacha: Int = 0,
        yoshChegarasiDan: Int = 0,
        limit: Int = 6,
        result: (recomId: String, list: List<Nomzod>) -> Unit
    ) {
        scope.launch {
            val response = if (recomId.isEmpty()) {
                var filter = "'type' == $type"
                if (manzil.isNotEmpty() && manzil != City.Hammasi.name) {
                    filter += " AND 'manzil' == \"$manzil\""
                }
                if (oilaviyHolati.isNotEmpty() && oilaviyHolati != OilaviyHolati.Aralash.name) {
                    filter += " AND 'oilaviyHolati' == \"$oilaviyHolati\""
                }
                if (yoshChegarasiGacha > 17) {
                    filter += " AND '${Nomzod::yoshChegarasiGacha.name}' <= $yoshChegarasiGacha"
                }
                if (yoshChegarasiDan < 17) {
                    filter += " AND '${Nomzod::yoshChegarasiDan.name}' >= $yoshChegarasiDan"
                }
                var req = RecommendItemsToUser(userId, limit.toLong()).setReturnProperties(true)

                if (filter.isNotEmpty()) {
                    req = req.setFilter(filter)
                }
                client.send(req)
            } else {
                client.send(RecommendNextItems(recomId, limit.toLong()))
            }
            val list = arrayListOf<Nomzod>()

            response.forEach { it ->
                val rec = it.values
                rec.toNomzod()?.let {
                    list.add(it)
                }
            }
            result.invoke(response.recommId, list)
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

    fun setNomzodProfileViewed(userId: String, nomzodId: String) {
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