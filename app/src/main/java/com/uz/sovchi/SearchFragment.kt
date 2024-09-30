package com.uz.sovchi

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.RangeSlider
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.ValueEventListener
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.UserRepository
import com.uz.sovchi.data.filter.FilterViewUtils
import com.uz.sovchi.data.filter.MyFilter
import com.uz.sovchi.data.nomzod.KELIN
import com.uz.sovchi.data.nomzod.KUYOV
import com.uz.sovchi.data.valid
import com.uz.sovchi.databinding.AgeWhatSheetBinding
import com.uz.sovchi.databinding.DislikeActionAlertBinding
import com.uz.sovchi.databinding.SearchFragmentBinding
import com.uz.sovchi.databinding.WeWillNotifySheetBinding
import com.uz.sovchi.databinding.WhoYouNeedBinding
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.search.SearchAdapter
import com.uz.sovchi.ui.search.SearchViewModel
import com.uz.sovchi.ui.utils.EndlessScrollListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment : BaseFragment<SearchFragmentBinding>() {
    override val layId: Int
        get() = R.layout.search_fragment

    private var listAdapter: SearchAdapter? = null
    private val viewModel: SearchViewModel by viewModels()

    private var new: Boolean = false

    private var showDialogDelayed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        new = arguments?.getBoolean("new") ?: false

        viewModel.loadNextNomzodlar()

        if (LocalUser.user.hasNomzod.not() && LocalUser.user.valid) {
            lifecycleScope.launch {
                delay(200)
                navigate(R.id.addNomzodFragment, Bundle().apply {
                    putBoolean("new", new)
                    showDialogDelayed = true
                })
            }
        }
    }

    private fun initFilters() {
        binding?.apply {
            filterView.setOnClickListener {
                navigate(R.id.filterFragment)
            }
        }
    }

    init {
        showBottomSheet = true
    }

    private val whoNeedCache: SharedPreferences =
        appContext.getSharedPreferences("whoNeedShowed", Context.MODE_PRIVATE)

    private fun showWhoNeedSheet() {
        if (context == null) return
        viewLifecycleOwner.lifecycleScope.launch {
            delay(500)
            if (isVisible.not()) return@launch
            if (context == null) return@launch
            val showed = whoNeedCache.getBoolean("showedFilter", false)
            if (showed) {
                mainActivity()?.requestNotificationPermission()
                return@launch
            }
            val setShowed = {
                whoNeedCache.edit {
                    putBoolean("showedFilter", true)
                }
            }
            val bottomSheet = BottomSheetDialog(requireContext())
            val binding =
                WhoYouNeedBinding.inflate(LayoutInflater.from(requireContext()), null, false)
            bottomSheet.setContentView(binding.root)
            bottomSheet.setCancelable(false)
            bottomSheet.setOnDismissListener {
                viewModel.refresh()
            }
            bottomSheet.show()
            binding.apply {
                kelin.setOnClickListener {
                    FilterViewUtils.updateFilter {
                        nomzodType = KUYOV
                        viewModel.nomzodTuri = nomzodType
                        viewModel.refresh()
                    }
                    bottomSheet.dismiss()
                    setShowed.invoke()
                    showAgeSheet(true)
                }
                kuyov.setOnClickListener {
                    FilterViewUtils.updateFilter {
                        nomzodType = KELIN
                        viewModel.nomzodTuri = nomzodType
                        viewModel.refresh()
                    }
                    bottomSheet.dismiss()
                    setShowed.invoke()
                    showAgeSheet(false)
                }
            }
        }
    }

    private fun showAlertSheet() {
        val showed = whoNeedCache.getBoolean("showedFilter", false)
        if (showed) return
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
        if (context == null) return
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
            title.text = "${if (kelin.not()) "Kelin" else "Kuyov"} yosh chegarasini belgilang"
            ageSlider.labelBehavior = LabelFormatter.LABEL_VISIBLE
            ageSlider.values = listOf(
                MyFilter.filter.yoshChegarasiDan.toFloat().coerceIn(18f, 70f),
                MyFilter.filter.yoshChegarasiGacha.toFloat().coerceIn(18f, 70f)
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
                    lifecycleScope.launch {
                        MyFilter.update()
                    }
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
//        val changed = MyFilter.changedFromDefault()
//        binding?.filterDotView?.apply {
//            visibleOrGone(changed)
//        }
    }

    private fun updateUnReadLabel(unread: Int) {
        val badge = binding?.filterDotView ?: return
        badge.isVisible = unread > 0
    }

    private var unreadValueEventListener: ValueEventListener? = null

    private fun observeUnMessages() {
        updateUnReadLabel(LocalUser.user.unreadMessages)
        unreadValueEventListener = UserRepository.observeUnReadMessages {
            updateUnReadLabel(it)
        }
    }

    override fun viewCreated(bind: SearchFragmentBinding) {
        if (LocalUser.user.hasNomzod.not()) {
            showWhoNeedSheet()
        } else {
            mainActivity()?.requestNotificationPermission()
        }
        postponeEnterTransition()

        bind.apply {
            notifyView.setOnClickListener {
                navigate(R.id.messagesFragment)
            }
            initAdapter()
            //Observe
            observe()
            MyFilter.filterChangeObserver.observe(viewLifecycleOwner) {
                viewModel.checkNeedRefresh()
            }
            initFilters()
            initSwipe()
            checkFilterChangedFromDefault()
            tabLayout.getTabAt(
                when (viewModel.filterType) {
                    SearchViewModel.FILTER_FOR_ME -> 0
                    SearchViewModel.FILTER_NEW -> 1
                    else -> 0
                }
            )?.select()
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(p0: TabLayout.Tab?) {

                }

                override fun onTabSelected(p0: TabLayout.Tab?) {
                    val pos = p0?.position ?: return
                    viewModel.applyFilterType(
                        when (pos) {
                            0 -> SearchViewModel.FILTER_FOR_ME
                            1 -> SearchViewModel.FILTER_NEW
                            else -> -1
                        }
                    )
                }

                override fun onTabUnselected(p0: TabLayout.Tab?) {

                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkNeedRefresh()
        observeUnMessages()
        //binding?.adView?.loadAd()
    }

    override fun onPause() {
        super.onPause()
        unreadValueEventListener?.let {
            UserRepository.removeUnreadMessageListener(it)
        }
    }

    private fun observe() {
        binding?.apply {
            var first = true
            viewModel.nomzodlarLive.observe(viewLifecycleOwner) {
                updateList(first)
                checkEmpty()
                if (first) {
                    first = false
                }
            }
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

    private fun initAdapter() {
        binding?.apply {
            recyclerView.apply {
                if (listAdapter == null) {
                    listAdapter = SearchAdapter({
                        viewModel.loadNextNomzodlar()
                    }, {
                        val nomzodId = it.id
                        setFragmentResultListener("deleted") { key, bundle ->
                            viewModel.nomzodlar.removeIf { it.id == nomzodId }
                            viewModel.nomzodlarLive.postVal(viewModel.nomzodlar)
                        }
                        navigate(R.id.nomzodDetailsFragment, Bundle().apply {
                            putString("nomzodId", it.id)
                        })
                    }, onLiked = { liked, id ->
                        val alertDialog = BottomSheetDialog(requireContext(), R.style.SheetStyle)
                        val view = DislikeActionAlertBinding.inflate(layoutInflater, null, false)
                        alertDialog.setContentView(view.root)
                        view.apply {
                            skipButton.setOnClickListener {
                                alertDialog.dismiss()
                            }
                            okButton.setOnClickListener {
                                val nomzod = viewModel.nomzodlar.find { it.id == id }
                                    ?: return@setOnClickListener
                                SearchAdapter.likeOrDislike(nomzod, false)
                                if (!liked) {
                                    viewModel.nomzodlar.removeIf { it.id == id }
                                    viewModel.nomzodlarLive.postVal(viewModel.nomzodlar)
                                }
                                alertDialog.dismiss()
                            }
                        }
                        alertDialog.show()

                    })
                }
                adapter = listAdapter
                setHasFixedSize(true)
                itemAnimator = null
                layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false).also {
                        addOnScrollListener(object : EndlessScrollListener() {
                            override fun onLoadMore(page: Int) : Boolean {
                                return viewModel.loadNextNomzodlar()
                            }
                        })
                    }
            }
        }
    }

    private fun updateList(first: Boolean) {
        binding?.recyclerView?.apply {
            if (first) {
                viewTreeObserver.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        try {
                            viewTreeObserver.removeOnGlobalLayoutListener(this)
                        } catch (e: Exception) {
                            handleException(e)
                        }
                        listAdapter?.submitList(viewModel.nomzodlar)
                        startPostponedEnterTransition()
                    }
                })
            } else {
                listAdapter?.submitList(viewModel.nomzodlar)
                startPostponedEnterTransition()
            }
        }
    }

    private fun checkEmpty() {
        lifecycleScope.launch {
            val loading = viewModel.nomzodlarLoading.value
            if (viewModel.nomzodlar.isEmpty() && loading != true) {
                binding?.emptyView?.isVisible = true
            }
        }
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