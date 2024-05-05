package com.uz.sovchi.ui.nomzod

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.text.Html
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.material.color.MaterialColors
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
import com.uz.sovchi.data.nomzod.getManzilText
import com.uz.sovchi.data.nomzod.getTugilganJoyi
import com.uz.sovchi.data.nomzod.getYoshChegarasi
import com.uz.sovchi.data.nomzod.paramsText
import com.uz.sovchi.data.recombee.RecombeeDatabase
import com.uz.sovchi.data.saved.SavedRepository
import com.uz.sovchi.data.valid
import com.uz.sovchi.data.viewed.ViewedNomzods
import com.uz.sovchi.databinding.NomzodDetailsBinding
import com.uz.sovchi.databinding.RequestNomzodAlertBinding
import com.uz.sovchi.gson
import com.uz.sovchi.openImageViewer
import com.uz.sovchi.openPhoneCall
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.photo.PhotoAdapter
import com.uz.sovchi.ui.photo.PickPhotoFragment
import com.uz.sovchi.ui.search.SearchAdapter
import com.uz.sovchi.visibleOrGone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class NomzodDetailsFragment : BaseFragment<NomzodDetailsBinding>() {

    override val layId: Int
        get() = R.layout.nomzod_details

    private var nomzod: Nomzod? = null

    private val nomzodViewModel: NomzodViewModel by viewModels()
    private var nomzodId = ""

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
        nomzodId = arguments?.getString("nomzodId") ?: ""
        if (json.isNullOrEmpty().not()) {
            nomzod = gson!!.fromJson(json!!, Nomzod::class.java)
            if (nomzod != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    RecombeeDatabase.setNomzodViewed(
                        userViewModel.user.uid,
                        nomzod!!.id.ifEmpty { nomzodId })

                    ViewedNomzods.setViewed(nomzod!!.id)
                }
            }
        }
    }

    private fun setLoading(show: Boolean, empty: Boolean = false) {
        binding?.apply {
            if (empty) {
                progressBar.isVisible = false
                return
            }
            progressBar.isVisible = show
            container.isVisible = show.not()
            phoneView.isVisible = show.not()
        }
    }

    private val photosAdapter: PhotoAdapter by lazy {
        PhotoAdapter { _, pos, model, imageView ->
            imageView.openImageViewer(photosAdapter.currentList.map { it.path }, pos)
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
                                shareIntent, "Share with"
                            )
                        )
                    } else {
                        Toast.makeText(
                            context, "No suitable apps to share content!", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    override fun viewCreated(bind: NomzodDetailsBinding) {
        bind.apply {
            back.setOnClickListener { closeFragment() }
            if (nomzod?.id.isNullOrEmpty() && nomzodId.isNotEmpty()) {
                loadNomzod()
            } else {
                setView(bind, nomzod)
                loadAd()
            }
        }
    }

    private fun loadNomzod() {
        if (nomzodId.isNotEmpty()) {
            setLoading(true)
            lifecycleScope.launch {
                nomzod = nomzodViewModel.repository.getNomzodById(nomzodId, true)
                if (nomzod == null) {
                    setLoading(false, true)
                    return@launch
                }
                binding?.let {
                    lifecycleScope.launch(Dispatchers.Main) {
                        setLoading(false)
                        viewCreated(it)
                    }
                }
            }
        }
    }

    private val similarListAdapter: SearchAdapter by lazy {
        SearchAdapter(userViewModel, onClick = {
            navigateToHere(this, it)
        }, next = {}, onLiked = { liked, _ ->
            mainActivity()?.showSnack(if (liked) "Yoqtirganlarga qo'shildi!" else "Yoqtirganlardan olib tashlandi")
        })
    }

    private fun initSimilar() {
        val nomzodId = nomzod?.id ?: return
        RecombeeDatabase.getSimilarNomzods(nomzodId, 6) {
            lifecycleScope.launch {
                if (it.isNotEmpty()) {
                    binding?.apply {
                        similarContainer.isVisible = true
                        similarRecyclerView.adapter = similarListAdapter.apply {
                            submitList(it)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun setView(binding: NomzodDetailsBinding, nomzod: Nomzod?) {
        if (nomzod == null || isAdded.not() || context == null) return

        binding.apply {
            nomzod.apply {
                likeButton.isVisible = true
                //Photos
                nomzodViewModel.repository.increaseNomzodViews(id)
                initSimilar()
                showPhotos()
                nomzodQuyish.setOnClickListener {
                    showAddNomzodAlert()
                }
                //Like
                val isLiked = SavedRepository.isNomzodLiked(nomzod.id)
                likeButton.imageTintList = ColorStateList.valueOf(
                    if (isLiked) MaterialColors.getColor(
                        likeButton, androidx.appcompat.R.attr.colorPrimary
                    ) else Color.LTGRAY
                )
                likeButton.setOnClickListener {
                    val liked = !SearchAdapter.likeItem(nomzod)
                    likeButton.imageTintList = ColorStateList.valueOf(
                        if (liked) MaterialColors.getColor(
                            likeButton, androidx.appcompat.R.attr.colorPrimary
                        ) else Color.LTGRAY
                    )
                }
//                nameAgeView.setCompoundDrawablesWithIntrinsicBounds(
//                    requireContext().getDrawable(if (type == KUYOV) R.drawable.man_ic else R.drawable.woman_ic),
//                    null,
//                    null,
//                    null
//                )
                photoCon.isVisible = nomzod.photos.isNotEmpty()
                nomzodQuyish.isVisible = false

                val getString: (id: Int) -> String = { it ->
                    container.context.getString(it)
                }
                var parmText = "${
                    name.ifEmpty {
                        if (nomzod.type == KUYOV) getString(
                            R.string.kuyovlikga
                        ) else getString(R.string.kelinlikga)
                    }
                }"
                parmText += "   ${tugilganYili}-yosh"
                nameAgeView.text = parmText
                paramsView.text = paramsText()
                paramsView.isVisible = paramsView.text.trim().isNotEmpty()
                millatiView.text = "$millati"

                val oilaviyHolatiText = try {
                    appContext.getString(OilaviyHolati.valueOf(oilaviyHolati).resourceId)
                } catch (e: Exception) {
                    oilaviyHolati
                }
                if (oilaviyHolati == OilaviyHolati.AJRASHGAN.name || oilaviyHolati == OilaviyHolati.Beva.name) {
                    farzandlarView.apply {
                        visibleOrGone(true)
                        if (farzandlar.isEmpty()) {
                            farzandlar = getString(R.string.yoq)
                        }
                        text =
                            Html.fromHtml("${getString(R.string.farzandlar)}:  <b>$farzandlar<\b>")
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
                oqishView.text = Html.fromHtml("${getString(R.string.ma_lumoti)} $oqishText")

                if (ishJoyi.isNotEmpty()) {
                    ishView.text = "${getString(R.string.kasbi)} $ishJoyi"
                } else {
                    ishView.isVisible = false
                }
                val manzilText = getString(City.valueOf(manzil).resId)
                manzilView.text = getManzilText()
                if (tugilganJoyi != manzilText) {
                    tgjView.text = getTugilganJoyi()
                } else {
                    tgjView.isVisible = false
                }
                qoshimchaView.text = "$talablar"
                dateView.text = DateUtils.formatDate(uploadDate)
                imkonChekBadgeTextView.visibleOrGone(imkoniyatiCheklangan)
                imkonChekBadgeTextView.visibleOrGone(imkoniyatiCheklangan)
                imkonCheckInfo.text = imkoniyatiCheklanganHaqida

                val joyOdam = nomzod.joylaganOdam
                if (joyOdam.isEmpty()) {
                    joylaganOdamView.visibleOrGone(false)
                } else {
                    joylaganOdamView.text = "${nomzod!!.joylaganOdam}"
                }
                qoshimchaView.maxLines = Int.MAX_VALUE

                if (yoshChegarasiDan == 0 && yoshChegarasiGacha == 0) {
                    yoshChegarasiView.isVisible = false
                } else {
                    yoshChegarasiView.text = getYoshChegarasi()
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
                if (context != null) {
                    talablarListView.layoutManager = FlexboxLayoutManager(requireContext())
                    talablarListView.adapter = TalablarAdapter().apply {
                        showCheckBox = false
                        try {
                            submitList(talablarList.map { Talablar.valueOf(it) })
                        } catch (e: Exception) {
                            //
                        }
                    }
                }
                if (mobilRaqam.isNotEmpty()) {
                    callview.isVisible = true
                    callview.setOnClickListener {
                        openPhoneCall(requireActivity(), mobilRaqam)
                        RecombeeDatabase.setConnectedToNomzod(userViewModel.user.uid, nomzod.id)
                    }
                }
                requestButton.setOnClickListener {
                    RecombeeDatabase.setConnectedToNomzod(userViewModel.user.uid, nomzod.id)
                    if (userViewModel.user.hasNomzod.not()) {
                        showAddNomzodAlert()
                    } else {
                        SocialMedia.openLink(
                            requireActivity(), SocialMedia.parseTelegramLink(nomzod.telegramLink)
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