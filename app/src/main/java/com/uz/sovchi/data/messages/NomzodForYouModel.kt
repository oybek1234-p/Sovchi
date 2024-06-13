package com.uz.sovchi.data.messages

data class NomzodForYouModel(
    var nomzodId: String?, var nomzodName: String?, var nomzodAge: Int?,
    val showPhoto: Boolean?, val photo: String?
) {
    constructor(): this(null,null,null,null,null)
}