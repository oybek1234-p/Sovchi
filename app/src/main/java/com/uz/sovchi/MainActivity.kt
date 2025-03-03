package com.uz.sovchi

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
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
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
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
import com.uz.sovchi.data.messages.MESSAGE_TYPE_CHAT_MESSAGE
import com.uz.sovchi.data.messages.MESSAGE_TYPE_NOMZOD_FOR_YOU
import com.uz.sovchi.data.messages.MESSAGE_TYPE_NOMZOD_LIKED
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.NomzodRepository
import com.uz.sovchi.data.nomzod.NomzodState
import com.uz.sovchi.data.premium.PremiumUtil
import com.uz.sovchi.data.valid
import com.uz.sovchi.data.viewed.ViewedNomzods
import com.uz.sovchi.databinding.ActivityMainBinding
import com.uz.sovchi.databinding.ChatLimitSheetBinding
import com.uz.sovchi.databinding.ContactSheetBinding
import com.uz.sovchi.databinding.NoInternetDialogBinding
import com.uz.sovchi.databinding.NotVerifiedAccountBinding
import com.uz.sovchi.databinding.PremiumSheetBinding
import com.uz.sovchi.databinding.SupportSheetBinding
import com.uz.sovchi.ui.base.BaseFragment
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private lateinit var navcontroller: NavController
    private lateinit var navHost: NavHostFragment

    private lateinit var binding: ActivityMainBinding

    private val viewModel: UserViewModel by viewModels()

    fun moveToTanishing() {
        binding.bottomNavView.selectedItemId = R.id.likedFragment
    }

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
        LocalUser.getUser(appContext)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            navHost = (supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment)
            navcontroller = navHost.navController
            NavigationUI.setupWithNavController(binding.bottomNavView,navcontroller)
        }

        val inflater = navHost.navController.navInflater
        val graph = inflater.inflate(R.navigation.main_navigation)
        graph.setStartDestination(if (LocalUser.user.valid) R.id.searchFragment else R.id.splashFragment)

        val navController = navHost.navController
        navController.setGraph(graph, intent.extras)
        lifecycleScope.launch {
            ViewedNomzods.init()
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
        initInternetConnectivity()
    }

    fun showContactSheet(nomzod: Nomzod) {
        val sheet = BottomSheetDialog(this)
        val binding = ContactSheetBinding.inflate(layoutInflater, null, false)
        binding.apply {
//            telegramButton.setOnClickListener {
//                sheet.dismiss()
//                SocialMedia.openLink(this@MainActivity, SocialMedia.parseTelegramLink(nomzod.telegramLink))
//            }
//            telegramButton.isVisible = nomzod.telegramLink.isNotEmpty()
//            callButton.setOnClickListener {
//                sheet.dismiss()
//                openPhoneCall(this@MainActivity,nomzod.mobilRaqam)
//            }
//            callButton.isVisible = nomzod.mobilRaqam.isNotEmpty()
            chatButton.setOnClickListener {
                sheet.dismiss()
                navcontroller.navigate(R.id.chatMessageFragment, Bundle().apply {
                    putString("id", nomzod.id)
                    putString("name", nomzod.name)
                    putString("photo", nomzod.photos.firstOrNull() ?: "")
                })
            }
        }
        sheet.setContentView(binding.root)
        sheet.show()
    }

    fun showNotVerifiedAccountDialog() {
        val dialog = BottomSheetDialog(this, R.style.SheetStyle)
        val binding = NotVerifiedAccountBinding.inflate(LayoutInflater.from(this), null, false)
        dialog.setContentView(binding.root)
        binding.okButton.setOnClickListener {
            dialog.dismiss()
        }
        binding.apply {
            if (MyNomzodController.nomzod.state != NomzodState.CHECKING && MyNomzodController.nomzod.state != NomzodState.VISIBLE) {
                textView2.text = getString(R.string.not_verified)
                textView3.text = getString(R.string.check_not_verified_info)
            }
        }
        dialog.show()
    }

    fun showChatLimitSheet() {
        val sheet = BottomSheetDialog(this)
        val binding = ChatLimitSheetBinding.inflate(layoutInflater, null, false)
        binding.apply {
            button.setOnClickListener {
                sheet.dismiss()
                showPremiumSheet()
            }
            close.setOnClickListener {
                sheet.dismiss()
            }
        }
        sheet.setContentView(binding.root)
        sheet.show()
    }

    fun showPremiumSheet() {
        PremiumUtil.loadPremiumPrice {
            val sheet = BottomSheetDialog(this)
            val binding = PremiumSheetBinding.inflate(layoutInflater, null, false)
            binding.priceView.text = "$it so'm"
            binding.apply {
                button.setOnClickListener {
                    sheet.dismiss()
                    navcontroller.navigate(R.id.paymentGetCheckFragment)
                }
            }
            sheet.setContentView(binding.root)
            sheet.show()
        }
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

    private fun updateChatsCount() {
        viewModel.repository.observeUnChatMessages {
            binding.bottomNavView.getOrCreateBadge(R.id.messagesFragment).apply {
                isVisible = it > 0
                number = it
            }
        }
    }

    private fun initInternetConnectivity() {
        val networkRequest =
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).build()
        val connectivityManager =
            getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        var hasInternet = connectivityManager.activeNetwork != null
        if (connectivityManager.activeNetwork == null) {
            MainScope().launch {
                internetBottomSheet.show()
            }
        }
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                try {
                    if (hasInternet) {
                        return
                    }
                    hasInternet = true
                    runOnUiThread {
                        internetBottomSheet.dismiss()
                    }
                    runOnUiThread {
                        try {
                            val currentFragment = navHost.childFragmentManager.fragments[0]
                            if (currentFragment is BaseFragment<*>) currentFragment.onInternetAvailable()
                        } catch (e: Exception) {
                            //
                        }
                    }
                } catch (e: Exception) {
                    //
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                try {
                    runOnUiThread {
                        if (!isFinishing) {
                            internetBottomSheet.show()
                        }
                    }
                } catch (e: Exception) {
                    //
                }
                hasInternet = false
            }
        }
        connectivityManager.requestNetwork(networkRequest, networkCallback)
    }

    private val internetBottomSheet: BottomSheetDialog by lazy {
        BottomSheetDialog(this).apply {
            val binding =
                NoInternetDialogBinding.inflate(LayoutInflater.from(this@MainActivity), null, false)
            setContentView(binding.root)
            binding.button.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun initUser() {
        MyFilter.get()
        if (LocalUser.user.valid) {
            MyFilter.update()
            MyNomzodController.getNomzod()
            Firebase.messaging.subscribeToTopic(LocalUser.user.uid + "topic")
            viewModel.repository.updateLastSeenTime()
            updateChatsCount()
            lifecycleScope.launch {
                try {
                    viewModel.repository.loadCurrentUser()
                    runOnUiThread {
                        if (viewModel.user.name.isEmpty()) {
                            navcontroller.navigate(R.id.getUserNameFragment)
                        }
                    }
                } catch (e: Exception) {
                    //
                }
            }
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkDeeplink(intent)
    }

    fun showSupportSheet() {
        val dialog = BottomSheetDialog(this, R.style.SheetStyle)
        val binding = SupportSheetBinding.inflate(LayoutInflater.from(this), null, false)
        dialog.setContentView(binding.root)
        binding.apply {
            callview.setOnClickListener {
                dialog.dismiss()
                openPhoneCall(this@MainActivity, "+998971871415")
            }
            requestButton.setOnClickListener {
                dialog.dismiss()
                SocialMedia.openLink(
                    this@MainActivity, SocialMedia.parseTelegramLink("@oybek_tech")
                )
            }
        }
        dialog.show()
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
                                NomzodRepository.loadNomzods(
                                    -1, null, userId, "", "", limit = 1
                                ) { list, c ->
                                    val item = list.firstOrNull()
                                    if (item != null) {
                                        navcontroller.navigate(R.id.nomzodDetailsFragment,
                                            Bundle().apply {
                                                putString("nomzodId", item.id)
                                            })
                                    }
                                }
                            } catch (e: Exception) {
                                //
                            }
                        }

                        MESSAGE_TYPE_CHAT_MESSAGE.toString() -> {
                            nomzodId.let {
                                navcontroller.navigate(R.id.chatMessageFragment, Bundle().apply {
                                    putString("id", it)
                                })
                            }
                        }

                        MESSAGE_TYPE_NOMZOD_FOR_YOU.toString() -> {
                            try {
                                navcontroller.navigate(R.id.nomzodDetailsFragment, Bundle().apply {
                                    putString("nomzodId", nomzodId)
                                })
                            } catch (e: Exception) {
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

    fun recreateUi(isNew: Boolean = false) {
        finish()
        startActivity(intent.also {
            it.putExtra("new", isNew)
        })
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