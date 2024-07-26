package com.uz.sovchi.data.messages

data class NomzodLikedModel(
    var nomzodId: String,
    var likedUserName: String,
    var likedUserId: String,
    var hasNomzod: Boolean,
    var liked: Boolean,
    var photo: String
) {
    constructor() : this("", "", "", false,true, "")
}

data class NomzodRequestModel(
    var nomzodId: String, var likedUserName: String, var likedUserId: String, var hasNomzod: Boolean
) {
    constructor() : this("", "", "", false)
}