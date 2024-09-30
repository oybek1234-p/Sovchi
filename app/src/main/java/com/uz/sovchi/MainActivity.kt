package com.uz.sovchi

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.Firebase
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.messaging
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.REQUEST_MAX
import com.uz.sovchi.data.UserRepository
import com.uz.sovchi.data.chat.ChatController
import com.uz.sovchi.data.filter.MyFilter
import com.uz.sovchi.data.like.LikeController
import com.uz.sovchi.data.messages.MESSAGE_TYPE_CHAT_MESSAGE
import com.uz.sovchi.data.messages.MESSAGE_TYPE_NOMZOD_FOR_YOU
import com.uz.sovchi.data.messages.MESSAGE_TYPE_NOMZOD_LIKED
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.NomzodRepository
import com.uz.sovchi.data.nomzod.NomzodState
import com.uz.sovchi.data.premium.PremiumUtil
import com.uz.sovchi.data.rating.RateData
import com.uz.sovchi.data.valid
import com.uz.sovchi.data.verify.VerificationData
import com.uz.sovchi.data.viewed.ViewedNomzods
import com.uz.sovchi.databinding.ActivityMainBinding
import com.uz.sovchi.databinding.NoInternetDialogBinding
import com.uz.sovchi.databinding.NotVerifiedAccountBinding
import com.uz.sovchi.databinding.PremiumSheetBinding
import com.uz.sovchi.databinding.RateAppDialogBinding
import com.uz.sovchi.databinding.RejectInfoSheetBinding
import com.uz.sovchi.databinding.SupportSheetBinding
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.nomzod.NomzodViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    lateinit var navcontroller: NavController
    private lateinit var navHost: NavHostFragment

    private lateinit var binding: ActivityMainBinding

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

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {}

    private fun showRateInfoInputSheet(result: (string: String) -> Unit) {
        val sheet = BottomSheetDialog(this)
        val binding = RejectInfoSheetBinding.inflate(layoutInflater, null, false)
        sheet.setContentView(binding.root)
        binding.apply {
            send.text = "Yuborish"
            send.setOnClickListener {
                val text = binding.editText.text.toString()
                result.invoke(text)
                sheet.dismiss()
            }
        }
        sheet.show()
    }

    var rated = appContext.getSharedPreferences("rated", MODE_PRIVATE).getBoolean("rated", false)
        set(value) {
            appContext.getSharedPreferences("rated", MODE_PRIVATE).edit().putBoolean("rated", value)
                .apply()
            field = value
        }

    override fun onPause() {
        super.onPause()
        ChatController.currentOpenedChatNomzodId = ""
    }

    private fun showRateDialog() {
        if (LocalUser.user.valid.not()) return
        val dialog = BottomSheetDialog(this, R.style.SheetStyle)
        val binding = RateAppDialogBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(binding.root)
        binding.rateButton.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
            if (fromUser) {
                rated = true
                dialog.dismiss()
                if (rating > 3) {
                    launchMarket()
                } else {
                    showRateInfoInputSheet {
                        dialog.dismiss()
                        try {
                            FirebaseFirestore.getInstance().collection("ratings")
                                .document(LocalUser.user.uid)
                                .set(RateData(LocalUser.user.uid, rating.toInt(), it))
                        } catch (e: Exception) {
                            //
                        }
                    }
                }
            }
        }
        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        LocalUser.getUser(appContext)
        super.onCreate(null)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            navHost = (supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment)
            navcontroller = navHost.navController
            NavigationUI.setupWithNavController(binding.bottomNavView, navcontroller)
        }

        val inflater = navHost.navController.navInflater
        val graph = inflater.inflate(R.navigation.main_navigation)
        graph.setStartDestination(if (LocalUser.user.valid) R.id.searchFragment else R.id.authFragment)

        val navController = navHost.navController
        navController.setGraph(graph, Bundle().apply {
            putBoolean("new", intent.extras?.getBoolean("new") ?: false)
        })

        lifecycleScope.launch {
            ViewedNomzods.init()
            getMaxRequestLimit()
        }
        initUser()
        initUpdateManager(updateLauncher)
        checkDeeplink(intent)
        initInternetConnectivity()
    }

    private val nomzodViewModel: NomzodViewModel by viewModels()

    fun uploadMyNomzod(nomzod: Nomzod, verificationData: VerificationData?, done: () -> Unit) {
        lifecycleScope.launch {
            nomzodViewModel.repository.uploadNewMyNomzod(nomzod, verificationData) {
                done.invoke()
                if (it) {
                    UserRepository.setHasNomzod(true)
                    try {
                        if (!MyNomzodController.nomzod.verified) {
                            navcontroller.navigate(R.id.addVerificationInfoFragment)
                        } else {
                            navcontroller.navigate(R.id.nomzodUploadSuccessFragment)
                        }
                    } catch (e: Exception) {
                        handleException(e)
                    }
                }
            }
        }
    }

    private fun getMaxRequestLimit() {
        FirebaseDatabase.getInstance().getReference("request_limit").get().addOnSuccessListener {
            try {
                val limit = it.value.toString().toIntOrNull() ?: REQUEST_MAX
                REQUEST_MAX = limit
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }

    fun showNotVerifiedAccountDialog() {
        lifecycleScope.launch {
            if (isFinishing) {
                return@launch
            }
            val dialog = BottomSheetDialog(this@MainActivity, R.style.SheetStyle)
            val binding = NotVerifiedAccountBinding.inflate(
                LayoutInflater.from(this@MainActivity), null, false
            )
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
    }

    fun showPremiumSheet() {
        PremiumUtil.loadPremiumPrice {
            if (this.isFinishing || isDestroyed) return@loadPremiumPrice
            val sheet = BottomSheetDialog(this)
            val binding = PremiumSheetBinding.inflate(layoutInflater, null, false)
            binding.priceView.text = buildString {
                append(it)
                append(" ${getString(R.string.so_m)}")
            }
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
        showRateDialog()
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
        try {
            val appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo
            val listener = InstallStateUpdatedListener {
                if (it.installStatus() == InstallStatus.DOWNLOADED) {
                    // Complete the update if downloaded
                    appUpdateManager.completeUpdate()
                }
            }

            appUpdateManager.registerListener(listener)

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                if (isDestroyed || isFinishing) return@addOnSuccessListener
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    try {
                        // Start the update flow using the registered launcher
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            launcher,
                            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        handleException(e)
                    }
                }
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    fun requestNotificationPermission() {
        val permissionState =
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS);

        if (permissionState == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1
            );
        }
    }

    private fun updateChatsCount() {
        LocalUser.userLive.observeForever {
            lifecycleScope.launch {
                it.unreadChats.let {
                    binding.bottomNavView.getOrCreateBadge(R.id.chatFragment).apply {
                        isVisible = it > 0
                        number = it
                    }
                }
                it.liked.let {
                    LikeController.getLikedMeCount {
                        binding.bottomNavView.getOrCreateBadge(R.id.likedFragment).apply {
                            isVisible = it > 0
                            number = it
                        }
                    }
                }
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
                if (isFinishing || isDestroyed) return
                if (hasInternet) {
                    return
                }
                hasInternet = true
                lifecycleScope.launch {
                    internetBottomSheet.dismiss()
                    try {
                        val currentFragment = navHost.childFragmentManager.fragments[0]
                        if (currentFragment is BaseFragment<*>) currentFragment.onInternetAvailable()
                    } catch (e: Exception) {
                        handleException(e)
                    }
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                if (isFinishing || isDestroyed) return
                if (window.decorView.isAttachedToWindow) {
                    lifecycleScope.launch {
                        internetBottomSheet.show()
                    }
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

    private var preloadChats = true

    private fun initUser() {
        MyFilter.get()
        if (LocalUser.user.valid) {
            if (LocalUser.user.blocked) {
                lifecycleScope.launch {
                    binding.blockedView.isVisible = true
                }
                return
            }
            lifecycleScope.launch {
                MyFilter.update()
            }
            MyNomzodController.getNomzod()
            Firebase.messaging.subscribeToTopic(LocalUser.user.uid + "topic")
            updateChatsCount()
            if (preloadChats) {
                ChatController.preloadChats(lifecycleScope)
            }
            lifecycleScope.launch {
                UserRepository.observeCurrentUser {
                    runOnUiThread {
                        if (UserRepository.user.name.isEmpty()) {
                            navcontroller.navigate(R.id.getUserNameFragment)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            UserRepository.setOnline()
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
            telegramChannel.setOnClickListener {
                dialog.dismiss()
                SocialMedia.openLink(
                    this@MainActivity, SocialMedia.parseTelegramLink("https://t.me/sovchi_onlayn")
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
                                NomzodRepository.loadNomzod(userId).let {
                                    navcontroller.navigate(R.id.nomzodDetailsFragment,
                                        Bundle().apply {
                                            putString("nomzodId", userId)
                                        })
                                }
                            } catch (e: Exception) {
                                handleException(e)
                            }
                        }

                        MESSAGE_TYPE_CHAT_MESSAGE.toString() -> {
                            nomzodId.let {
                                preloadChats = false
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
                                handleException(e)
                            }
                        }
                    }
                }
            }
        }
    }

    fun recreateUi(isNew: Boolean = false) {
        finish()
        startActivity(intent.also {
            it.putExtra("new", isNew)
        })
    }

    override fun onDestroy() {
        if (UserRepository.user.valid && UserRepository.user.name.isEmpty()) {
            UserRepository.signOut()
        }
        super.onDestroy()
    }

    private var bottomSetUp = false
    private var bottomNavShown = true

    private val decelerateInterpolator = DecelerateInterpolator(1.5f)
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
                binding.root.post {
                    animate().setDuration(180).setInterpolator(decelerateInterpolator)
                        .translationY(if (show) 0f else measuredHeight.toFloat()).withEndAction {
                            if (!show) {
                                visibleOrGone(false)
                            }
                        }.start()
                }
            } else {
                visibleOrGone(show)
            }
        }
    }


}