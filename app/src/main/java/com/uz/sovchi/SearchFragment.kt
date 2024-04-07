package com.uz.sovchi

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.uz.sovchi.data.filter.FilterViewUtils
import com.uz.sovchi.data.filter.MyFilter
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.nomzod.KELIN
import com.uz.sovchi.data.nomzod.KUYOV
import com.uz.sovchi.data.nomzod.OilaviyHolati
import com.uz.sovchi.data.recombee.RecombeeDatabase
import com.uz.sovchi.databinding.AgeWhatSheetBinding
import com.uz.sovchi.databinding.SearchFragmentBinding
import com.uz.sovchi.databinding.WhoYouNeedBinding
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.nomzod.NomzodDetailsFragment
import com.uz.sovchi.ui.search.SearchAdapter
import com.uz.sovchi.ui.search.SearchViewModel

class SearchFragment : BaseFragment<SearchFragmentBinding>() {
    override val layId: Int
        get() = R.layout.search_fragment

    private var listAdapter: SearchAdapter? = null
    private val viewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadNextNomzodlar()
    }

    private fun initFilters() {
        binding?.apply {
            yoshChegarasiView.setOnClickListener {
                navigate(R.id.filterFragment)
            }
            photoButton.isChecked = viewModel.hasPhoto
            photoButton.setOnCheckedChangeListener { buttonView, isChecked ->
                viewModel.hasPhoto = isChecked
                MyFilter.filter.hasPhoto = isChecked
                MyFilter.update()
                viewModel.refresh()
            }
            updateYoshChegarasi()
            FilterViewUtils.apply {
                setLocationView(locationFilter.editText as AutoCompleteTextView) {
                    viewModel.apply {
                        searchLocation = MyFilter.filter.manzil
                        refresh()
                    }
                }
                setNomzodTypeView(typeFilter.editText as AutoCompleteTextView) {
                    viewModel.apply {
                        nomzodTuri = MyFilter.filter.nomzodType
                        refresh()
                    }
                }
            }
        }

        RecombeeDatabase.getRecommendForUser(
            "",
            KELIN,
            City.Toshkent.name,
            userViewModel.user.uid,
            OilaviyHolati.Aralash.name,
            0,
            0,
            6
        ) { rec, list ->

        }
    }

    private val whoNeedCache: SharedPreferences =
        appContext.getSharedPreferences("whoNeedShowed", Context.MODE_PRIVATE)

    @SuppressLint("SetTextI18n")
    private fun updateYoshChegarasi() {
        binding?.yoshChegarasiView?.apply {
            val has = MyFilter.filter.yoshChegarasi > 0
            if (has) {
                text = "${getString(R.string.yosh_chegarasi)}: ${MyFilter.filter.yoshChegarasi}"
            }
            isVisible = has
        }
    }

    private fun showWhoNeedSheet() {
        val showed = whoNeedCache.getBoolean("showed", false)
        if (showed) return
        val setShowed = {
            whoNeedCache.edit {
                putBoolean("showed", true)
            }
        }
        val bottomSheet = BottomSheetDialog(requireContext())
        val binding = WhoYouNeedBinding.inflate(LayoutInflater.from(requireContext()), null, false)
        bottomSheet.setContentView(binding.root)
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

    private fun showAgeSheet(kelin: Boolean) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val binding =
            AgeWhatSheetBinding.inflate(LayoutInflater.from(requireContext()), null, false)
        bottomSheet.setContentView(binding.root)
        bottomSheet.show()
        binding.apply {
            title.text = "${if (kelin) "Kelin" else "Kuyov"} nechi yoshgacha bo'lsin?"
            yoshChegarasiView.editText?.showKeyboard()
            save.setOnClickListener {
                val age = yoshChegarasiView.editText?.text?.toString()?.toIntOrNull() ?: 0
                if (age > 17) {
                    FilterViewUtils.updateFilter {
                        yoshChegarasi = age
                        viewModel.yoshChegarasi = age
                        viewModel.refresh()
                        updateYoshChegarasi()
                    }
                }
                bottomSheet.dismiss()
            }
            keyin.setOnClickListener {
                bottomSheet.dismiss()
            }
        }
    }

    private fun checkFilterChangedFromDefault() {
        val changed = MyFilter.changedFromDefault()
        binding?.filterDotView?.visibleOrGone(changed)
    }

    override fun viewCreated(bind: SearchFragmentBinding) {
        showBottomSheet = true
        showWhoNeedSheet()
        bind.apply {
            listAdapter = SearchAdapter(userViewModel, onClick = {
                NomzodDetailsFragment.navigateToHere(this@SearchFragment, it)
            }, next = {
                viewModel.loadNextNomzodlar()
            }, onLiked = { liked, nomzodId ->
                mainActivity()?.showSnack(if (liked) "Yoqtirganlarga qo'shildi!" else "Yoqtirganlardan olib tashlandi")
            })
            recyclerView.apply {
                adapter = listAdapter
                layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            }
            filterView.setOnClickListener {
                navigate(R.id.filterFragment)
            }
            viewModel.nomzodlarLive.observe(viewLifecycleOwner) {
                listAdapter?.submitList(it)
            }
            imkonChekTextView.visibleOrGone(MyFilter.filter.imkonChek)
            viewModel.nomzodlarLoading.observe(viewLifecycleOwner) {
                val listEmpty = viewModel.nomzodlar.isEmpty()
                progressBar.visibleOrGone(it && listEmpty)
                emptyView.apply {
                    visibleOrGone(listEmpty && !it)
                    if (listEmpty) {
                        val shaxar = locationFilter.editText?.text?.toString()
                        var message = "$shaxar shaxrida"
                        if (MyFilter.filter.oilaviyHolati != OilaviyHolati.Aralash.name) {
                            val oilaviyHolati =
                                getString(OilaviyHolati.valueOf(MyFilter.filter.oilaviyHolati).resourceId)
                            message += " ${oilaviyHolati.lowercase()}lar"
                        }
                        if (MyFilter.filter.yoshChegarasi > 0) {
                            message += " ${MyFilter.filter.yoshChegarasi} yoshgacha bo'lgan"
                        }
                        message += " nomzodlar topilmadi, filterni o'zgartiring"
                        text = message
                    }
                }
            }
            initFilters()
            swipeRefresh.setOnRefreshListener {
                viewModel.refresh()
                swipeRefresh.isRefreshing = false
            }
            viewModel.checkNeedRefresh()
            checkFilterChangedFromDefault()
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