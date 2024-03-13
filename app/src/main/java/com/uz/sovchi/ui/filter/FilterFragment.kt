package com.uz.sovchi.ui.filter

import android.widget.AutoCompleteTextView
import com.uz.sovchi.R
import com.uz.sovchi.data.filter.FilterViewUtils
import com.uz.sovchi.databinding.FragmentFilterBinding
import com.uz.sovchi.ui.base.BaseFragment

class FilterFragment : BaseFragment<FragmentFilterBinding>() {

    override val layId: Int
        get() = R.layout.fragment_filter

    override fun viewCreated(bind: FragmentFilterBinding) {
        bind.apply {
            myToolbar.setUpBackButton(this@FilterFragment)

            FilterViewUtils.setLocationView(locationFilter.editText as AutoCompleteTextView) { }
            FilterViewUtils.setNomzodTypeView(typeFilter.editText as AutoCompleteTextView) {}
            FilterViewUtils.setOilaviyHolati(oilaviyView.editText as AutoCompleteTextView) {}
            FilterViewUtils.setYoshChegarasiView(yoshChegarasiView.editText!!) {}
            FilterViewUtils.setImkoniyatiCheklangan(imkonchekCheckBox) {}

            doneButton.setOnClickListener {
                closeFragment()
            }
        }
    }
}