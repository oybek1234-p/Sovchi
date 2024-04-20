package com.uz.sovchi.data.messages

data class NomzodLikedModel(
    var nomzodId: String,
    var likedUserName: String,
    var likedUserId: String
) {
    constructor() : this("", "", "")
}