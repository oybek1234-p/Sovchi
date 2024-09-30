package com.uz.sovchi.ui.filter

import android.widget.AutoCompleteTextView
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.RangeSlider
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.filter.FilterViewUtils
import com.uz.sovchi.data.filter.MyFilter
import com.uz.sovchi.data.valid
import com.uz.sovchi.databinding.FragmentFilterBinding
import com.uz.sovchi.ui.base.BaseFragment

class FilterFragment : BaseFragment<FragmentFilterBinding>() {

    override val layId: Int
        get() = R.layout.fragment_filter

    override fun viewCreated(bind: FragmentFilterBinding) {
        bind.apply {
            myToolbar.setUpBackButton(this@FilterFragment)

            FilterViewUtils.setLocationView(locationFilter.editText as AutoCompleteTextView) {

            }
            FilterViewUtils.setNomzodTypeView(typeFilter.editText as AutoCompleteTextView) {}
            FilterViewUtils.setOilaviyHolati(oilaviyView.editText as AutoCompleteTextView) {

            }
            ageSlider.labelBehavior = LabelFormatter.LABEL_VISIBLE
            ageSlider.values = listOf(
                MyFilter.filter.yoshChegarasiDan.toFloat().coerceIn(18f, 70f),
                MyFilter.filter.yoshChegarasiGacha.toFloat().coerceIn(18f, 70f)
            )
            ageSlider.setLabelFormatter {
                it.toInt().toString()
            }
            ageSlider.addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener {
                override fun onStartTrackingTouch(p0: RangeSlider) {

                }

                override fun onStopTrackingTouch(p0: RangeSlider) {
                    val values = p0.values
                    val dan = values.firstOrNull()?.toFloat() ?: MyFilter.AGE_MIN
                    val gacha = values.lastOrNull()?.toFloat() ?: MyFilter.AGE_MAX
                    MyFilter.filter.apply {
                        yoshChegarasiDan = dan.toInt()
                        yoshChegarasiGacha = gacha.toInt()
                    }
                    MyFilter.update()
                }
            })
            doneButton.setOnClickListener {
                closeFragment()
            }
        }
    }
}