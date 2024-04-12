package com.uz.sovchi.data.recombee

import com.recombee.api_client.RecombeeClient
import com.recombee.api_client.api_requests.AddDetailView
import com.recombee.api_client.api_requests.AddPurchase
import com.recombee.api_client.api_requests.RecommendItemsToItem
import com.recombee.api_client.api_requests.RecommendItemsToUser
import com.recombee.api_client.api_requests.RecommendNextItems
import com.recombee.api_client.util.Region
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.filter.MyFilter
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.nomzod.Nomzod
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

    const val randomBooster1 = "if 'date' >= now() - 24 * 60 * 60 > 0 then 1.5\n" +
            "  else if 'date' >= now() - 7 * 24 * 60 * 60 then 1.3\n" +
            "  else random()"

    fun getBooster()  = listOf(randomBooster1).random()

    fun getSimilarNomzods(nomzodId: String, count: Int, result: (list: List<Nomzod>) -> Unit) {
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
                    ).setReturnProperties(true).setFilter("$filter AND 'nid' != $nomzodId").setBooster(getBooster())
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
    }

    private fun buildFilter(
        type: Int,
        manzil: String,
        userId: String,
        oilaviyHolati: String,
        yoshChegarasiGacha: Int = 0,
        yoshChegarasiDan: Int = 0
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
        if (yoshChegarasiDan > MyFilter.AGE_MIN) {
            filter += " AND '${Nomzod::tugilganYili.name}' >= $yoshChegarasiDan"
        }
        return filter
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
        result: (recomId: String, list: List<Nomzod>) -> Unit
    ) {
        scope.launch {
            try {
                val response = if (recomId.isEmpty()) {
                    var req = RecommendItemsToUser(userId.ifEmpty { null }, limit.toLong()).setReturnProperties(true)
                    val filter = buildFilter(
                        type, manzil, userId, oilaviyHolati, yoshChegarasiGacha, yoshChegarasiDan
                    )
                    if (filter.isNotEmpty()) {
                        req = req.setFilter(filter)
                    }
                    client.send(req.setScenario("homePage").setBooster(getBooster()))
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
            } catch (e: Exception) {
                //
                showToast(e.message.toString())
            }
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