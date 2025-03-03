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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors
import com.uz.sovchi.DateUtils
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.UserRepository
import com.uz.sovchi.data.like.LikeController
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.nomzod.KUYOV
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.NomzodRepository
import com.uz.sovchi.data.nomzod.OilaviyHolati
import com.uz.sovchi.data.nomzod.OqishMalumoti
import com.uz.sovchi.data.nomzod.Talablar
import com.uz.sovchi.data.nomzod.getYoshChegarasi
import com.uz.sovchi.data.nomzod.isVisible
import com.uz.sovchi.data.nomzod.paramsText
import com.uz.sovchi.data.recombee.RecombeeDatabase
import com.uz.sovchi.data.valid
import com.uz.sovchi.data.viewed.ViewedNomzods
import com.uz.sovchi.databinding.CancelLikeAlertBinding
import com.uz.sovchi.databinding.DislikeActionAlertBinding
import com.uz.sovchi.databinding.LikedActionAlertBinding
import com.uz.sovchi.databinding.NomzodDetailsBinding
import com.uz.sovchi.gson
import com.uz.sovchi.loadAd
import com.uz.sovchi.openImageViewer
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
        fun navigateToHere(fragment: BaseFragment<*>, nomzod: Nomzod, needResult: Boolean = false) {
            val json = gson!!.toJson(nomzod)
            val bundle = Bundle().apply {
                putString("data", json)
                putBoolean("needResult", needResult)
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
            imageView.openImageViewer(
                photosAdapter.currentList.map { it.path }, pos, nomzod?.showPhotos ?: true
            )
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
                    binding?.photoHideInfo?.isVisible = showPhotos.not()
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
                val cached = NomzodRepository.cacheNomzods[nomzodId]
                if (cached != null) {
                    nomzod = cached
                    setView(bind, nomzod)
                    loadAd()
                }
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
            if (nomzod == null) {
                setLoading(true)
            }
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
        likeInited = false
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
                    if (oilaviyHolati == OilaviyHolati.AJRASHGAN.name || oilaviyHolati == OilaviyHolati.Beva.name) {
                        farzandlarView.apply {
                            visibleOrGone(hasChild != null || farzandlar.isNotEmpty())
                            if (hasChild != null || farzandlar.isNotEmpty()) {
                                var textT = ""
                                if (hasChild != null) {
                                    textT += (if (hasChild!!) getString(R.string.bor) else getString(
                                        R.string.yoq
                                    ))
                                }
                                if (farzandlar.isNotEmpty()) {
                                    textT += "  $farzandlar"
                                }
                                text = Html.fromHtml("${textT}")
                            }
                        }
                    } else {
                        farzandlarView.visibleOrGone(false)
                        farzandTitle.isVisible = false
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
                    oqishView.text = Html.fromHtml("$oqishText")

                    if (ishJoyi.isNotEmpty()) {
                        ishView.text = "$ishJoyi"
                    } else {
                        ishView.isVisible = false
                    }
                    val manzilText = getString(City.valueOf(manzil).resId)
                    manzilView.text = manzilText
                    if (manzilText != tugilganJoyi) {
                        tugilganViewC.isVisible = true
                        tgjView.text = tugilganJoyi
                    } else {
                        tugilganViewC.isVisible = false
                    }
                    qoshimchaView.text = "$talablar"
                    qoshimchaView.isVisible = talablar.trim().isNotEmpty().also {
                        aboutTitle.isVisible = false
                        aboutLine.isVisible = it
                    }
                    dateView.text = DateUtils.formatDate(uploadDate)
                    imkonChekBadgeTextView.visibleOrGone(imkoniyatiCheklangan)
                    imkonChekBadgeTextView.visibleOrGone(imkoniyatiCheklangan)
                    imkonCheckInfo.text = imkoniyatiCheklanganHaqida
                    if (verified) {
                        verifiedUserBadge.isVisible = true
                        notVerifiedUserBadge.isVisible = false
                    } else {
                        notVerifiedUserBadge.isVisible = true
                        verifiedUserBadge.isVisible = false
                    }
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

    private fun showCancelLikeAlert() {
        val alertDialog = AlertDialog.Builder(requireContext(), R.style.RoundedCornersDialog)
        val view = CancelLikeAlertBinding.inflate(layoutInflater, null, false)
        alertDialog.setView(view.root)
        val dialog = alertDialog.create()
        view.apply {
            skipButton.setOnClickListener {
                dialog.dismiss()
            }
            okButton.setOnClickListener {
                cancelLike()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showDislikeAlert() {
        if (likeEnabled.not()) return
        likeItem(nomzod!!, false)
    }

    private fun showLikeAlert() {
        if (likeEnabled.not()) return
        likeItem(nomzod!!, true)
    }

    private var likeEnabled = true

    private fun setLikeDisabled(liked: Boolean, matched: Boolean = false) {
        binding?.apply {
            likeEnabled = false
            if (likedMe && liked || matched) {
                contactButton.isVisible = true
                likeView.isVisible = false
                dislikeView.isVisible = false
                cancelLike.isVisible = false
            } else {
                contactButton.isVisible = false
                cancelLike.isVisible = true
                if (liked) {
                    likeView.isVisible = true
                    dislikeView.isVisible = false
                    likeView.text = getString(R.string.so_rov_yuborildi)
                } else {
                    likeView.isVisible = false
                    dislikeView.isVisible = true
                }
            }
        }
    }

    private fun cancelLike() {
        if (nomzod == null) return
        LikeController.removeLiked(nomzod!!)
        likeEnabled = true
        binding?.apply {
            likeView.isVisible = true
            dislikeView.isVisible = true
            cancelLike.isVisible = false
            likeView.text = getString(R.string.so_rov_yuborish)
        }
    }

    private fun likeItem(nomzod: Nomzod, like: Boolean, close: Boolean = true) {
        val doLike = {
            val done = SearchAdapter.likeOrDislike(nomzod, like)
            if (done) {
                setLikeDisabled(like)
                lifecycleScope.launch(Dispatchers.Main) {
                    delay(400)
                    if (needResult && close) {
                        setLikeResult(like)
                    }
                }
            }
        }
        if (LocalUser.user.hasNomzod.not() || MyNomzodController.nomzod.id.isEmpty()) {
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

    private var likedMe = false

    private var likeInited = false

    private fun initLikeButtons() {
        if(likeInited) return
        likeInited = true
        binding?.apply {
            likeContainer.isVisible = false
            dislikeView.setOnClickListener {
                if (LocalUser.user.hasNomzod.not() || MyNomzodController.nomzod.id.isEmpty()) {
                    showAddNomzodAlert { }
                    return@setOnClickListener
                }
                if (MyNomzodController.nomzod.isVisible().not()) {
                    mainActivity()?.showNotVerifiedAccountDialog()
                    return@setOnClickListener
                }
                showDislikeAlert()
            }
            likeView.setOnClickListener {
                if (LocalUser.user.hasNomzod.not() || MyNomzodController.nomzod.id.isEmpty()) {
                    showAddNomzodAlert { }
                    return@setOnClickListener
                }
                if (MyNomzodController.nomzod.isVisible().not()) {
                    mainActivity()?.showNotVerifiedAccountDialog()
                    return@setOnClickListener
                }
                if (UserRepository.user.requests >= 5) {
                    mainActivity()?.showPremiumSheet()
                    return@setOnClickListener
                }
                showLikeAlert()
            }
            cancelLike.setOnClickListener {
                if (LocalUser.user.hasNomzod.not() || MyNomzodController.nomzod.id.isEmpty()) {
                    showAddNomzodAlert { }
                    return@setOnClickListener
                }
                if (MyNomzodController.nomzod.isVisible().not()) {
                    mainActivity()?.showNotVerifiedAccountDialog()
                    return@setOnClickListener
                }
                showCancelLikeAlert()
            }
            var matchedMe = false
            var iLikedIt = false
            contactButton.setOnClickListener {
                if (nomzod == null) return@setOnClickListener
                navigate(R.id.chatMessageFragment, Bundle().apply {
                    putString("id", nomzod!!.id)
                    putString("name", nomzod!!.name)
                    putString("photo", nomzod!!.photos.firstOrNull() ?: "")
                })
            }
            chatView.setOnClickListener {
                if (LocalUser.user.hasNomzod.not() || MyNomzodController.nomzod.id.isEmpty()) {
                    showAddNomzodAlert { }
                    return@setOnClickListener
                }
                if (MyNomzodController.nomzod.isVisible().not()) {
                    mainActivity()?.showNotVerifiedAccountDialog()
                    return@setOnClickListener
                }
                if (likedMe || matchedMe) {
                    if (iLikedIt.not()) {
                        likeItem(nomzod!!, true, false)
                    }
                    navigate(R.id.chatMessageFragment, Bundle().apply {
                        putString("id", nomzod!!.id)
                        putString("photo", nomzod!!.photos.firstOrNull() ?: "")
                        putString("name", nomzod!!.name)
                    })
                }
            }
            LikeController.getLikeInfo(nomzod!!) { iLiked, likedMe, dislikedMe, matched ->
                if (view != null && isResumed) {
                    chatView.isClickable = true
                    likeContainer.isVisible = true
                    matchedMe = matched ?: false
                    iLikedIt = iLiked ?: false
                    if (iLiked != null) {
                        setLikeDisabled(iLiked, matched ?: false)
                    } else {
                        if (likedMe == true) {
                            likeView.text = requireContext().getString(R.string.qabul_qilish)
                        }
                    }
                    if (likedMe == true) {
                        this@NomzodDetailsFragment.likedMe = likedMe
                        likedYou.isVisible = true
                    }
                    if (dislikedMe == true) {
                        dislikedYou.isVisible = true
                    }
                }
            }
        }
    }

    private fun showAddNomzodAlert(skip: () -> Unit) {
        if (userViewModel.user.valid.not()) {
            try {
                navigate(R.id.authFragment)
            } catch (e: Exception) {
                //
            }
            return
        }
        try {
            navigate(R.id.addNomzodFragment)
        } catch (e: Exception) {
            //
        }
    }

    private fun loadAd() {
        binding?.adView?.loadAd()
    }
}