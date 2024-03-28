package com.uz.sovchi.ui.nomzod

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.valid
import com.uz.sovchi.databinding.NomzodFragmentBinding
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.search.SearchAdapter
import com.uz.sovchi.visibleOrGone
import kotlinx.coroutines.launch

class NomzodFragment : BaseFragment<NomzodFragmentBinding>() {

    override val layId: Int
        get() = R.layout.nomzod_fragment

    private var searchAdapter: SearchAdapter? = null

    private val viewModel: NomzodViewModel by activityViewModels()

    private fun loadMyNomzods() {
        lifecycleScope.launch {
            val nomzods: List<Nomzod> = viewModel.repository.myNomzods
            if (nomzods.isEmpty()) {
                val result = viewModel.repository.loadMyNomzods()
                nomzodsLoaded(result)
            } else {
                nomzodsLoaded(nomzods)
            }
        }
    }

    private fun nomzodsLoaded(nomzods: List<Nomzod>) {
        searchAdapter?.submitList(nomzods)
    }

    override fun viewCreated(bind: NomzodFragmentBinding) {
        showBottomSheet = true
        bind.apply {
            val authed = userViewModel.user.valid
            authView.apply {
                authButton.setOnClickListener {
                    findNavController().navigate(R.id.auth_graph)
                }
                root.visibleOrGone(authed.not())
            }
            if (!authed) {
                addNomzodButton.visibleOrGone(false)
                return
            }
            val openAddNomzodFragment: (id: String) -> Unit = {
                navigate(R.id.addNomzodFragment, Bundle().apply {
                    putString("nId", it)
                })
            }
            recyclerView.apply {
                searchAdapter = SearchAdapter(userViewModel,onClick = {
                    openAddNomzodFragment.invoke(it.id)
                }, next = {}).also {
                    adapter = it
                }
                layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            }
            viewModel.repository.myNomzodsLoading.observe(viewLifecycleOwner) {
                progressBar.visibleOrGone(it)
                updateAddButton()
            }
            addNomzodButton.setOnClickListener {
                openAddNomzodFragment.invoke("")
            }
            loadMyNomzods()
            updateAddButton()
        }
    }

    private fun updateAddButton() {
        val showAddButton =
            searchAdapter?.currentList?.size == 0 && viewModel.repository.myNomzodsLoading.value == false
        binding?.emptyView?.visibleOrGone(showAddButton)
        binding?.addNomzodButton?.visibleOrGone(showAddButton || LocalUser.user.phoneNumber == "+998971871415")
    }
}