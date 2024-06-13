package com.uz.sovchi.ui.nomzod

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.text.Html
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.exoplayer.ExoPlayer
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.uz.sovchi.DateUtils
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.like.LikeController
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
import com.uz.sovchi.data.valid
import com.uz.sovchi.data.viewed.ViewedNomzods
import com.uz.sovchi.databinding.NomzodDetailsBinding
import com.uz.sovchi.databinding.RequestNomzodAlertBinding
import com.uz.sovchi.gson
import com.uz.sovchi.openImageViewer
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.photo.PhotoAdapter
import com.uz.sovchi.ui.photo.PickPhotoFragment
import com.uz.sovchi.ui.search.SearchAdapter
import com.uz.sovchi.visibleOrGone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File


class NomzodDetailsFragment : BaseFragment<NomzodDetailsBinding>() {

    override val layId: Int
        get() = R.layout.nomzod_details

    private var nomzod: Nomzod? = null

    private val nomzodViewModel: NomzodViewModel by viewModels()
    private var nomzodId = ""

    companion object {
        fun navigateToHere(fragment: BaseFragment<*>, nomzod: Nomzod,needResult: Boolean = false) {
            val json = gson!!.toJson(nomzod)
            val bundle = Bundle().apply {
                putString("data", json)
                putBoolean("needResult",needResult)
            }
            fragment.navigate(R.id.nomzodDetailsFragment, bundle)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val json = arguments?.getString("data")
        nomzodId = arguments?.getString("nomzodId") ?: ""
        needResult = arguments?.getBoolean("needResult") ?: false
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
                    showPhotos = nomzod!!.needShowPhotos()
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

    override fun onInternetAvailable() {
        super.onInternetAvailable()
        if (nomzodSet.not()) {
            loadNomzod()
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
        }, isBackGray = true, disliked = { id, pos ->
            if (binding == null) return@SearchAdapter
            try {
                Snackbar.make(
                    binding!!.similarRecyclerView,
                    "Nomzod boshqa ko'rinmaydi!",
                    Snackbar.LENGTH_SHORT
                ).setAction("Qaytarish") {
                    lifecycleScope.launch(Dispatchers.IO) {
                        delay(500)
                        ViewedNomzods.removeDisliked(id)
                        val list = similarListAdapter.currentList.toMutableList()
                        list.add(pos, nomzod)
                        similarListAdapter.submitList(list)
                        lifecycleScope.launch(Dispatchers.Main) {
                            binding?.similarRecyclerView?.scrollToPosition(pos)
                        }
                    }

                }.show()
            } catch (e: Exception) {
                //
            }
        })
    }

    private fun updateLastSeen() {
        if (nomzod == null) return
        try {
            val userId = nomzod!!.userId
            if (userId.isNotEmpty()) {
                if (context == null || isDetached) return
                userViewModel.repository.getLastSeen(userId) {
                    try {
                        val date = DateUtils.formatDate(it)
                        val now = date == getString(R.string.yaqinda)
                        binding?.lastSeenTime?.apply {
                            text = "Onlayn $date"
                            setTextColor(
                                MaterialColors.getColor(
                                    this,
                                    if (now) androidx.appcompat.R.attr.colorPrimary else com.google.android.material.R.attr.colorOnSurfaceVariant
                                )
                            )
                        }
                    } catch (e: Exception) {
                        //
                    }
                }
            }
        } catch (e: Exception) {
            //
        }
    }

    private var exoPlayerInit = false

    private val exoPlayer: ExoPlayer by lazy {
        exoPlayerInit = true
        ExoPlayer.Builder(requireContext()).build()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (exoPlayerInit) {
            exoPlayer.release()
        }
    }

    private var nomzodSet = false

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun setView(binding: NomzodDetailsBinding, nomzod: Nomzod?) {
        if (nomzod == null || isAdded.not() || context == null) return
        try {
            binding.apply {
                nomzod.apply {
                    nomzodSet = true
                    updateLastSeen()
                    //Photos
                    nomzodViewModel.repository.increaseNomzodViews(id)
                    showPhotos()
                    //Like
                    photoCon.isVisible = nomzod.photos.isNotEmpty()
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
                    if (oilaviyHolati == OilaviyHolati.AJRASHGAN.name || oilaviyHolati == OilaviyHolati.Beva.name || oilaviyHolati == OilaviyHolati.Oilali.name) {
                        farzandlarView.apply {
                            visibleOrGone(hasChild != null || farzandlar.isNotEmpty())
                            if (hasChild != null || farzandlar.isNotEmpty()) {
                                var textT = ""
                                if (hasChild != null) {
                                    textT =
                                        (if (hasChild!!) getString(R.string.bor) else getString(R.string.yoq))
                                }
                                if (farzandlar.isNotEmpty()) {
                                    textT += "  $farzandlar"
                                }
                                text = Html.fromHtml("${getString(R.string.farzandlar)}: $textT")
                            }
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
                    qoshimchaView.isVisible = talablar.trim().isNotEmpty().also {
                        aboutTitle.isVisible = it
                        aboutLine.isVisible = it
                    }

                    dateView.text = DateUtils.formatDate(uploadDate)
                    imkonChekBadgeTextView.visibleOrGone(imkoniyatiCheklangan)
                    imkonChekBadgeTextView.visibleOrGone(imkoniyatiCheklangan)
                    imkonCheckInfo.text = imkoniyatiCheklanganHaqida

                    qoshimchaView.maxLines = Int.MAX_VALUE

                    initLikeButtons()

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
                }
            }
        } catch (e: Exception) {
            //
        }
    }

    private fun setLikeDisabled(liked: Boolean) {
        binding?.apply {
            dislikeButton.isClickable = false
            likeButton.isClickable = false
            if (liked) {
                likeButton.alpha = 0.5f
                likeButton.isEnabled = false
                likeButton.isClickable = false
                dislikeButton.isVisible = false
            } else {
                dislikeButton.alpha = 0.5f
                dislikeButton.isEnabled = false
                dislikeButton.isClickable = false
                likeButton.isVisible = false
            }
        }
    }

    private fun likeItem(nomzod: Nomzod, like: Boolean) {
        val doLike = {
            val done = SearchAdapter.likeOrDislike(nomzod, like)
            if (done) {
//                if (like) {
//                 ////   mainActivity()?.showSnack(getString(R.string.added_toLikes))
//                } else {
//                 //   mainActivity()?.showSnack(getString(R.string.didnt_like_item))
//                }
                setLikeDisabled(like)
                lifecycleScope.launch(Dispatchers.Main) {
                    delay(400)
                    if (needResult) {
                        setLikeResult(like)
                    }
                }
            }
        }
        if (LocalUser.user.hasNomzod.not()) {
            showAddNomzodAlert {}
        } else {
            doLike.invoke()
        }
    }

    private var needResult = false

    private fun setLikeResult(liked: Boolean) {
        setFragmentResult("liked", Bundle().apply {
            putBoolean("liked", liked)
        })
        closeFragment()
    }

    private fun initLikeButtons() {
        binding?.apply {
            likeContainer.isVisible = false
            dislikeButton.setOnClickListener {
                likeItem(nomzod!!, false)
            }
            likeButton.setOnClickListener {
                likeItem(nomzod!!, true)
            }
            LikeController.getLikeInfo(nomzod!!) { iLiked, likedMe, matched ->
                if (view != null && isResumed) {
                    likeContainer.isVisible = true
                    if (iLiked != null) {
                        setLikeDisabled(iLiked)
                    }
                    if (likedMe == true) {
                        likedYou.isVisible = true
                    }
                }
            }
        }
    }

    private fun showAddNomzodAlert(skip: () -> Unit) {
        if (userViewModel.user.valid.not()) {
            try {
                navigate(R.id.auth_graph)
            } catch (e: Exception) {
                //
            }
            return
        }
        val alertDialog = AlertDialog.Builder(requireContext(), R.style.RoundedCornersDialog)
        val view = RequestNomzodAlertBinding.inflate(layoutInflater, null, false)
        alertDialog.setView(view.root)
        val dialog = alertDialog.create()
        view.apply {
            okButton.setOnClickListener {
                try {
                    navigate(R.id.addNomzodFragment)
                } catch (e: Exception) {
                    //
                }
                dialog.dismiss()
            }
            skipButton.setOnClickListener {
                dialog.dismiss()
                skip.invoke()
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