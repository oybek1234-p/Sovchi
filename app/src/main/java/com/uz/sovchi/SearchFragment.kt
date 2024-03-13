package com.uz.sovchi

import android.content.Context
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.sovchi.data.filter.FilterViewUtils
import com.uz.sovchi.data.filter.MyFilter
import com.uz.sovchi.data.nomzod.OilaviyHolati
import com.uz.sovchi.databinding.SearchFragmentBinding
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
    }

    private fun checkFilterChangedFromDefault() {
        val changed = MyFilter.changedFromDefault()
        binding?.filterDotView?.visibleOrGone(changed)
    }

    override fun viewCreated(bind: SearchFragmentBinding) {
        showBottomSheet = true
        bind.apply {
            listAdapter = SearchAdapter(onClick = {
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
            onItemClickListener =
                AdapterView.OnItemClickListener()
                { _, _, position, id ->
                    click.invoke(position)
                }
        }
    }
}