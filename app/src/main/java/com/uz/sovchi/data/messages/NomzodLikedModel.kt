package com.uz.sovchi.data.messages

data class NomzodLikedModel(
    var nomzodId: String,
    var likedUserName: String,
    var likedUserId: String,
    var hasNomzod: Boolean,
    var photo: String
) {
    constructor() : this("", "", "", false, "")
}

data class NomzodRequestModel(
    var nomzodId: String, var likedUserName: String, var likedUserId: String, var hasNomzod: Boolean
) {
    constructor() : this("", "", "", false)
}