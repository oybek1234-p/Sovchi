package com.uz.sovchi

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.RangeSlider
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.filter.FilterViewUtils
import com.uz.sovchi.data.filter.MyFilter
import com.uz.sovchi.data.nomzod.KELIN
import com.uz.sovchi.data.nomzod.KUYOV
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.valid
import com.uz.sovchi.databinding.AgeWhatSheetBinding
import com.uz.sovchi.databinding.NomzodItemBinding
import com.uz.sovchi.databinding.RequestNomzodAlertBinding
import com.uz.sovchi.databinding.SearchFragmentBinding
import com.uz.sovchi.databinding.WeWillNotifySheetBinding
import com.uz.sovchi.databinding.WhoYouNeedBinding
import com.uz.sovchi.ui.base.BaseAdapter
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.nomzod.NomzodDetailsFragment
import com.uz.sovchi.ui.search.SearchAdapter
import com.uz.sovchi.ui.search.SearchViewModel
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.Duration
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment : BaseFragment<SearchFragmentBinding>() {
    override val layId: Int
        get() = R.layout.search_fragment

    private var listAdapter: SearchAdapter? = null
    private val viewModel: SearchViewModel by viewModels()

    private var new: Boolean = false

    private var draging = false
    private var needUpdate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        new = arguments?.getBoolean("new") ?: false
        showBottomSheet = !new
        viewModel.loadNextNomzodlar()
    }

    private fun initFilters() {
        binding?.apply {
            filterView.setOnClickListener {
                navigate(R.id.filterFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            currentNomzod = viewModel.nomzodlar[savedPosition]
        } catch (e: Exception) {
            //
        }
    }

    private val whoNeedCache: SharedPreferences =
        appContext.getSharedPreferences("whoNeedShowed", Context.MODE_PRIVATE)

    private fun showWhoNeedSheet() {
        val showed = whoNeedCache.getBoolean("showedFilter", false)
        if (showed) return
        val setShowed = {
            whoNeedCache.edit {
                putBoolean("showedFilter", true)
            }
        }
        val bottomSheet = BottomSheetDialog(requireContext())
        val binding = WhoYouNeedBinding.inflate(LayoutInflater.from(requireContext()), null, false)
        bottomSheet.setContentView(binding.root)
        bottomSheet.setCancelable(false)
        bottomSheet.setOnDismissListener {
            viewModel.refresh()
        }
        bottomSheet.show()
        binding.apply {
            kelin.setOnClickListener {
                FilterViewUtils.updateFilter {
                    nomzodType = KELIN
                    viewModel.nomzodTuri = nomzodType
                    viewModel.refresh()
                }
                bottomSheet.dismiss()
                setShowed.invoke()
                showAgeSheet(true)
            }
            kuyov.setOnClickListener {
                FilterViewUtils.updateFilter {
                    nomzodType = KUYOV
                    viewModel.nomzodTuri = nomzodType
                    viewModel.refresh()
                }
                bottomSheet.dismiss()
                setShowed.invoke()
                showAgeSheet(false)
            }
        }
    }

    private fun showAlertSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val binding =
            WeWillNotifySheetBinding.inflate(LayoutInflater.from(requireContext()), null, false)
        bottomSheet.setContentView(binding.root)
        bottomSheet.setCancelable(false)
        binding.davom.setOnClickListener {
            bottomSheet.dismiss()
        }
        bottomSheet.show()
        bottomSheet.setOnDismissListener {
            mainActivity()?.requestNotificationPermission()
            checkFilterChangedFromDefault()
            viewModel.checkNeedRefresh()
        }
    }

    private fun showAgeSheet(kelin: Boolean) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val binding =
            AgeWhatSheetBinding.inflate(LayoutInflater.from(requireContext()), null, false)
        bottomSheet.setContentView(binding.root)
        bottomSheet.setCancelable(false)
        bottomSheet.show()
        bottomSheet.setOnDismissListener {
            showAlertSheet()
        }
        binding.apply {
            title.text = "${if (kelin) "Kelin" else "Kuyov"} yosh chegarasini belgilang?"
            ageSlider.labelBehavior = LabelFormatter.LABEL_VISIBLE
            ageSlider.values = listOf(
                MyFilter.filter.yoshChegarasiDan.toFloat(),
                MyFilter.filter.yoshChegarasiGacha.toFloat()
            )
            ageSlider.setLabelFormatter { it.toInt().toString() }
            ageSlider.addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener {
                override fun onStartTrackingTouch(p0: RangeSlider) {

                }

                override fun onStopTrackingTouch(p0: RangeSlider) {
                    val values = p0.values
                    val dan = values.firstOrNull()?.toFloat() ?: 17
                    val gacha = values.lastOrNull()?.toFloat() ?: 100
                    MyFilter.filter.apply {
                        yoshChegarasiDan = dan.toInt()
                        yoshChegarasiGacha = gacha.toInt()
                    }
                    MyFilter.update()
                }
            })
            save.setOnClickListener {
                bottomSheet.dismiss()
            }
        }
    }

    override fun onInternetAvailable() {
        if (listAdapter?.itemCount == 0) {
            viewModel.refresh()
        }
    }

    private fun checkFilterChangedFromDefault() {
        val changed = MyFilter.changedFromDefault()
        binding?.filterDotView?.visibleOrGone(changed)
    }

    private fun updateUnReadLabel(unread: Int) {
        val badge = binding?.filterDotView ?: return
        badge.isVisible = unread > 0
    }

    private fun observeUnMessages() {
        updateUnReadLabel(LocalUser.user.unreadMessages)
        userViewModel.repository.observeUnReadMessages {
            updateUnReadLabel(it)
        }
    }

    private var stackLayManager: CardStackLayoutManager? = null

    override fun viewCreated(bind: SearchFragmentBinding) {
        showBottomSheet = true
        showWhoNeedSheet()
        bind.apply {
            notifyView.setOnClickListener {
                navigate(R.id.messagesFragment)
            }
            initAdapter()
            //Observe
            observe()

            initFilters()
            initSwipe()
            checkFilterChangedFromDefault()
            observeUnMessages()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkNeedRefresh()
        stackLayManager?.onRestoreInstanceState(layManagerState)
        mainActivity()?.requestNotificationPermission()
    }

    private fun observe() {
        binding?.apply {
            //List
            viewModel.nomzodlarLive.observe(viewLifecycleOwner) {
                updateList()
                checkEmpty()
            }
            //Progress
            viewModel.nomzodlarLoading.observe(viewLifecycleOwner) {
                val listEmpty = viewModel.nomzodlar.size == 0
                progressBar.visibleOrGone(it && listEmpty)
                if (it) {
                    emptyView.isVisible = false
                }
            }
        }
    }

    private fun initSwipe() {
        binding?.swipeRefresh?.isEnabled = false
    }

    private var currentNomzod: Nomzod? = null

    private fun initAdapter() {
        binding?.apply {
            setFragmentResultListener("liked") { requestKey, bundle ->
                clearFragmentResultListener("liked")
                val liked = bundle.getBoolean("liked")
                if (currentNomzod != null) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        delay(400)
                        val pos = viewModel.nomzodlar.indexOfFirst { it.id == currentNomzod!!.id }
                        onItemLiked(liked, currentNomzod!!.id, pos, true)
                    }
                }
            }
            listAdapter = SearchAdapter(userViewModel, onClick = {
                NomzodDetailsFragment.navigateToHere(this@SearchFragment, it, true)
            }, next = {}, onLiked = { _, nomzodId ->
                skipSwipeCallback = true
                onItemLiked(true, nomzodId, 0)
            }, disliked = { id, pos ->
                skipSwipeCallback = true
                onItemLiked(false, id, pos)
            }, onChatClick = { nomzod ->
                if (LocalUser.user.hasNomzod.not() || MyNomzodController.nomzod.id.isEmpty()) {
                    showAddNomzodAlert {}
                    return@SearchAdapter
                }
                if (LocalUser.user.premium || nomzod.likedMe) {
                    navigate(R.id.chatMessageFragment, Bundle().apply {
                        putString("id", nomzod.id)
                        putString("name", nomzod.name)
                        putString("photo", nomzod.photos.firstOrNull() ?: "")
                    })
                } else {
                    mainActivity()?.showChatLimitSheet()
                }
            })
            recyclerView.apply {
                adapter = listAdapter
                itemAnimator = null
                layoutManager = CardStackLayoutManager(requireContext(), cardStackListener).apply {
                    stackLayManager = this
                }
                setSwipeSettings(true)
            }
        }
    }

    private var savedPosition = 0
    private fun updateList() {
        if (draging) {
            needUpdate = true
            return
        }
        listAdapter?.submitList(viewModel.nomzodlar)
    }

    private fun checkEmpty() {
        try {
            val loading = viewModel.nomzodlarLoading.value
            if (viewModel.nomzodlar.isEmpty() && loading != true) {
                binding?.emptyView?.isVisible = true
                val message = "Sizga mos nomzodlar qolmadi, har kuni yangilari qo'shiladi"
                binding?.emptyView?.text = message
            }
        } catch (e: Exception) {
            //
        }
    }

    private fun showAddNomzodAlert(skip: () -> Unit) {
        if (userViewModel.user.valid.not()) {
            try {
                navigate(R.id.auth_graph)
            } catch (e: Exception) {
                //
            }
            return
        }
        val alertDialog = AlertDialog.Builder(requireContext(), R.style.RoundedCornersDialog)
        val view = RequestNomzodAlertBinding.inflate(layoutInflater, null, false)
        alertDialog.setView(view.root)
        val dialog = alertDialog.create()
        view.apply {
            okButton.setOnClickListener {
                try {
                    navigate(R.id.addNomzodFragment)
                } catch (e: Exception) {
                    //
                }
                dialog.dismiss()
            }
            skipButton.setOnClickListener {
                dialog.dismiss()
                skip.invoke()
            }
        }
        dialog.show()
    }

    private fun onItemLiked(liked: Boolean, nomzodId: String, pos: Int, skipLike: Boolean = false) {
        val nomzod = viewModel.nomzodlar.firstOrNull { it.id == nomzodId } ?: return
        if (LocalUser.user.hasNomzod.not() || MyNomzodController.nomzod.id.isEmpty()) {
            showAddNomzodAlert {}
            return
        }
        if (skipLike.not()) {
            val done = SearchAdapter.likeOrDislike(nomzod, liked)
            if (done.not()) return
        }
        viewModel.nomzodlar.removeIf { it.id == nomzodId }
        setSwipeSettings(liked)
        binding?.recyclerView?.swipe()
        draging = true
        view?.postDelayed(500) {
            draging = false
        }
    }

    private fun setSwipeSettings(right: Boolean) {
        stackLayManager?.apply {
            val setting = SwipeAnimationSetting.Builder()
                .setDirection(if (right) Direction.Right else Direction.Left)
                .setDuration(Duration.Slow.duration).setInterpolator(AccelerateInterpolator())
                .build()
            setCanScrollVertical(false)
            setScaleInterval(0.8f)
            setSwipeThreshold(0.5f)
            setVisibleCount(2)
            setMaxDegree(30f)
            setSwipeAnimationSetting(setting)
        }
    }

    private var layManagerState: Parcelable? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        layManagerState = stackLayManager?.onSaveInstanceState()
    }
}

object AutoCompleteView {
    fun createAutoCompleteAdapter(context: Context, list: List<Any>): ArrayAdapter<Any> {
        return ArrayAdapter(
            context, R.layout.list_item, list
        )
    }

    fun setUpAutoCompleteView(
        autoCompleteTextView: AutoCompleteTextView,
        list: List<String>,
        defaultText: String,
        click: (position: Int) -> Unit
    ) {
        autoCompleteTextView.apply {
            setText(defaultText)
            setAdapter(createAutoCompleteAdapter(autoCompleteTextView.context, list))
            onItemClickListener = AdapterView.OnItemClickListener() { _, _, position, id ->
                click.invoke(position)
            }
        }
    }
}