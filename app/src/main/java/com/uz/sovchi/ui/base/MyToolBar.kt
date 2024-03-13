package com.uz.sovchi.ui.base

import android.content.Context
import android.util.AttributeSet
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.uz.sovchi.R

class MyToolBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defS: Int = com.google.android.material.R.attr.toolbarStyle
) : MaterialToolbar(
    context,
    attrs,
    defS
) {

    var showArrowBack = false
        set(value) {
            field = value
            if (value) {
                setNavigationIcon(R.drawable.arrow_back)
            } else {
                navigationIcon = null
            }
        }

    fun setUpBackButton(fragment: Fragment) {
        setNavigationOnClickListener {
            fragment.findNavController().popBackStack()
        }
    }

    init {
        showArrowBack = true
        setTitleTextAppearance(context, R.style.ToolbarTitleApearence)
    }

}