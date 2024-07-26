package com.uz.sovchi.ui.verification.selfie

import android.Manifest
import android.net.Uri
import android.os.Bundle
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
import kotlinx.coroutines.launch
import java.io.File

class SelfieFragment : BaseFragment<SelfieFragmentBinding>() {

    override val layId: Int
        get() = R.layout.selfie_fragment

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
                        appContext.cacheDir,
                        "selfie" + ".jpg"
                    )
                    if (file.exists().not()) {
                        file.createNewFile()
                    }
                    fotoapparat.takePicture().saveToFile(file).await()
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
        try {
            fotoapparat?.start()
        } catch (e: Exception) {
            //Ignore
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            fotoapparat?.stop()
        } catch (e: Exception) {
            //Ignore
        }
    }

    private var fotoapparat: Fotoapparat? = null

    override fun viewCreated(bind: SelfieFragmentBinding) {
        bind.apply {
            backButton.setOnClickListener {
                closeFragment()
            }
            captureButton.setOnClickListener {
                takeSelfie()
            }
            PermissionController.getInstance().requestPermissions(
                requireContext(),
                0,
                arrayOf(Manifest.permission.CAMERA),
                object : PermissionController.PermissionResult {
                    override fun onDenied() {
                        showToast("Kameraga ruxsat bering!")
                        closeFragment()
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