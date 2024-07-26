package com.uz.sovchi.data.verify

data class VerificationData(
    var passportPhoto: String?, var selfiePhoto: String?, var divorcePhoto: String?
) {
    constructor() : this(null, null, null)
}