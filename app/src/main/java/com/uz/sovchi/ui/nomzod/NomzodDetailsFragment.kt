package com.uz.sovchi.ui.nomzod

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.ads.AdRequest
import com.uz.sovchi.DateUtils
import com.uz.sovchi.R
import com.uz.sovchi.SocialMedia
import com.uz.sovchi.appContext
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.nomzod.KUYOV
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.OilaviyHolati
import com.uz.sovchi.data.nomzod.OqishMalumoti
import com.uz.sovchi.data.nomzod.Talablar
import com.uz.sovchi.data.nomzod.paramsText
import com.uz.sovchi.data.valid
import com.uz.sovchi.data.viewed.ViewedNomzods
import com.uz.sovchi.databinding.NomzodDetailsBinding
import com.uz.sovchi.databinding.RequestNomzodAlertBinding
import com.uz.sovchi.gson
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.photo.PhotoAdapter
import com.uz.sovchi.ui.photo.PickPhotoFragment
import com.uz.sovchi.visibleOrGone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


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

    private val photosAdapter: PhotoAdapter by lazy {
        PhotoAdapter { _, pos, model ->

        }
    }

    private fun showPhotos() {
        if (nomzod == null) return
        if (nomzod?.photos.isNullOrEmpty()) return
        binding?.apply {
            photoCountView.isVisible = true

            photoPager.apply {
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        photoCountView.text = "${position + 1}/${nomzod?.photos?.size ?: 0}"
                    }
                })
                isVisible = true
                adapter = photosAdapter.apply {
                    deleteShown = false
                    matchParent = true
                    submitList(nomzod!!.photos.map { PickPhotoFragment.Image(it) })
                }
            }
        }
    }

    private fun share(url: String, text: String) {
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        Glide.with(appContext).asFile().load(url).into(object : SimpleTarget<File>() {
            override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                lifecycleScope.launch {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_TEXT, text)
                        putExtra(Intent.EXTRA_STREAM, resource.toUri())
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    val packageManager = requireActivity().packageManager
                    if (shareIntent.resolveActivity(packageManager) != null) {
                        requireContext().startActivity(
                            Intent.createChooser(
                                shareIntent,
                                "Share with"
                            )
                        )
                    } else {
                        Toast.makeText(
                            context,
                            "No suitable apps to share content!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    override fun viewCreated(bind: NomzodDetailsBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@NomzodDetailsFragment)
            if (nomzod == null) return
            nomzod!!.apply {
                setView(bind, this)
                toolbar.title =
                    if (type == KUYOV) getString(R.string.kuyovlikga) else getString(R.string.kelinlikga) + "likga nomzod"
//                if (mobilRaqam.length < 8) {
//                    callButton.visibleOrGone(false)
//                } else {
//                    callButton.text = "${getString(R.string.telefon_qilish)} ${
//                        PhoneUtils.formatPhoneNumber(mobilRaqam)
//                    }"
//                }

//            callButton.setOnClickListener {
//                openPhoneCall(requireActivity(), nomzod!!.mobilRaqam)
//            }

            }
        }
        loadAd()
    }

    private fun setView(binding: NomzodDetailsBinding, nomzod: Nomzod) {
        binding.apply {
            nomzod.apply {
                //Photos
                val hasNomzod = userViewModel.user.hasNomzod
                if (hasNomzod) {
                    showPhotos()
                }
                nomzodQuyish.setOnClickListener {
                    showAddNomzodAlert()
                }
                photoCon.isVisible = nomzod.photos.isNotEmpty() && hasNomzod
                nomzodQuyish.isVisible = hasNomzod.not() && photos.isNotEmpty()

                val getString: (id: Int) -> String = { it ->
                    requireActivity().getString(it)
                }
                var parmText =
                    " ${
                        name.ifEmpty {
                            if (nomzod.type == KUYOV) getString(
                                R.string.kuyovlikga
                            ) else getString(R.string.kelinlikga)
                        }
                    }"
                nameAgeView.setCompoundDrawablesWithIntrinsicBounds(root.context.getDrawable(if (type == KUYOV) R.drawable.man_ic else R.drawable.woman_ic),null,null,null)
                parmText += "   ${tugilganYili}-yosh"
                nameAgeView.text = parmText
                paramsView.text = paramsText()
                millatiView.text = millati

                val oilaviyHolatiText = try {
                    appContext.getString(OilaviyHolati.valueOf(oilaviyHolati).resourceId)
                } catch (e: Exception) {
                    oilaviyHolati
                }
                if (oilaviyHolati == OilaviyHolati.AJRASHGAN.name || oilaviyHolati == OilaviyHolati.Beva.name) {
                    farzandlarView.apply {
                        visibleOrGone(true)
                        if (farzandlar.trim().isEmpty()) {
                            farzandlar = getString(R.string.yoq)
                        }
                        text = "${getString(R.string.farzandlar)}:  $farzandlar"
                    }
                } else {
                    farzandlarView.visibleOrGone(false)
                }
                oilaviyView.text = "$oilaviyHolatiText"

                val oqishText = try {
                    getString(
                        OqishMalumoti.valueOf(
                            oqishMalumoti
                        ).resId
                    )
                } catch (e: Exception) {
                    oqishMalumoti
                }
                oqishView.text =
                    "$oqishText"

                ishView.text = "$ishJoyi"

                manzilView.text = "${getString(City.valueOf(manzil).resId)}"
                tgjView.text = "${getString(R.string.tugilgan_joyi)}: $tugilganJoyi"
                qoshimchaView.text = "$talablar"
                dateView.text = DateUtils.formatDate(uploadDate)
                imkonChekBadgeTextView.visibleOrGone(imkoniyatiCheklangan)
                imkonChekBadgeTextView.visibleOrGone(imkoniyatiCheklangan)
                imkonCheckInfo.text = imkoniyatiCheklanganHaqida

                val joyOdam = nomzod.joylaganOdam
                if (joyOdam.isEmpty()) {
                    joylaganOdamView.visibleOrGone(false)
                } else {
                    joylaganOdamView.text =
                        "${getString(R.string.joylagan_odam)} - ${nomzod!!.joylaganOdam}"
                }
                qoshimchaView.maxLines = Int.MAX_VALUE

                if (yoshChegarasiDan == 0 && yoshChegarasiGacha == 0) {
                    yoshChegarasiView.visibleOrGone(false)
                } else {
                    yoshChegarasiView.text =
                        "${if (type == KUYOV) getString(R.string.kelinlikga) else getString(R.string.kuyovlikga)} yosh chegarasi $yoshChegarasiDan - $yoshChegarasiGacha gacha"
                }

                if (!yoshChegarasiView.isVisible && talablarList.isEmpty()) {
                    talablarTitle.isVisible = false
                    talablarListView.isVisible = false
                }
                if (imkoniyatiCheklangan) {
                    imkonCheckInfo.text =
                        "${getString(R.string.ma_lumot)}: $imkoniyatiCheklanganHaqida"
                }
                //Talablar
                talablarListView.layoutManager = LinearLayoutManager(
                    requireContext(), LinearLayoutManager.VERTICAL, false
                )
                talablarListView.adapter = TalablarAdapter().apply {
                    showCheckBox = false
                    try {
                        submitList(talablarList.map { Talablar.valueOf(it) })
                    } catch (e: Exception) {
                        //
                    }
                }
                requestButton.setOnClickListener {
                    if (userViewModel.user.hasNomzod.not()) {
                        showAddNomzodAlert()
                    } else {
                        SocialMedia.openLink(
                            requireActivity(),
                            SocialMedia.parseTelegramLink(nomzod.telegramLink)
                        )
                    }
                }
            }
        }
    }

    private fun showAddNomzodAlert() {
        if (userViewModel.user.valid.not()) {
            navigate(R.id.auth_graph)
            return
        }
        val alertDialog = AlertDialog.Builder(requireContext())
        val view = RequestNomzodAlertBinding.inflate(layoutInflater, null, false)
        alertDialog.setView(view.root)
        val dialog = alertDialog.create()
        view.apply {
            okButton.setOnClickListener {
                navigate(R.id.addNomzodFragment)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun loadAd() {
        binding?.adView?.apply {
            val adRequest = AdRequest.Builder().build()
            loadAd(adRequest)
        }
    }
}