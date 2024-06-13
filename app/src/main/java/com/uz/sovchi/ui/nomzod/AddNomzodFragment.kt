package com.uz.sovchi.ui.nomzod

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.load
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.textfield.TextInputLayout
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.nomzod.KUYOV
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.NomzodState
import com.uz.sovchi.data.nomzod.NomzodTarif
import com.uz.sovchi.data.nomzod.OilaviyHolati
import com.uz.sovchi.data.nomzod.OqishMalumoti
import com.uz.sovchi.data.nomzod.Talablar
import com.uz.sovchi.data.nomzod.nomzodTypes
import com.uz.sovchi.databinding.AddNomzodFragmentBinding
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.photo.PhotoAdapter
import com.uz.sovchi.ui.photo.PickPhotoFragment
import com.uz.sovchi.visibleOrGone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class AddNomzodFragment : BaseFragment<AddNomzodFragmentBinding>() {

    override val layId: Int
        get() = R.layout.add_nomzod_fragment

    private val name: String get() = binding?.ismiView?.editText?.text.toString()
    private val tugilganYili: Int
        get() = binding?.tgyView?.editText?.text.toString().toIntOrNull() ?: 0
    private val tugilganJoyi: String get() = binding?.tgjView?.editText?.text.toString()
    private val manzil: String get() = binding?.manzilView?.editText?.text.toString()
    private val buyi: Int get() = binding?.buyiView?.editText?.text.toString().toIntOrNull() ?: 0
    private val vazni: Int get() = binding?.vazniView?.editText?.text.toString().toIntOrNull() ?: 0
    private val farzandlarSoni: String
        get() = binding?.farzandlarView?.editText?.text.toString()
    private val millati: String get() = binding?.millatiView?.editText?.text.toString()
    private val oilaviyHolati: String get() = binding?.oilaviyView?.editText?.text.toString()
    private val oqishMalumoti: String get() = binding?.oqishView?.editText?.text.toString()
    private val ishJoyi: String get() = binding?.ishView?.editText?.text.toString()
    private val yoshChegarasiDan: Int
        get() = binding?.yoshChegarasiDanView?.editText?.text.toString().toIntOrNull() ?: 0
    private val yoshChegarasiGacha: Int
        get() = binding?.yoshChegarasiGachaView?.editText?.text.toString().toIntOrNull() ?: 0

    private val talablar: String get() = binding?.talablarView?.editText?.text.toString()
    private val telegramLink: String get() = binding?.telegramView?.editText?.text.toString()
    private val joylaganOdam: String get() = binding?.joylaganOdamView?.editText?.text.toString()
    private val mobilRaqam: String get() = binding?.mobilRaqamView?.editText?.text.toString()
    private val imkoniyatChekMalumot: String get() = binding?.imkoniyatiMalumotView?.editText?.text.toString()
    private var imkoniyatiCheklangan = false

    private var hasChild: Boolean? = null

    private var uploadDate: Long? = null

    private val viewModel: AddNomzodViewModel by viewModels()

    private val nomzodId: String? get() = viewModel.nomzodId

    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = arguments?.getString("nId")
        isAdmin = arguments?.getBoolean("admin") ?: false
        if (id != null) {
            viewModel.nomzodId = id
        }
    }

    private var exoPlayerInit = false

    private val exoPlayer: ExoPlayer by lazy {
        exoPlayerInit = true
        ExoPlayer.Builder(requireContext()).build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (exoPlayerInit) {
            exoPlayer.release()
        }
    }

    private var audioUrl: String? = null

    private fun initAudioPlayer() {
        binding?.apply {
            updateAudioPlayer()
            setFragmentResultListener("audio") { _, result ->
                audioUrl = result.getString("url")
                updateAudioPlayer()
            }
            audioButton.setOnClickListener {
                if (nomzodType == -1) {
                    showToast("Nomzod turini tanlang!")
                    return@setOnClickListener
                }
                navigate(R.id.voiceRecordFragment, Bundle().apply {
                    putInt("type", nomzodType)
                })
            }
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        exoPlayer.seekTo(0)
                        exoPlayer.pause()
                        playView.setImageResource(R.drawable.play_ic)
                    }
                }
            })
            playView.setOnClickListener {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                    playView.setImageResource(R.drawable.play_ic)
                } else {
                    exoPlayer.play()
                    playView.setImageResource(R.drawable.pause_ic)
                }
            }
        }
    }

    private fun updateAudioPlayer() {
        if (audioUrl.isNullOrEmpty().not()) {
            exoPlayer.setMediaItem(MediaItem.fromUri(audioUrl!!))
            exoPlayer.prepare()
            binding?.playView?.isVisible = true
        } else {
            binding?.playView?.isVisible = false
        }
    }

    private val needNotFillViews = arrayOf(
        R.id.mobil_raqam_view,
        R.id.joylagan_odam_view,
        R.id.farzandlar_view,
        R.id.imkoniyati_malumot_view,
        R.id.buyi_view,
        R.id.vazni_view,
        R.id.ish_view,
        R.id.yosh_chegarasi_dan_view,
        R.id.yosh_chegarasi_gacha_view,
        R.id.ismi_view,
        R.id.telegram_view,
        R.id.talablar_view
    )

    private fun checkEditTextsFilled(): Boolean {
        val parent = binding!!.container
        var notFilled = false
        var error = ""
        var notFilledView: View? = null
        parent.children.forEach {
            if (!needNotFillViews.contains(it.id)) {
                if (it is TextInputLayout && notFilled.not()) {
                    val text = it.editText?.text?.toString()
                    if (text.isNullOrEmpty()) {
                        error = it.hint.toString()
                        notFilled = true
                        notFilledView = it
                    }
                }
            }
        }
        if (telegramLink.isEmpty() && mobilRaqam.isEmpty()) {
            notFilled = true
            showToast("Telegram yoki mobil raqamni kiriting!")
        }
        if (imkoniyatiCheklangan && imkoniyatChekMalumot.isEmpty()) {
            showToast("Imkoniyati cheklanganligi haqida malumot yozing!")
        }
        if (hasChild == null) {
            showToast("Farzandlar belgilang !")
            notFilled = true
        }
        notFilledView?.top?.let {
            showToast(getString(R.string.to_ldiring, error))
        }
        return notFilled.not()
    }

    private val nomzodViewModel: NomzodViewModel by activityViewModels()

    private var nomzodType: Int = -1

    private var uploading = false
        set(value) {
            field = value
            binding?.progressBar?.visibleOrGone(value)
        }

    private var showPhotos = true

    private fun saveNomzod(cache: Boolean = false, checkFields: Boolean = true) {
        if (uploading) return
        if (checkFields) {
            val allFilled = checkEditTextsFilled()
            if (allFilled.not()) {
                return
            }
        }
        val state = NomzodState.CHECKING
        val photos = photoAdapter.currentList
        val upDate = uploadDate ?: System.currentTimeMillis()
        val nomzod =
            Nomzod(id = if (nomzodId.isNullOrEmpty()) LocalUser.user.uid.toString() else nomzodId!!,
                userId = currentNomzod?.userId?.ifEmpty { LocalUser.user.uid }
                    ?: LocalUser.user.uid,
                name = name.trim().capitalize(Locale.ROOT),
                type = nomzodType,
                state = state,
                tarif = currentNomzod?.tarif?.ifEmpty { nomzodTarif.name } ?: nomzodTarif.name,
                currentNomzod?.paymentCheckPhotoUrl ?: "",
                photos.map { it.path },
                tugilganYili,
                tugilganJoyi.trim().capitalize(),
                manzilSelected,
                buyi,
                vazni,
                farzandlarSoni,
                hasChild ?: false,
                millati,
                oilaviyHolatiSelected,
                oqishMalumotiSelected,
                ishJoyi.trim().capitalize(),
                yoshChegarasiDan,
                yoshChegarasiGacha,
                talablar.trim().capitalize(),
                showPhotos = showPhotos,
                imkoniyatiCheklangan,
                imkoniyatChekMalumot,
                talablarList = talablarAdapter.selectedTalablar.map { it.name },
                telegramLink = telegramLink,
                joylaganOdam,
                mobilRaqam,
                uploadDateString = timestamp(upDate),
                uploadDate = upDate
            )
        if (cache) {
            currentNomzod = nomzod
            return
        }
        uploading = true
        MainScope().launch(Dispatchers.Main) {
            nomzodViewModel.repository.uploadNewMyNomzod(nomzod) {
                uploading = false
                try {
                    try {
                        userViewModel.repository.setHasNomzod(true)
                    } catch (e: Exception) {
                        //
                    }
                    closeFragment()
                    navigate(R.id.nomzodUploadSuccessFragment)
                } catch (e: Exception) {
                    //
                }
            }
        }
    }


    private fun timestamp(date: Long): String = java.sql.Timestamp(date).toString()

    private var manzilSelected = City.Hammasi.name
    private var oilaviyHolatiSelected = OilaviyHolati.Aralash.name
    private var oqishMalumotiSelected = OqishMalumoti.OrtaMaxsus.name

    private val talablarAdapter: TalablarAdapter by lazy {
        TalablarAdapter()
    }

    private val photoAdapter: PhotoAdapter by lazy {
        PhotoAdapter { del, pos, model, view ->
            if (del) {
                photoAdapter.currentList.toMutableList().apply {
                    remove(model)
                    photoAdapter.submitList(this)
                }
            }
        }
    }

    private var currentNomzod: Nomzod? = null
    private fun initUi() {
        binding?.apply {
            with(currentNomzod!!) {
                this@AddNomzodFragment.showPhotos = showPhotos
                this@AddNomzodFragment.hasChild = this.hasChild
                hidePhoto.isChecked = showPhotos.not()
                hidePhoto.setOnCheckedChangeListener { buttonView, isChecked ->
                    this@AddNomzodFragment.showPhotos = isChecked.not()
                }
                if (isAdmin && paymentCheckPhotoUrl?.ifEmpty { "" }?.isNotEmpty() == true) {
                    checkView.isVisible = true
                    chekTitle.isVisible = true
                    val check = paymentCheckPhotoUrl
                    checkView.load(check)
                }
                //Photo
                this@AddNomzodFragment.uploadDate = uploadDate

                photoRecyclerView.adapter = photoAdapter.apply {
                    submitList(photos.map { PickPhotoFragment.Image(it) })
                }
                addPhotoButton.setOnClickListener {
                    PickPhotoFragment(true) {
                        if (it.isNotEmpty()) {
                            photoAdapter.submitList(it)
                        }
                    }.open(mainActivity()!!)
                }
                backButton.setOnClickListener {
                    closeFragment()
                }
                val oilaviyHolati = OilaviyHolati.entries.filter { it != OilaviyHolati.Aralash }
                val oilaviyHolatiAdapter =
                    ArrayAdapter(requireContext(), R.layout.list_item, oilaviyHolati.map {
                        getString(it.resourceId)
                    })
                oilaviyView.editText?.apply {
                    (this as AutoCompleteTextView).apply {
                        setAdapter(oilaviyHolatiAdapter)
                        val selectType =
                            oilaviyHolati.find { it.name == currentNomzod!!.oilaviyHolati }
                        if (selectType != null) {
                            setText(getString(selectType.resourceId), false)
                            oilaviyHolatiSelected = selectType.name
                            farzandlarView.visibleOrGone(selectType != OilaviyHolati.Buydoq)
                        }
                        onItemClickListener =
                            AdapterView.OnItemClickListener { parent, view, position, id ->
                                val sType = oilaviyHolati[position]
                                oilaviyHolatiSelected = sType.name
                                val buydoq = sType == OilaviyHolati.Buydoq
                            }
                    }
                }

                val manzilAdapter =
                    ArrayAdapter(requireContext(), R.layout.list_item, City.asListNames(false))
                manzilView.editText?.apply {
                    (this as AutoCompleteTextView).apply {
                        val selectedType = City.entries.find { it.name == currentNomzod?.manzil }
                        selectedType?.let {
                            manzilSelected = it.name
                            setText(getString(it.resId), false)
                        }
                        setAdapter(manzilAdapter)
                    }

                    onItemClickListener = AdapterView.OnItemClickListener { _, _, position, id ->
                        val cityCurrent =
                            City.entries.filter { it.name != City.Hammasi.name }[position]
                        manzilSelected = cityCurrent.name
                        if (tgjView.editText?.text.isNullOrEmpty()) {
                            tgjView.editText?.setText(getString(cityCurrent.resId))
                        }
                    }
                }
                val oqishMalumotiAdapter = ArrayAdapter(requireContext(),
                    R.layout.list_item,
                    OqishMalumoti.entries.map { getString(it.resId) })
                oqishView.editText?.apply {
                    (this as AutoCompleteTextView).apply {
                        val selectedType =
                            OqishMalumoti.entries.find { it.name == currentNomzod?.oqishMalumoti }
                        selectedType?.let {
                            setText(getString(it.resId), false)
                            oqishMalumotiSelected = it.name
                        }
                        setAdapter(oqishMalumotiAdapter)
                    }
                    onItemClickListener = AdapterView.OnItemClickListener { _, _, position, id ->
                        oqishMalumotiSelected = OqishMalumoti.entries[position].name
                    }
                }
                childrenLay.setOnCheckedStateChangeListener { chipGroup, ints ->
                    val selected = ints.firstOrNull()
                    val hasChild = selected == R.id.farzand_yes
                    farzandlarView.isVisible = hasChild
                    if (selected != null && hasChild.not()) {
                        farzandlarView.editText?.setText("")
                    }
                    if (selected == null) {
                        this@AddNomzodFragment.hasChild = null
                    } else {
                        this@AddNomzodFragment.hasChild = hasChild
                    }
                }
                if (hasChild != null) {
                    childrenLay.check(if (hasChild!!) R.id.farzand_yes else R.id.farzand_no)
                }
                val nomzodTypeAdapter = ArrayAdapter(requireContext(),
                    R.layout.list_item,
                    nomzodTypes.map { it.second })
                typeView.editText?.apply {
                    (this as AutoCompleteTextView).apply {
                        val selectedType = nomzodTypes.find { it.first == type }
                        setText(selectedType?.second)
                        setAdapter(nomzodTypeAdapter)
                        onItemClickListener = AdapterView.OnItemClickListener { _, _, po, od ->
                            nomzodType = nomzodTypes[po].first

                            //Talablar submit
                            val list = arrayListOf<Talablar>()
                            list.addAll(Talablar.entries)
                            binding?.paid?.isVisible = nomzodType == KUYOV
                            if (nomzodType == KUYOV) {
                                list.apply {
                                    remove(Talablar.IkkinchiRuzgorgaTaqiq)
                                    remove(Talablar.BuydoqlarTaqiq)
                                    remove(Talablar.AlohidaUyJoy)
                                }
                            } else {
                                list.apply {
                                    remove(Talablar.Hijoblik)
                                }
                            }
                            list.apply {
                                remove(Talablar.FaqatShaxarlik)
                                remove(Talablar.FaqatViloyat)
                            }
                            talablarAdapter.submitList(list)
                            talablarView.isVisible = true
                            talabTitle.isVisible = true
                        }
                        nomzodType = type

                        //Talablar
                        try {
                            talablarListView.adapter = talablarAdapter.apply {
                                talablarList.forEach {
                                    selectedTalablar.add(Talablar.valueOf(it))
                                }
                            }
                        } catch (e: Exception) {
                            //
                        }
                        talablarListView.layoutManager = FlexboxLayoutManager(requireContext())
                        ismiView.editText?.setText(name)
                        tgyView.editText?.setText(tugilganYili.toStringOrEmpty())
                        tgjView.editText?.setText(tugilganJoyi)
                        buyiView.editText?.setText(buyi.toStringOrEmpty())
                        vazniView.editText?.setText(vazni.toStringOrEmpty())
                        farzandlarView.editText?.setText(farzandlar)
                        millatiView.editText?.setText(millati.ifEmpty { getString(R.string.o_zbek) })
                        ishView.editText?.setText(ishJoyi)
                        yoshChegarasiDanView.editText?.setText(yoshChegarasiDan.toStringOrEmpty())
                        yoshChegarasiGachaView.editText?.setText(yoshChegarasiGacha.toStringOrEmpty())
                        talablarView.editText?.setText(talablar)
                        telegramView.editText?.setText(telegramLink)
                        if (joylaganOdam.isNotEmpty()) {
                            joylaganOdamView.editText?.setText(joylaganOdam)
                        }
                        mobilRaqamView.editText?.setText(mobilRaqam)
                    }
                    this@AddNomzodFragment.imkoniyatiCheklangan =
                        currentNomzod!!.imkoniyatiCheklangan
                }
            }

            imkoniyatiMalumotView.visibleOrGone(imkoniyatiCheklangan)
            imkonchekCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
                imkoniyatiCheklangan = isChecked
                imkoniyatiMalumotView.visibleOrGone(isChecked)
            }
            saveView.setOnClickListener {
                try {
                    saveNomzod(false, true)
                } catch (e: Exception) {
                    //
                }
            }
        }
    }

    private var nomzodTarif: NomzodTarif = NomzodTarif.STANDART

    private var saving = false

    private fun startSaving() {
        if (saving) return
        try {
            if (checkEditTextsFilled().not()) {
                return
            }
            saving = true
            saveNomzod(false)
            lifecycleScope.launch {
                delay(300)
                saving = false
            }
        } catch (e: Exception) {
            //
        }
    }

    private fun Int.toStringOrEmpty(): String {
        if (this == 0) {
            return ""
        }
        return toString()
    }

    private fun getNomzod() {
        if (nomzodId.isNullOrEmpty()) return
        binding?.apply {
            lifecycleScope.launch {
                val nomzod = if (nomzodId == MyNomzodController.nomzod.id) {
                    MyNomzodController.nomzod
                } else nomzodViewModel.repository.getNomzodById(nomzodId!!, true)
                currentNomzod = nomzod
                launch(Dispatchers.Main) {
                    initUi()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        saveNomzod(true, checkFields = false)
    }

    override fun viewCreated(bind: AddNomzodFragmentBinding) {
        bind.apply {
            initAudioPlayer()

            if (currentNomzod != null) {
                initUi()
            } else {
                if (nomzodId.isNullOrEmpty()) {
                    currentNomzod = Nomzod()
                    initUi()
                } else {
                    getNomzod()
                }
            }
        }
    }
}