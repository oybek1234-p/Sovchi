package com.uz.sovchi.ui.verification

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import coil.load
import com.uz.sovchi.R
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.data.nomzod.NomzodRepository
import com.uz.sovchi.data.nomzod.NomzodState
import com.uz.sovchi.data.verify.VerificationData
import com.uz.sovchi.databinding.VerificationLayoutBinding
import com.uz.sovchi.loadPhoto
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.photo.PickPhotoFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AddVerificationInfoFragment(override val layId: Int = R.layout.verification_layout) :
    BaseFragment<VerificationLayoutBinding>() {

    private var selfiePhotoPath: String? = null
    private var passportPhotoPath: String? = null
    private var divorcePhotoPath: String? = null

    var nomzodId = MyNomzodController.nomzod.id
    private var verificationData: VerificationData? = null

    private var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (nomzodId.isEmpty()) {
            nomzodId = MyNomzodController.nomzod.id
        }
        loadVerificationData()
    }

    private fun loadVerificationData() {
        if (nomzodId.isEmpty()) return
        loading = true
        MyNomzodController.loadVerificationInfo(nomzodId) {
            verificationData = it.also {
                passportPhotoPath = it?.passportPhoto
                selfiePhotoPath = it?.selfiePhoto
                divorcePhotoPath = it?.divorcePhoto
                loading = false
            }
            updateVerificationUi()
        }
    }

    private fun updateVerificationUi() {
        if (verificationData != null) {
            binding?.apply {
                val passport = verificationData?.passportPhoto
                val selfie = verificationData?.selfiePhoto
                passportPhoto.loadPhoto(passport)
                selfiePhoto.loadPhoto(selfie)
                passportPhoto.isVisible = passport.isNullOrEmpty().not().also {
                    if (it) {
                        passportButton.isVisible = false
                    }
                }
                selfiePhoto.isVisible = selfie.isNullOrEmpty().not().also {
                    if (it) {
                        selfieButton.isVisible = false
                    }
                }
            }
        }
    }

    private val nomzodRepo = NomzodRepository()

    private fun saveInfo() {
        if (loading) return
        if (selfiePhotoPath.isNullOrEmpty()) {
            showToast("Yuzingizni rasimga oling!")
            return
        }
        val verifyData = VerificationData()
        verifyData.selfiePhoto = selfiePhotoPath
        verifyData.passportPhoto = passportPhotoPath
        verifyData.divorcePhoto = divorcePhotoPath
        loading = true
        binding?.progressBar?.isVisible = true
        GlobalScope.launch(Dispatchers.Main) {
            nomzodRepo.uploadNewMyNomzod(MyNomzodController.nomzod.also {
                it.state = NomzodState.CHECKING
            }, verifyData) {
                if (view != null) {
                    loading = false
                    binding?.progressBar?.isVisible = false
                    closeFragment()
                    navigate(R.id.nomzodUploadSuccessFragment)
                }
            }
        }
    }

    private fun initSelfie() {
        binding?.apply {
            val openSelfie = {
                setFragmentResultListener("selfie") { _, result ->
                    val path = result.getString("path") ?: return@setFragmentResultListener
                    if (path.isEmpty().not()) {
                        selfiePhotoPath = path
                        verificationData?.selfiePhoto = selfiePhotoPath
                        binding?.selfiePhoto?.apply {
                            isVisible = true
                            load(path)
                        }
                    }
                }
                navigate(R.id.selfieFragment)
            }
            selfieButton.setOnClickListener {
                openSelfie.invoke()
            }
            selfiePhoto.setOnClickListener {
                openSelfie.invoke()
            }
            selfieIc.setOnClickListener {
                openSelfie.invoke()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateVerificationUi()
    }

    override fun viewCreated(bind: VerificationLayoutBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@AddVerificationInfoFragment)
            saveView.setOnClickListener {
                saveInfo()
            }
            initSelfie()
            val openPassport = {
                PickPhotoFragment(false) {
                    val path = it.firstOrNull() ?: return@PickPhotoFragment
                    if (path.path.isEmpty()) return@PickPhotoFragment
                    passportPhotoPath = path.path
                    verificationData?.passportPhoto = passportPhotoPath
                    passportPhoto.load(passportPhotoPath)
                    passportPhoto.isVisible = true
                }.open(mainActivity()!!)
            }
            passportTitle.setOnClickListener {
                openPassport.invoke()
            }
            passIc.setOnClickListener {
                openPassport.invoke()
            }
            passportPhoto.setOnClickListener {
                openPassport.invoke()
            }
            passportButton.setOnClickListener {
                openPassport.invoke()
            }
        }
    }

}