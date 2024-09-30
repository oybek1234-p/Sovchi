package com.uz.sovchi.ui.nomzod

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FieldValue
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.User
import com.uz.sovchi.data.UserRepository
import com.uz.sovchi.data.filter.MyFilter
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
import com.uz.sovchi.data.verify.VerificationData
import com.uz.sovchi.databinding.AddNomzodFragmentBinding
import com.uz.sovchi.handleException
import com.uz.sovchi.hideSoftInput
import com.uz.sovchi.loadPhoto
import com.uz.sovchi.openImageViewer
import com.uz.sovchi.setEditTextErrorIf
import com.uz.sovchi.showKeyboard
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.photo.PhotoAdapter
import com.uz.sovchi.ui.photo.PickPhotoFragment
import com.uz.sovchi.visibleOrGone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.Locale

class AddNomzodFragment : BaseFragment<AddNomzodFragmentBinding>() {

    override val layId: Int
        get() = R.layout.add_nomzod_fragment

    private val name: String get() = binding?.ismiView?.editText?.text.toString()
    private val tugilganYili: Int
        get() = binding?.tgyView?.editText?.text.toString().toIntOrNull() ?: 0
    private val buyi: Int get() = binding?.buyiView?.editText?.text.toString().toIntOrNull() ?: 0
    private val vazni: Int get() = binding?.vazniView?.editText?.text.toString().toIntOrNull() ?: 0
    private val farzandlarSoni: String = ""
    private val millati: String get() = binding?.millatiView?.editText?.text.toString()

    private val ishJoyi: String get() = binding?.ishView?.editText?.text.toString()
    private val yoshChegarasiDan: Int
        get() = binding?.yoshChegarasiDanView?.editText?.text.toString().toIntOrNull() ?: 0
    private val yoshChegarasiGacha: Int
        get() = binding?.yoshChegarasiGachaView?.editText?.text.toString().toIntOrNull() ?: 0

    private val talablar: String get() = binding?.talablarView?.editText?.text.toString()
    private val telegramLink: String get() = binding?.telegramView?.editText?.text.toString()
    private val joylaganOdam: String get() = binding?.joylaganOdamView?.editText?.text.toString()
    private val mobilRaqam: String get() = binding?.mobilRaqamView?.editText?.text.toString()

    private var hasChild: Boolean? = null

    private var uploadDate: Any? = null

    private val viewModel: AddNomzodViewModel by viewModels()

    private val nomzodId: String? get() = viewModel.nomzodId

    private var isAdmin = false

    private var verificationData: VerificationData? = null

    override fun onResume() {
        super.onResume()
        updateVerificationUi()
    }

    private fun updateVerificationUi() {
        if (verificationData != null) {
            binding?.apply {
                val passport = verificationData?.passportPhoto
                val selfie = verificationData?.selfiePhoto
                val divorce = verificationData?.divorcePhoto
                if (isAdmin) {
                    passportPhoto.loadPhoto(passport)
                    passportPhoto.setOnClickListener {
                        if (passportPhotoPath.isNullOrEmpty().not()) {
                            passportPhoto.openImageViewer(arrayListOf(passportPhotoPath!!))
                        }
                    }
                    selfiePhoto.setOnClickListener {
                        if (selfiePhotoPath.isNullOrEmpty().not()) {
                            passportPhoto.openImageViewer(arrayListOf(selfiePhotoPath!!))
                        }
                    }
                    selfiePhoto.loadPhoto(selfie)
                    divorcePhoto.loadPhoto(divorce)
                    passportPhoto.isVisible = passport.isNullOrEmpty().not()
                    selfiePhoto.isVisible = selfie.isNullOrEmpty().not()
                    divorcePhoto.isVisible = divorce.isNullOrEmpty().not()
                }
            }
        }
    }

    private fun loadVerificationData() {
        if (nomzodId.isNullOrEmpty() || isAdmin.not()) return
        MyNomzodController.loadVerificationInfo(nomzodId!!) {
            verificationData = it.also {
                passportPhotoPath = it?.passportPhoto
                selfiePhotoPath = it?.selfiePhoto
                divorcePhotoPath = it?.divorcePhoto
            }
            updateVerificationUi()
        }
    }

