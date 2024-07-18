package com.uz.sovchi.data.like

import com.uz.sovchi.data.nomzod.Nomzod

data class LikeModelFull(
    var id: String,
    val userId: String,
    val userName: String,
    val hasNomzod: Boolean,
    val userPhoto: String,
    val nomzodId: String,
    val nomzodUserId: String,
    val nomzod: Nomzod?,
    val likedUserNomzod: Nomzod?,
    val likeState: Int,
    val matched: Boolean,
    val date: String
) {
    constructor() : this("", "", "", false, "", "", "", null, null, 0, false, "")

}