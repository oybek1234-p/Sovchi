package com.uz.sovchi.ui.matched

import android.os.Bundle
import com.uz.sovchi.R
import com.uz.sovchi.data.nomzod.KELIN
import com.uz.sovchi.data.nomzod.KUYOV
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.NomzodRepository
import com.uz.sovchi.databinding.MatchedFragmentBinding
import com.uz.sovchi.loadPhoto
import com.uz.sovchi.ui.base.BaseFragment

class MatchedFragment(override val layId: Int = R.layout.matched_fragment) :
    BaseFragment<MatchedFragmentBinding>() {

    private var nomzodId = ""
    private var nomzodPhoto = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            nomzodId = it.getString("nomzodId", "")
            nomzodPhoto = it.getString("nomzodPhoto", "")
        }
    }

    override fun viewCreated(bind: MatchedFragmentBinding) {
        bind.apply {
            backButton.setOnClickListener {
                closeFragment()
            }
            myPhoto.loadPhoto((MyNomzodController.nomzod.photos.firstOrNull() ?: "").ifEmpty {
                if (MyNomzodController.nomzod.type == KUYOV) Nomzod.KUYOV_TEXT else Nomzod.KELIN_TEXT
            })
            userPhoto.loadPhoto(nomzodPhoto.ifEmpty {
                if (MyNomzodController.nomzod.type == KELIN) Nomzod.KUYOV_TEXT else Nomzod.KELIN_TEXT
            })
            chatButton.setOnClickListener {
                navigate(R.id.chatMessageFragment, Bundle().apply {
                    putString("id", nomzodId)
                    val nomzod = NomzodRepository.cacheNomzods[nomzodId]
                    if (nomzod != null) {
                        putString("name", nomzod.name)
                        putString("photo", nomzod.photos.firstOrNull() ?: "")
                    }
                })
            }
        }
    }
}