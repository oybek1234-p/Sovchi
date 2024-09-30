package com.uz.sovchi.ui.photo

import com.uz.sovchi.R
import com.uz.sovchi.databinding.GoodBadPhotoBinding
import com.uz.sovchi.loadPhoto
import com.uz.sovchi.ui.base.BaseFragment

class GoodBadPhotoFragment(override val layId: Int = R.layout.good_bad_photo) :
    BaseFragment<GoodBadPhotoBinding>() {
    override fun viewCreated(bind: GoodBadPhotoBinding) {
        bind.toolbar.setUpBackButton(this)
        bind.apply {
            ph1.loadPhoto("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRgOJQEN3sLA457ve43Eod1_9Dn4G6T7M_Tww&s")
            ph3.loadPhoto("https://d2az3zd39o5d63.cloudfront.net/tinder-pics-smile.jpg")
            ph2.loadPhoto("https://miro.medium.com/v2/resize:fit:1400/1*gpQ6exwlDCCpvQgQeyv4uw.png")
            ph4.loadPhoto("https://d2az3zd39o5d63.cloudfront.net/tinder-pics-animal.jpg")
        }
    }
}