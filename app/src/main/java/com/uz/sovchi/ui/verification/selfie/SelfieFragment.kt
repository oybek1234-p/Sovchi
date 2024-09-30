package com.uz.sovchi.ui.verification.selfie

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.uz.sovchi.PermissionController
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.databinding.SelfieFragmentBinding
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseFragment
import io.fotoapparat.Fotoapparat
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.selector.front
import io.fotoapparat.view.CameraView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class SelfieFragment : BaseFragment<SelfieFragmentBinding>() {

    override val layId: Int
        get() = R.layout.selfie_fragment

    private var savedBrightness = -1f

    private fun peakScreenBright(peak: Boolean) {
        try {
            val layout: WindowManager.LayoutParams = requireActivity().window.attributes
            if (savedBrightness == 0f) {
                savedBrightness = layout.screenBrightness
            }
            if (peak) {
                layout.screenBrightness = 1f
            } else {
                layout.screenBrightness = savedBrightness
            }
            requireActivity().window.setAttributes(layout)
        } catch (e: Exception) {
            //
        }
    }

    private fun createFotoApparat(view: CameraView) {
        if (fotoapparat != null) return
        fotoapparat = Fotoapparat(context = requireContext(),
            view = view,
            scaleType = ScaleType.CenterCrop,
            lensPosition = front(),
            cameraErrorCallback = { error ->
                showToast(error.message.toString())
            })
    }

    private var selfieFile: File? = null

    private var selfieTaken = false

    private fun takeSelfie() {
        if (selfieTaken) return
        selfieTaken = true
        try {
            lifecycleScope.launch(Dispatchers.IO) {
                val fotoapparat = fotoapparat ?: return@launch
                try {
                    val file = File(
                        appContext.cacheDir, "selfie" + ".jpg"
                    )
                    if (file.exists().not()) {
                        file.createNewFile()
                    }
                    lifecycleScope.launch(Dispatchers.Main) {
                        binding?.whiteScreen?.isVisible = true
                    }
                    fotoapparat.takePicture().saveToFile(file).await()
                    lifecycleScope.launch(Dispatchers.Main) {
                        binding?.whiteScreen?.isVisible = false
                    }
                    selfieFile = file
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (isAdded) {
                            setFragmentResult("selfie", Bundle().apply {
                                putString("path", Uri.fromFile(file).toString())
                            })
                            closeFragment()
                        } else {
                            selfieTaken = false
                        }
                    }
                } catch (e: Exception) {
                    selfieTaken = false
                    showToast(e.message.toString())
                }
            }
        } catch (e: Exception) {
            selfieTaken = false
        }
    }

    private fun initCameraView(cameraView: CameraView) {
        createFotoApparat(cameraView)
    }

    override fun onResume() {
        super.onResume()
        peakScreenBright(true)
        try {
            fotoapparat?.start()
        } catch (e: Exception) {
            //Ignore
        }
    }

    override fun onPause() {
        peakScreenBright(false)
        super.onPause()
        try {
            fotoapparat?.stop()
        } catch (e: Exception) {
            //Ignore
        }
    }

    private var fotoapparat: Fotoapparat? = null

    private fun openAppSettings(activity: Activity) {
        Toast.makeText(activity, "Kameraga ruxsat bering!", Toast.LENGTH_LONG).show()
        lifecycleScope.launch {
            delay(1000)
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
            activity.startActivity(intent)
        }
    }

    override fun viewCreated(bind: SelfieFragmentBinding) {
        bind.apply {
            backButton.setOnClickListener {
                closeFragment()
            }
            captureButton.setOnClickListener {
                takeSelfie()
            }
            PermissionController.getInstance().requestPermissions(requireContext(),
                0,
                arrayOf(Manifest.permission.CAMERA),
                object : PermissionController.PermissionResult {
                    override fun onDenied() {
                        if (activity != null) {
                            openAppSettings(requireActivity())
                        } else {
                            showToast("Kameraga ruxsat bering!")
                            closeFragment()
                        }
                    }

                    override fun onGranted() {
                        initCameraView(cameraView)
                        try {
                            fotoapparat?.start()
                        } catch (e: Exception) {
                            //
                        }
                    }
                })
        }
    }

}