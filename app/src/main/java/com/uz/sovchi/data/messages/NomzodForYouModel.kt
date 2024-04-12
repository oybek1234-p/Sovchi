package com.uz.sovchi.data.messages

data class NomzodForYouModel(
    var nomzodId: String?, var title: String?, var body: String?
) {
    constructor(): this(null,null,null)
}