package com.uz.sovchi.ui.nomzod

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.text.Html
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors
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
import com.uz.sovchi.data.nomzod.NomzodState
import com.uz.sovchi.data.nomzod.OilaviyHolati
import com.uz.sovchi.data.nomzod.OqishMalumoti
import com.uz.sovchi.data.nomzod.Talablar
import com.uz.sovchi.data.nomzod.getYoshChegarasi
import com.uz.sovchi.data.nomzod.paramsText
import com.uz.sovchi.data.utils.DateUtils
import com.uz.sovchi.data.valid
import com.uz.sovchi.data.viewed.ViewedNomzods
import com.uz.sovchi.databinding.CancelLikeAlertBinding
import com.uz.sovchi.databinding.FirstLikeToChatDialogBinding
import com.uz.sovchi.databinding.LikedActionAlertBinding
import com.uz.sovchi.databinding.NomzodDetailsBinding
import com.uz.sovchi.databinding.NotYourMatchDialogBinding
import com.uz.sovchi.databinding.VerifiedInfoBinding
import com.uz.sovchi.gson
import com.uz.sovchi.handleException
import com.uz.sovchi.loadAd
import com.uz.sovchi.openImageViewer
import com.uz.sovchi.postVal
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.like.LikeViewModel
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

    private fun showHowToLikeInfo() {
        if (context == null) return
        val pref = appContext.getSharedPreferences("appInfo", Context.MODE_PRIVATE)
        val show = pref.getBoolean("showHowToLikeInfo", true)
        if (show) {
            pref.edit().putBoolean("showHowToLikeInfo", false).apply()
            val alertDialog = AlertDialog.Builder(requireContext(), R.style.RoundedCornersDialog)
            val binding = LikedActionAlertBinding.inflate(layoutInflater, null, false)
            alertDialog.setView(binding.root)
            val dialog = alertDialog.show()
            binding.apply {
                okButton.setOnClickListener {
                    dialog.dismiss()
                }
            }
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
                ViewedNomzods.setViewed(nomzod!!.id)
            }
        }
    }

    private fun setLoading(show: Boolean, empty: Boolean = false) {
        binding?.apply {
            if (empty) {
                emptyView.isVisible = true
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

    private var photosInited = false

    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    private fun showPhotos() {
        if (photosInited) return
        binding?.apply {
            photoCountView.isVisible = true
            photoPager.apply {
                if (pageChangeCallback == null) {
                    pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            super.onPageSelected(position)
                            lifecycleScope.launch {
                                if (nomzod?.photos?.size == 0) {
                                    photoCountView.isVisible = false
                                } else {
                                    photoCountView.isVisible = true
                                    photoCountView.text = buildString {
                                        append(position + 1)
                                        append("/")
                                        append(nomzod?.photos?.size ?: 0)
                                    }
                                }
                            }
                        }
                    }
                }
                registerOnPageChangeCallback(pageChangeCallback!!)
                isVisible = true
                adapter = photosAdapter.apply {
                    showPhotos = if (likeInfo?.likedMe == true) true else nomzod!!.showPhotos
                    deleteShown = false
                    matchParent = true
                    submitList(nomzod!!.photos().map { PickPhotoFragment.Image(it) })
                }
                if (nomzod!!.photos.size > 1) {
                    springDotsIndicator.attachTo(this)
                    springDotsIndicator.isVisible = true
                } else {
                    springDotsIndicator.isVisible = false
                }
                currentItem = 0
                photosInited = true
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
                    setView(bind, nomzod, true)
                    loadAd()
                }
                loadNomzod()
            } else {
                setView(bind, nomzod, true)
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

    private var likeInfo: LikeController.LikeInfo? = null

    private fun loadNomzod() {
        if (nomzodId.isNotEmpty()) {
            if (nomzod == null) {
                setLoading(true)
            }
            lifecycleScope.launch(Dispatchers.Default) {
                if (nomzod == null) {
                    val result = nomzodViewModel.repository.getNomzodById(nomzodId)
                    val info = result.second
                    info?.let {
                        likeInfo = it
                    }
                    nomzod = result.first
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (nomzod == null) {
                            setLoading(false, empty = true)
                        } else {
                            setLoading(false)
                            binding?.let {
                                setView(it, nomzod, false)
                            }
                        }
                    }
                } else {
                    LikeController.getLikeInfo(nomzodId) {
                        likeInfo = it
                        lifecycleScope.launch(Dispatchers.Main) {
                            initLikeButtons(it)
                        }
                    }
                }
            }
        }
    }

    private fun updateLastSeen() {
        try {
            val userId = nomzod!!.userId
            if (userId.isNotEmpty()) {
                UserRepository.getLastSeen(userId) {
                    lifecycleScope.launch {
                        binding?.lastSeenTime?.apply {
                            val date = DateUtils.getUserLastSeenTime(it)
                            val now = date == appContext.getString(R.string.onlayn)
                            text = date
                            setTextColor(
                                if (now) appContext.getColor(R.color.green) else MaterialColors.getColor(
                                    this, com.google.android.material.R.attr.colorOnSurfaceVariant
                                )
                            )
                            alpha = 0f
                            animate().alpha(1f).setDuration(150).start()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        likeInited = false
        photosInited = false
        initedView = false
    }

    override fun onDestroy() {
        super.onDestroy()
        likeInfo = null
        nomzod = null
    }

    private var nomzodSet = false

    private var initedView = false


    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun setView(binding: NomzodDetailsBinding, nomzod: Nomzod?, first: Boolean) {
        if (nomzod == null || isAdded.not() || context == null) {
            return
        }
        if (initedView) {
            return
        }
        initedView = true
        val updateView = {
            try {
                binding.apply {
                    likeInfo?.let { initLikeButtons(it) }
                    updateLastSeen()
                    nomzod.apply {
                        nomzodSet = true
                        //Like
                        showPhotos()
                        photoCon.isVisible = nomzod.photos().isNotEmpty()
                        val getString: (id: Int) -> String = { it ->
                            container.context.getString(it)
                        }
                        var parmText = name
                        if (parmText.isEmpty()) {
                            parmText = if (nomzod.type == KUYOV) getString(
                                R.string.kuyovlikga
                            ) else getString(R.string.kelinlikga)
                        }
                        verifiedUserBadge.setOnClickListener {
                            showVerifiedInfo()
                        }
                        parmText += "   ${tugilganYili}-yosh"
                        nameAgeView.text = parmText
                        paramsView.text = paramsText()
                        paramsView.isVisible = paramsView.text.trim().isNotEmpty().also {
                            //   paramId.isVisible = it
                        }
                        millatiView.text = "$millati"

                        val oilaviyHolatiText = try {
                            appContext.getString(OilaviyHolati.valueOf(oilaviyHolati).resourceId)
                        } catch (e: Exception) {
                            oilaviyHolati
                        }
                        if (oilaviyHolati == OilaviyHolati.AJRASHGAN.name || oilaviyHolati == OilaviyHolati.Beva.name) {
                            childC.apply {
                                visibleOrGone(hasChild != null || farzandlar.isNotEmpty())
                                if (hasChild != null || farzandlar.isNotEmpty()) {
                                    var textT = "Farzand "
                                    if (hasChild != null) {
                                        textT += (if (hasChild!!) getString(R.string.bor) else getString(
                                            R.string.yoq
                                        ))
                                    }
                                    farzandlarView.text =
                                        Html.fromHtml("${textT.lowercase().capitalize()}")
                                }
                            }
                        } else {
                            childC.visibleOrGone(false)
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
                        manzilView.text = "${manzilText}"
                        if (manzilText != tugilganJoyi) {
                            tugilganViewC.isVisible = true
                            tgjView.text = "${tugilganJoyi}da tug'ilgan"
                        } else {
                            tugilganViewC.isVisible = false
                        }
                        qoshimchaView.text = "$talablar"
                        qoshimchaView.isVisible = talablar.trim().isNotEmpty()
                        if (verified) {
                            val verifiedText = "Rasm haqiqiy"
                            verifiedUserBadge.text = verifiedText
                            verifiedUserBadge.isVisible = true
                        } else {
                            verifiedUserBadge.isVisible = false
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
                        if (talablarList.isNotEmpty()) {
                            talablarTitle.isVisible = true
                            talablarListView.isVisible = true
                        }
                        //Talablar
                        if (context != null) {
                            talablarListView.layoutManager = FlexboxLayoutManager(requireContext())
                            talablarListView.adapter = TalablarAdapter().apply {
                                showCheckBox = false
                                try {
                                    submitList(talablarList.map { Talablar.valueOf(it) })
                                } catch (e: Exception) {
                                    handleException(e)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                handleException(e)
            }
        }
        updateView.invoke()
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

    private fun showLikeAlert(skipMatching: Boolean = true) {
        if (likeEnabled.not()) return
        if (nomzod == null) return
        if (skipMatching.not() && MyNomzodController.nomzod.isMatchToMe().not()) {
            val alertDialog = BottomSheetDialog(requireContext(), R.style.SheetStyle)
            val view = NotYourMatchDialogBinding.inflate(layoutInflater, null, false)
            alertDialog.setContentView(view.root)
            view.apply {
                skipButton.setOnClickListener {
                    alertDialog.dismiss()
                }
                okButton.setOnClickListener {
                    showLikeAlert(true)
                    alertDialog.dismiss()
                }
            }
            alertDialog.show()
            return
        }
        if (nomzod == null) return
        likeItem(nomzod!!, true)
    }

    private var likeEnabled = true

    private fun setLikeDisabled(liked: Boolean, matched: Boolean = false) {
        lifecycleScope.launch {
            binding?.apply {
                try {
                    likeEnabled = false
                    if (likedMe && liked || matched) {
                        contactButton.isVisible = true
                        contactButtonSecond.isVisible = false
                        likeView.isVisible = false
                        dislikeView.isVisible = false
                        cancelLike.isVisible = false
                    } else {
                        contactButton.isVisible = false
                        cancelLike.isVisible = true
                        if (liked) {
                            likeView.isVisible = true
                            dislikeView.isVisible = false
                            likeView.text = appContext.getString(R.string.so_rov_yuborildi)
                            likeView.isEnabled = false
                        } else {
                            likeView.isVisible = false
                            dislikeView.isVisible = true
                            dislikeView.text = "Sizga yoqmadi"
                            dislikeView.isEnabled = false
                        }
                    }
                } catch (e: Exception) {
                    handleException(e)
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
            likeView.isEnabled = true
            dislikeView.isEnabled = true
            dislikeView.isVisible = true
            cancelLike.isVisible = false
            dislikeView.text = ""
            likeView.text = getString(R.string.so_rov_yuborish)
        }
    }

    private fun likeItem(nomzod: Nomzod, like: Boolean, close: Boolean = true) {
        val doLike = doLike@{
            if (context == null) return@doLike
            val done = SearchAdapter.likeOrDislike(nomzod, like)
            binding?.apply {
                if (like) {
                    likeView.isEnabled = false
                    if (likedMe) {
                        navigate(R.id.matchedFragment, Bundle().apply {
                            putString("nomzodId", nomzod.id)
                            putString("nomzodPhoto", nomzod.photos.firstOrNull())
                        })
                        likeInfo?.matched = true
                        likeInfo?.iLiked = true
                    }
                } else {
                    dislikeView.isEnabled = false
                }
            }
            if (done) {
                LikeViewModel.allList.removeIf { it.userId == nomzodId }
                LikeViewModel.allListLive.postVal(LikeViewModel.allList)
                if (like && likedMe.not()) {
                    showToast("So'rov junatildi")
                }
                setLikeDisabled(like)
            }
        }
        if (LocalUser.user.hasNomzod.not() || MyNomzodController.nomzod.id.isEmpty()) {
            showAddNomzodAlert {}
        } else {
            doLike.invoke()
        }
    }

    private var needResult = false

    private var likedMe = false

    private var likeInited = false

    @SuppressLint("NotifyDataSetChanged")
    private fun initLikeButtons(info: LikeController.LikeInfo) {
        if (nomzod == null || context == null || likeInited) return
        likeInited = true
        if (nomzod?.showPhotos == false) {
            photosAdapter.apply {
                showPhotos = info.likedMe ?: false
                notifyDataSetChanged()
            }
        }
        val match = nomzod!!.isMatchToMe() || info.likedMe == true
        binding?.apply {
            likeContainer.isVisible = false
            dislikeView.setOnClickListener {
                if (UserRepository.user.valid.not()) {
                    navigate(R.id.authFragment)
                    return@setOnClickListener
                }
                if (LocalUser.user.hasNomzod.not() || MyNomzodController.nomzod.id.isEmpty()) {
                    showAddNomzodAlert { }
                    return@setOnClickListener
                }
                showDislikeAlert()
            }
            likeView.setOnClickListener {
                if (UserRepository.user.valid.not()) {
                    navigate(R.id.authFragment)
                    return@setOnClickListener
                }
                if (LocalUser.user.hasNomzod.not() || MyNomzodController.nomzod.id.isEmpty()) {
                    showAddNomzodAlert { }
                    return@setOnClickListener
                }
                if (UserRepository.user.blocked) {
                    showToast("Siz ilovada bloklangansiz! Bizga aloqaga chiqing")
                    return@setOnClickListener
                }
                if (MyNomzodController.nomzod.state == NomzodState.CHECKING || MyNomzodController.nomzod.state == NomzodState.REJECTED) {
                    mainActivity()?.showNotVerifiedAccountDialog()
                    return@setOnClickListener
                }
                showLikeAlert()
            }
            cancelLike.setOnClickListener {
                if (LocalUser.user.hasNomzod.not() || MyNomzodController.nomzod.id.isEmpty()) {
                    showAddNomzodAlert { }
                    return@setOnClickListener
                }
                showCancelLikeAlert()
            }
            val onContactClick = click@{
                if (nomzod == null) return@click
                val navigate = {
                    navigate(R.id.chatMessageFragment, Bundle().apply {
                        putString("id", nomzod!!.id)
                        putString("name", nomzod!!.name)
                        putString("photo", nomzod!!.photos.firstOrNull() ?: "")
                    })
                }
                if (info.matched == true) {
                    navigate.invoke()
                } else {
                    if (info.likedMe == true) {
                        likeItem(nomzod!!, true)
                        navigate.invoke()
                    } else {
                        if (info.likedMe == null) {
                            if (context == null) return@click
                            val sheet = BottomSheetDialog(requireContext(), R.style.SheetStyle)
                            val binding = FirstLikeToChatDialogBinding.inflate(layoutInflater)
                            sheet.setContentView(binding.root)
                            binding.apply {
                                okButton.setOnClickListener {
                                    sheet.dismiss()
                                }
                            }
                            sheet.show()
                        }
                    }
                }
            }
            contactButton.setOnClickListener {
                onContactClick.invoke()
            }
            contactButtonSecond.setOnClickListener {
                onContactClick.invoke()
            }
            if (binding != null && context != null) {
                likedMe = info.likedMe ?: false
                likeContainer.apply {
                    isVisible = true
                    alpha = 0f
                    translationY = 80f
                    animate().translationY(0f).alpha(1f).setDuration(150).start()
                }
                if (match) {
                    if (info.dislikedMe == true) {
                        dislikedYou.isVisible = true
                        likeContainer.forEach {
                            it.isVisible = it == notForYou
                        }
                        notForYou.text = getString(R.string.rad_javobi)
                        contactButtonSecond.isVisible = false
                    } else if (info.iLiked != null) {
                        contactButtonSecond.isVisible = true
                        setLikeDisabled(info.iLiked!!, info.matched ?: false)
                    } else {
                        if (likedMe) {
                            likeView.text = appContext.getString(R.string.qabul_qilish)
                        }
                        contactButtonSecond.isVisible = true
                    }
                    if (likedMe) {
                        this@NomzodDetailsFragment.likedMe = likedMe
                        likedYou.isVisible = true
                        likedYou.apply {
                            isVisible = true
                            alpha = 0f
                            animate().alpha(1f).setDuration(100).start()
                        }
                    }
                } else {
                    binding?.apply {
                        likeContainer.forEach {
                            it.isVisible = it == notForYou
                        }
                        contactButtonSecond.isVisible = false
                    }
                }
            }
        }
    }

    private fun showVerifiedInfo() {
        val sheet = BottomSheetDialog(requireContext(), R.style.SheetStyle)
        val binding = VerifiedInfoBinding.inflate(layoutInflater, null, false)
        binding.apply {
            done.setOnClickListener {
                sheet.dismiss()
            }
        }
        sheet.setContentView(binding.root)
        sheet.show()
    }

    private fun showAddNomzodAlert(skip: () -> Unit) {
        if (UserRepository.user.valid.not()) {
            try {
                navigate(R.id.authFragment)
            } catch (e: Exception) {
                handleException(e)
            }
            return
        }
        try {
            navigate(R.id.addNomzodFragment)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private fun loadAd() {
        lifecycleScope.launch {
            delay(300)
            binding?.adView?.loadAd()
        }
    }
}