    private var isNew = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = arguments?.getString("nId")
        isAdmin = arguments?.getBoolean("admin") ?: false
        if (id != null) {
            viewModel.nomzodId = id
        }
        isNew = arguments?.getBoolean("new") ?: false
        loadVerificationData()
    }

    private val needNotFillViews = arrayOf(
        R.id.joylagan_odam_view,
        R.id.farzandlar_view,
        R.id.imkoniyati_malumot_view,
        R.id.buyi_view,
        R.id.vazni_view,
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
        if (hasChild == null) {
            showToast("Farzandlar belgilang !")
            notFilled = true
            notFilledView = binding?.farzandYes
        } else if (name.isEmpty()) {
            showToast("Ismingizni kiriting!")
            notFilled = true
            notFilledView = binding?.ismiView
        } else if (talablar.length < 50) {
            showToast("Batafsil 80 ta belgi kiriting")
            notFilledView = binding?.talablarView
            notFilled = true
        } else if (ishJoyi.isEmpty()) {
            notFilled = true
            notFilledView = binding?.ishView
        }
        notFilledView?.apply {
            top.let {
                showToast(appContext.getString(R.string.to_ldiring, error))
                if (it > 10) {
                    binding?.nestedScrollView?.smoothScrollTo(0, it)
                }
            }
            if (this is TextInputLayout) {
                postDelayed(150) {
                    editText?.showKeyboard()
                }
            }
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

    private var uploaded = false

    private fun saveNomzod(cache: Boolean = false, checkFields: Boolean = true) {
        if (uploading || uploaded) return
        if (activity != null) {
            hideSoftInput(requireActivity())
        }
        if (checkFields) {
            val allFilled = checkEditTextsFilled()
            if (allFilled.not()) {
                return
            }
        }
        val state = NomzodState.CHECKING
        val photos = photoAdapter.currentList
        val upDate = uploadDate ?: FieldValue.serverTimestamp()
        val nomzod = Nomzod(id = if (nomzodId.isNullOrEmpty()) LocalUser.user.uid else nomzodId!!,
            userId = currentNomzod?.userId?.ifEmpty { LocalUser.user.uid } ?: LocalUser.user.uid,
            name = name.trim().capitalize(Locale.ROOT),
            type = nomzodType,
            state = state,
            tarif = currentNomzod?.tarif?.ifEmpty { nomzodTarif.name } ?: nomzodTarif.name,
            currentNomzod?.paymentCheckPhotoUrl ?: "",
            photos.map { it.path },
            tugilganYili,
            tugilganSelected,
            manzilSelected,
            buyi,
            vazni,
            farzandlarSoni,
            hasChild ?: false,
            millati,
            oilaviyHolatiSelected,
            oqishMalumotiSelected,
            ishJoyi.trim()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            yoshChegarasiDan,
            yoshChegarasiGacha,
            talablar.trim()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            showPhotos = showPhotos,
            talablarList = talablarAdapter.selectedTalablar.map { it.name },
            joylaganOdam,
            uploadDate = upDate,
            verified = MyNomzodController.nomzod.verified,
            visibleDate = System.currentTimeMillis()
        )
        if (cache) {
            currentNomzod = nomzod
            return
        }
        MyFilter.apply {
            filter.yoshChegarasiDan = nomzod.yoshChegarasiDan
            filter.yoshChegarasiGacha = nomzod.yoshChegarasiGacha
            update()
        }
        uploaded = false
        uploading = true
        UserRepository.apply {
            user.phoneNumber = mobilRaqam
            updateUser(user)
        }
        lifecycleScope.launch {
            if (activity != null) {
                mainActivity()?.uploadMyNomzod(
                    nomzod, verificationData
                ) {
                    lifecycleScope.launch {
                        uploaded = true
                        if (isAdded) {
                            uploading = false
                            closeFragment()
                        }
                    }
                }
            }
        }
    }

    private var manzilSelected = ""
    private var tugilganSelected = ""
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
            } else {
                view.openImageViewer(arrayListOf(model.path))
            }
        }.apply {
            clickable = true
        }
    }

    private var currentNomzod: Nomzod? = null
    private fun initUi() {
        val update = {
            binding?.apply {
                with(currentNomzod!!) {
                    backButton.isVisible = isNew.not()
                    skipView.isVisible = isNew
                    skipView.setOnClickListener {
                        closeFragment()
                    }
                    mobilRaqamView.editText?.setText(LocalUser.user.phoneNumber.toString())
                    photoInfoView.setOnClickListener {
                        navigate(R.id.goodBadPhotoFragment)
                    }
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
                        checkView.loadPhoto(check)
                    }
                    //Photo
                    this@AddNomzodFragment.uploadDate = uploadDate
                    if (isAdmin) {
                        addPhotoButton.isVisible = false
                        passportButton.isVisible = false
                        selfieButton.isVisible = false
                        divorceButton.isVisible = false
                        divorceSub.isVisible = false
                    }
                    photoRecyclerView.adapter = photoAdapter.apply {
                        submitList(photos.map { PickPhotoFragment.Image(it) })
                    }
                    addPhotoButton.setOnClickListener {
                        PickPhotoFragment(true) {
                            if (it.isNotEmpty()) {
                                val paths = it
                                photoAdapter.submitList(paths)
                            }
                        }.open(mainActivity()!!)
                    }
                    backButton.setOnClickListener {
                        closeFragment()
                    }
                    talablarView.setEditTextErrorIf("Kamida 50 ta belgi kiriting") {
                        this@AddNomzodFragment.talablar.length < 50
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

                                if (isAdmin) {
                                    val divorced =
                                        oilaviyHolatiSelected == OilaviyHolati.AJRASHGAN.name
                                    divorceButton.isVisible = divorced
                                    divorcePhoto.isVisible =
                                        divorced && divorcePhotoPath.isNullOrEmpty().not()
                                    divorceTitle.isVisible = divorced
                                    divorceSub.isVisible = divorced
                                }
                            }
                            onItemClickListener =
                                AdapterView.OnItemClickListener { parent, view, position, id ->
                                    val sType = oilaviyHolati[position]
                                    oilaviyHolatiSelected = sType.name
                                    val divorced = sType == OilaviyHolati.AJRASHGAN

                                    if (isAdmin) {
                                        divorceButton.isVisible = divorced
                                        divorcePhoto.isVisible =
                                            divorced && divorcePhotoPath.isNullOrEmpty().not()
                                        divorceTitle.isVisible = divorced
                                        divorceSub.isVisible = divorced
                                    }
                                }
                        }
                    }

                    val tugilganAdapter =
                        ArrayAdapter(requireContext(), R.layout.list_item, City.asListNames(false))
                    tgjView.editText?.apply {
                        (this as AutoCompleteTextView).apply {
                            val selectedType =
                                City.entries.find { it.name == currentNomzod?.tugilganJoyi }
                            selectedType?.let {
                                tugilganSelected = it.name
                                setText(getString(it.resId), false)
                            }
                            setAdapter(tugilganAdapter)
                        }

                        onItemClickListener =
                            AdapterView.OnItemClickListener { _, _, position, id ->
                                val cityCurrent =
                                    City.entries.filter { it.name != City.Hammasi.name }[position]
                                tugilganSelected = cityCurrent.name
                            }
                    }
                    val manzilAdapter =
                        ArrayAdapter(requireContext(), R.layout.list_item, City.asListNames(false))
                    manzilView.editText?.apply {
                        (this as AutoCompleteTextView).apply {
                            val selectedType =
                                City.entries.find { it.name == currentNomzod?.manzil }
                            selectedType?.let {
                                manzilSelected = it.name
                                setText(getString(it.resId), false)
                            }
                            setAdapter(manzilAdapter)
                        }

                        onItemClickListener =
                            AdapterView.OnItemClickListener { _, _, position, id ->
                                val cityCurrent =
                                    City.entries.filter { it.name != City.Hammasi.name }[position]
                                manzilSelected = cityCurrent.name
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
                        onItemClickListener =
                            AdapterView.OnItemClickListener { _, _, position, id ->
                                oqishMalumotiSelected = OqishMalumoti.entries[position].name
                            }
                    }
                    childrenLay.setOnCheckedStateChangeListener { chipGroup, ints ->
                        val selected = ints.firstOrNull()
                        val hasChild = selected == R.id.farzand_yes

                        if (selected == null) {
                            this@AddNomzodFragment.hasChild = null
                        } else {
                            this@AddNomzodFragment.hasChild = hasChild
                        }
                    }
                    if (hasChild != null) {
                        childrenLay.check(if (hasChild!!) R.id.farzand_yes else R.id.farzand_no)
                    }
                    val nomzodTypeAdapter = ArrayAdapter(
                        requireContext(),
                        R.layout.list_item,
                        nomzodTypes.map { it.second })
                    typeView.editText?.apply {
                        (this as AutoCompleteTextView).apply {
                            val selectedType = nomzodTypes.find { it.first == type }
                            setText(selectedType?.second)
                            setAdapter(nomzodTypeAdapter)

                            val updateTalablar = {
                                val list = arrayListOf<Talablar>()
                                list.addAll(Talablar.entries)
                                list.apply {
                                    remove(Talablar.IkkinchiRuzgorgaTaqiq)
                                    remove(Talablar.AlohidaUyJoy)
                                    remove(Talablar.QonuniyAjrashgan)
                                    remove(Talablar.Hijoblik)
                                    remove(Talablar.OliyMalumotli)
                                }
                                if (nomzodType == KUYOV) {
                                    list.apply {
                                        remove(Talablar.IkkinchiRuzgorgaTaqiq)
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
                            }
                            onItemClickListener = AdapterView.OnItemClickListener { _, _, po, od ->
                                nomzodType = nomzodTypes[po].first
                                updateTalablar.invoke()
                            }
                            nomzodType = type
                            //Talablar
                            try {
                                talablarListView.isVisible = false
                                talabTitle.isVisible = false
                            } catch (e: Exception) {
                                //
                            }
                            talablarListView.layoutManager = FlexboxLayoutManager(requireContext())
                            updateTalablar.invoke()

                            ismiView.editText?.setText(name)
                            tgyView.editText?.setText(tugilganYili.toStringOrEmpty())
                            buyiView.editText?.setText(buyi.toStringOrEmpty())
                            vazniView.editText?.setText(vazni.toStringOrEmpty())
                            //farzandlarView.editText?.setText(farzandlar)
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
                    }
                }
                saveView.setOnClickListener {
                    try {
                        saveNomzod(false, true)
                    } catch (e: Exception) {
                        handleException(e)
                    }
                }
            }
        }
        update.invoke()
    }

    private var selfiePhotoPath: String? = null
    private var passportPhotoPath: String? = null
    private var divorcePhotoPath: String? = null

    private fun initSelfie() {
        binding?.apply {

        }
    }

    private var nomzodTarif: NomzodTarif = NomzodTarif.STANDART

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
                } else nomzodViewModel.repository.getNomzodById(nomzodId!!)?.first
                currentNomzod = nomzod ?: Nomzod()
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