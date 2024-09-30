package com.uz.sovchi.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.IdRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.uz.sovchi.MainActivity
import com.uz.sovchi.R
import com.uz.sovchi.handleException
import com.uz.sovchi.hideSoftInput

abstract class BaseFragment<T : ViewDataBinding> : Fragment() {

    abstract val layId: Int

    abstract fun viewCreated(bind: T)

    var binding: T? = null

    fun mainActivity() = if (activity is MainActivity) activity as MainActivity else null

    var showBottomSheet = false

    fun navigate(@IdRes resId: Int, args: Bundle? = null) {
        try {
            findNavController().navigate(
                resId,
                args,
                navOptions = animNavOptions,
            )
        } catch (e: Exception) {
            handleException(e)
        }
    }

    var transitionAnimating = false
    open fun onTransitionEnd() {}

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        try {
            val animation = super.onCreateAnimation(transit, enter, nextAnim)
                ?: AnimationUtils.loadAnimation(context, nextAnim)

            return animation?.also {
                it.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationEnd(animation: Animation?) {
                        transitionAnimating = false
                        onTransitionEnd()
                    }

                    override fun onAnimationRepeat(animation: Animation?) {

                    }

                    override fun onAnimationStart(animation: Animation?) {
                        transitionAnimating = true
                    }
                })
            }
        } catch (e: Exception) {
            return null
        }
    }

    open fun onInternetAvailable() {

    }

    override fun onResume() {
        super.onResume()
        mainActivity()?.showBottomSheet(showBottomSheet, true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, layId, container, false)
        viewCreated(binding!!)
        return binding?.root
    }

    override fun onPause() {
        hideSoftInput(requireActivity())
        super.onPause()
    }

    fun closeFragment() {
        if (isDetached) return
        try {
            if (isAdded) {
                findNavController().popBackStack()
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    companion object {
        val animNavOptions = NavOptions.Builder().setEnterAnim(R.anim.fragment_open_anim)
            .setExitAnim(R.anim.fragment_close_anim).setPopEnterAnim(R.anim.fragment_pop_enter)
            .setPopExitAnim(R.anim.fragment_pop_exit).build()
    }
}
