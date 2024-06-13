package com.uz.sovchi.ui.verification

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.sovchi.R
import com.uz.sovchi.databinding.AdminVerifyFragmentBinding
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.nomzod.NomzodViewModel
import com.uz.sovchi.ui.search.SearchViewModel

class AdminVerificationFragment : BaseFragment<AdminVerifyFragmentBinding>() {

    override val layId: Int
        get() = R.layout.admin_verify_fragment

    private val searchViewModel: SearchViewModel by viewModels()
    private val nomzodViewModel: NomzodViewModel by activityViewModels()
    
    private var vAdapter = VerifyNomzodAdapter({ n ->
        nomzodViewModel.repository.verify(n)
        searchViewModel.nomzodlar.removeIf { it.id == n.id }
        searchViewModel.nomzodlarLive.postValue(searchViewModel.nomzodlar)
    }, {
        searchViewModel.loadNextNomzodlar()
    }, {
        navigate(R.id.addNomzodFragment, Bundle().apply {
            putString("nId", it.id)
            putBoolean("admin", true)
        })
    },{model->
        nomzodViewModel.repository.deleteNomzod(model.id)
        searchViewModel.nomzodlar.removeIf { it.id == model.id }
        searchViewModel.nomzodlarLive.postValue(searchViewModel.nomzodlar)
    })

    override fun viewCreated(bind: AdminVerifyFragmentBinding) {
        bind.apply {
            searchViewModel.mainFilter = SearchViewModel.FILTER_NEW
            searchViewModel.forVerify = true
            searchViewModel.nomzodlarLoading.observe(viewLifecycleOwner) {
                progressBar.isVisible = it && vAdapter.itemCount == 0
            }
            searchViewModel.nomzodlarLive.observe(viewLifecycleOwner) {
                vAdapter.submitList(it)
            }
            recyclerView.apply {
                adapter = vAdapter
                layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            }
            searchViewModel.loadNextNomzodlar()
        }
    }
}