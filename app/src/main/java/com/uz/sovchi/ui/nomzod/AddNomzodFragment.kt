package com.uz.sovchi.ui.nomzod

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.nomzod.KELIN
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.OilaviyHolati
import com.uz.sovchi.data.nomzod.OqishMalumoti
import com.uz.sovchi.data.nomzod.nomzodTypes
import com.uz.sovchi.databinding.AddNomzodFragmentBinding
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.visibleOrGone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    private val yoshChegarasi: String get() = binding?.yoshChegarasiView?.editText?.text.toString()
    private val talablar: String get() = binding?.talablarView?.editText?.text.toString()
    private val telegramLink: String get() = binding?.telegramView?.editText?.text.toString()
    private val joylaganOdam: String get() = binding?.joylaganOdamView?.editText?.text.toString()
    private val mobilRaqam: String get() = binding?.mobilRaqamView?.editText?.text.toString()
    private val imkoniyatChekMalumot: String get() = binding?.imkoniyatiMalumotView?.editText?.text.toString()
    private var imkoniyatiCheklangan = false

    private var nomzodId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nomzodId = arguments?.getString("nId")
    }

    private val needNotFillViews = arrayOf(
        R.id.ismi_view,
        R.id.telegram_view,
        R.id.mobil_raqam_view,
        R.id.joylagan_odam_view,
        R.id.farzandlar_view,
        R.id.yosh_chegarasi_view,
        R.id.imkoniyati_malumot_view,
        R.id.buyi_view,
        R.id.vazni_view
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
            showToast("Telegram yoki mobil raqamni kiriting!")
        }
        if (imkoniyatiCheklangan && imkoniyatChekMalumot.isEmpty()) {
            showToast("Imkoniyati cheklanganligi haqida malumot yozing!")
        }
        notFilledView?.top?.let {
            binding?.nestedScrollView?.smoothScrollBy(0, it)
            showToast(getString(R.string.to_ldiring, error))
        }

        if (talablar.length < 70) {
            showToast("Talablarni to'liqroq yozing!")
            notFilled = true
        }
        return notFilled.not()
    }

    private val nomzodViewModel: NomzodViewModel by activityViewModels()

    private var nomzodType: Int = -1

    private val ikkinchisigaMumkin: Boolean get() = binding?.ikkinchisigaButton?.isChecked ?: false

    private fun saveNomzod() {
        val allFilled = checkEditTextsFilled()
        if (allFilled.not()) {
            return
        }
        val nomzod = Nomzod(
            if (nomzodId.isNullOrEmpty()) System.currentTimeMillis().toString() else nomzodId!!,
            LocalUser.user.uid,
            name,
            nomzodType,
            tugilganYili,
            tugilganJoyi,
            manzilSelected,
            buyi,
            vazni,
            farzandlarSoni,
            millati,
            oilaviyHolatiSelected,
            oqishMalumotiSelected,
            ishJoyi,
            yoshChegarasi,
            talablar,
            imkoniyatiCheklangan,
            imkoniyatChekMalumot,
            ikkinchisigaMumkin,
            telegramLink,
            joylaganOdam,
            mobilRaqam
        )
        nomzodViewModel.repository.uploadNewMyNomzod(nomzod)
        closeFragment()
    }

    private var manzilSelected = City.Hammasi.name
    private var oilaviyHolatiSelected = OilaviyHolati.Aralash.name
    private var oqishMalumotiSelected = OqishMalumoti.OrtaMaxsus.name

    private var currentNomzod: Nomzod? = null
    private fun initUi() {
        binding?.apply {
            deleteButton.visibleOrGone(nomzodId.isNullOrEmpty().not())
            deleteButton.setOnClickListener {
                nomzodViewModel.repository.deleteNomzod(nomzodId!!)
                findNavController().popBackStack()
            }
            with(currentNomzod!!) {
                toolbar.title =
                    if (id.isEmpty()) getString(R.string.yangi_nomzod) else getString(R.string.nomzod)
                ikkinchisigaButton.isVisible = false
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
                                farzandlarView.visibleOrGone(!buydoq)
                            }
                    }
                }

                val manzilAdapter =
                    ArrayAdapter(requireContext(), R.layout.list_item, City.asListNames())
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
                        manzilSelected = City.entries[position].name
                        if (tgjView.editText?.text.isNullOrEmpty()) {
                            tgjView.editText?.setText(getString(City.entries[position].resId))
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

                val nomzodTypeAdapter = ArrayAdapter(requireContext(),
                    R.layout.list_item,
                    nomzodTypes.map { it.second })
                typeView.editText?.apply {
                    (this as AutoCompleteTextView).apply {
                        val selectedType = nomzodTypes.find { it.first == type }
                        setText(selectedType?.second)
                        setAdapter(nomzodTypeAdapter)

                        nomzodType = type

                        if (nomzodId.isNullOrEmpty().not()) {
                            ikkinchisigaButton.isVisible = (type == KELIN).also {
                                ikkinchisigaButton.visibleOrGone(it)
                            }
                        }
                        ismiView.editText?.setText(name)
                        tgyView.editText?.setText(tugilganYili.toStringOrEmpty())
                        tgjView.editText?.setText(tugilganJoyi)
                        buyiView.editText?.setText(buyi.toStringOrEmpty())
                        vazniView.editText?.setText(vazni.toStringOrEmpty())
                        farzandlarView.editText?.setText(farzandlar)
                        millatiView.editText?.setText(millati.ifEmpty { getString(R.string.o_zbek) })
                        ishView.editText?.setText(ishJoyi)
                        yoshChegarasiView.editText?.setText(yoshChegarasi)
                        talablarView.editText?.setText(talablar)
                        telegramView.editText?.setText(telegramLink)
                        joylaganOdamView.editText?.setText(joylaganOdam)
                        mobilRaqamView.editText?.setText(mobilRaqam)
                        onItemClickListener =
                            AdapterView.OnItemClickListener { parent, view, position, id ->
                                val sType = nomzodTypes[position]
                                nomzodType = sType.first
                                ikkinchisigaButton.isVisible = sType.first == KELIN
                            }
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
                saveNomzod()
            }
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
                val nomzod = nomzodViewModel.repository.getNomzodById(nomzodId!!, true)
                currentNomzod = nomzod
                launch(Dispatchers.Main) {
                    initUi()
                }
            }
        }
    }

    override fun viewCreated(bind: AddNomzodFragmentBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@AddNomzodFragment)

            if (nomzodId.isNullOrEmpty()) {
                currentNomzod = Nomzod()
                initUi()
            } else {
                getNomzod()
            }
        }
    }
}