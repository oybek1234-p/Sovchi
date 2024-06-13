package com.uz.sovchi.data.recombee

import com.google.gson.annotations.SerializedName
import com.uz.sovchi.data.nomzod.Nomzod

data class RecommendationModel(
    @SerializedName("recommId") val recommId: String,
    @SerializedName("recomms") val recomms: List<RecommModel>
)

data class RecommModel(
    @SerializedName("id") val id: String,
    @SerializedName("values") val values: Nomzod
)

