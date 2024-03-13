package com.uz.sovchi.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.uz.sovchi.MainActivity
import com.uz.sovchi.R
import com.uz.sovchi.UserViewModel
import com.uz.sovchi.hideSoftInput


abstract class BaseFragment<T : ViewDataBinding> : Fragment() {

    abstract val layId: Int

    abstract fun viewCreated(bind: T)

    val userViewModel: UserViewModel by activityViewModels()

    var binding: T? = null

    fun mainActivity() = if (context is MainActivity) context as MainActivity else null

    var showBottomSheet = false

    fun navigate(@IdRes resId: Int, args: Bundle? = null) {
        findNavController().navigate(
            resId,
            args,
            navOptions = animNavOptions,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, layId, container, false)
        viewCreated(binding!!)
        mainActivity()?.showBottomSheet(showBottomSheet, true)
        return binding?.root
    }

    override fun onPause() {
        hideSoftInput(requireActivity())
        super.onPause()
    }

    fun closeFragment() {
        findNavController().popBackStack()
    }

    companion object {
        val animNavOptions = NavOptions.Builder().setEnterAnim(R.anim.fragment_open_anim)
            .setExitAnim(R.anim.fragment_close_anim).setPopEnterAnim(R.anim.fragment_pop_enter)
            .setPopExitAnim(R.anim.fragment_pop_exit).build()
    }
}
