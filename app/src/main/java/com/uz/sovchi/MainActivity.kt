package com.uz.sovchi

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import com.uz.sovchi.ad.AdMobApp
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.filter.MyFilter
import com.uz.sovchi.data.messages.MESSAGE_TYPE_NOMZOD_FOR_YOU
import com.uz.sovchi.data.messages.MESSAGE_TYPE_NOMZOD_LIKED
import com.uz.sovchi.data.nomzod.NomzodRepository
import com.uz.sovchi.data.saved.SavedRepository
import com.uz.sovchi.data.valid
import com.uz.sovchi.data.viewed.ViewedNomzods
import com.uz.sovchi.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var navcontroller: NavController
    private lateinit var navHost: NavHostFragment

    private lateinit var binding: ActivityMainBinding

    private val viewModel: UserViewModel by viewModels()

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionController.getInstance().onPermissionResult(requestCode, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        PermissionController.getInstance()
            .onActivityResult(requestCode, data, resultCode == RESULT_OK)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            navHost = (supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment)
            navcontroller = navHost.navController
            bottomNavView.setupWithNavController(navcontroller)
        }
        initUser()

        val launcher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) {}
        lifecycleScope.launch {
            delay(2000)
            AdMobApp.init { }
            initUpdateManager(launcher)
        }
        checkDeeplink(intent)
    }

    fun showSnack(message: String) {
        Snackbar.make(binding.snackContainer, message, Snackbar.LENGTH_SHORT).show()
    }

    fun requestReview() {
        launchMarket()
    }

    private fun launchMarket() {
        val uri = Uri.parse("market://details?id=$packageName")
        val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(myAppLinkToMarket)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, " unable to find market app", Toast.LENGTH_LONG).show()
        }
    }

    private fun initUpdateManager(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val appUpdateManager = AppUpdateManagerFactory.create(appContext)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        val listener = InstallStateUpdatedListener {
            if (it.installStatus() == InstallStatus.DOWNLOADED) {
                appUpdateManager.completeUpdate()
            }
        }
        appUpdateManager.registerListener(listener)
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    launcher,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                )
            }
        }
    }

    private fun updateUnReadLabel(unread: Int) {
        val badge = binding.bottomNavView.getOrCreateBadge(R.id.messages_nav)
        val isVisible = unread > 0
        badge.isVisible = isVisible
        if (isVisible) {
            badge.number = unread
        }
    }

    private fun observeUnMessages() {
        updateUnReadLabel(LocalUser.user.unreadMessages)
        viewModel.repository.observeUnReadMessages {
            updateUnReadLabel(it)
            unreadMessageChangedListener?.invoke(it)
        }
    }

    fun requestNotificationPermission() {
        val permissionState =
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS);
        // If the permission is not granted, request it.
        if (permissionState == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1
            );
        }
    }

    private var unreadMessageChangedListener: ((count: Int) -> Unit)? = null

    private fun initUser() {
        LocalUser.getUser(appContext)
        MyFilter.get()
        lifecycleScope.launch {
            ViewedNomzods.init()
        }
        if (LocalUser.user.valid) {
            MyFilter.update()
            SavedRepository.loadSaved { }
            Firebase.messaging.subscribeToTopic(LocalUser.user.phoneNumber.removePrefix("+") + "topic")
            viewModel.repository.updateLastSeenTime()
            observeUnMessages()
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkDeeplink(intent)
    }

    private fun checkDeeplink(intent: Intent?) {
        if (intent != null) {
            val nomzodId = intent.extras?.getString("nomzodId")
            val type = intent.extras?.getString("type")
            if (nomzodId != null) {
                lifecycleScope.launch {
                    delay(200)
                    when (type) {
                        MESSAGE_TYPE_NOMZOD_LIKED.toString() -> {
                            try {
                                val userId = nomzodId.toString()
                                NomzodRepository.loadNomzods(-1, null, userId, "", "", limit = 1) { list, c ->
                                    val item = list.firstOrNull()
                                    if (item != null) {
                                        navcontroller.navigate(R.id.nomzodDetailsFragment, Bundle().apply {
                                            putString("nomzodId", item.id)
                                        })
                                    }
                                }
                            }catch (e: Exception) {
                                //
                            }
                        }

                        MESSAGE_TYPE_NOMZOD_FOR_YOU.toString() -> {
                            try {
                                navcontroller.navigate(R.id.nomzodDetailsFragment, Bundle().apply {
                                    putString("nomzodId", nomzodId)
                                })
                            }catch (e: Exception) {
                                //
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (LocalUser.user.valid) {
            viewModel.repository.updateLastSeenTime()
        }
    }

    fun recreateUi() {
        finish()
        startActivity(intent)
    }

    override fun onDestroy() {
        if (viewModel.user.valid && viewModel.user.name.isEmpty()) {
            viewModel.signOut()
        }
        super.onDestroy()
    }

    private var bottomSetUp = false
    private var bottomNavShown = true

    fun showBottomSheet(show: Boolean, animate: Boolean) {
        if (bottomNavShown == show) return
        bottomNavShown = show
        binding.bottomNavView.apply {
            if (!bottomSetUp && show) {
                bottomSetUp = true
            }
            if (show && !isVisible) {
                visibleOrGone(true)
            }
            if (!show && !isVisible) return
            if (show) {
                translationY = 120.toFloat()
            }
            if (animate) {
                animate().setDuration(300).translationY(if (show) 0f else measuredHeight.toFloat())
                    .withEndAction {
                        if (!show) {
                            visibleOrGone(false)
                        }
                    }.start()
            } else {
                visibleOrGone(show)
            }
        }
    }
}