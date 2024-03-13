package com.uz.sovchi.ui.nomzod

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.uz.sovchi.PhoneUtils
import com.uz.sovchi.R
import com.uz.sovchi.SocialMedia
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.viewed.ViewedNomzods
import com.uz.sovchi.databinding.NomzodDetailsBinding
import com.uz.sovchi.gson
import com.uz.sovchi.openPhoneCall
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.visibleOrGone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NomzodDetailsFragment : BaseFragment<NomzodDetailsBinding>() {

    override val layId: Int
        get() = R.layout.nomzod_details

    private var nomzod: Nomzod? = null

    companion object {
        fun navigateToHere(fragment: BaseFragment<*>, nomzod: Nomzod) {
            val json = gson!!.toJson(nomzod)
            val bundle = Bundle().apply {
                putString("data", json)
            }
            fragment.navigate(R.id.nomzodDetailsFragment, bundle)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val json = arguments?.getString("data")
        if (json.isNullOrEmpty().not()) {
            nomzod = gson!!.fromJson(json!!, Nomzod::class.java)
            lifecycleScope.launch(Dispatchers.IO) {
                ViewedNomzods.setViewed(nomzod!!.id)
            }
        }
    }

    override fun viewCreated(bind: NomzodDetailsBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@NomzodDetailsFragment)
            if (nomzod == null) return
            nomzodView.setNomzod(nomzod!!, true)
            val joyOdam = nomzod!!.joylaganOdam
            if (joyOdam.isEmpty()) {
                joylaganOdamView.visibleOrGone(false)
            } else {
                joylaganOdamView.text =
                    "${getString(R.string.joylagan_odam)} - ${nomzod!!.joylaganOdam}"
            }
            nomzod!!.apply {
                if (mobilRaqam.length < 8) {
                    callButton.visibleOrGone(false)
                } else {
                    callView.text = "${getString(R.string.telefon_qilish)} ${
                        PhoneUtils.formatPhoneNumber(mobilRaqam)
                    }"
                }
                val telegramUserName = SocialMedia.telegramUserName(telegramLink)
                if (telegramUserName.isEmpty()) {
                    telegramButton.visibleOrGone(false)
                } else {
                    telegramView.text =
                        "${getString(R.string.telegram)} $telegramUserName"
                }
            }
            callButton.setOnClickListener {
                openPhoneCall(requireActivity(), nomzod!!.mobilRaqam)
            }
            telegramButton.setOnClickListener {
                SocialMedia.openLink(
                    requireActivity(),
                    SocialMedia.parseTelegramLink(nomzod!!.telegramLink)
                )
            }
        }
        loadAd()
    }

    private fun loadAd() {
        binding?.adView?.apply {
            val adRequest = AdRequest.Builder().build()
            loadAd(adRequest)
        }
    }
}