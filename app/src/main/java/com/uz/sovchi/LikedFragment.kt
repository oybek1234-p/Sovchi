package com.uz.sovchi

import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.saved.SavedRepository
import com.uz.sovchi.data.valid
import com.uz.sovchi.databinding.LikedFragmentBinding
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.nomzod.NomzodDetailsFragment
import com.uz.sovchi.ui.search.SearchAdapter

class LikedFragment : BaseFragment<LikedFragmentBinding>() {

    override val layId: Int
        get() = R.layout.liked_fragment

    private var listAdapter: SearchAdapter? = null

    private fun loadSaved() {
        SavedRepository.apply {
            if (savedList.isEmpty()) {
                loadSaved {
                    updateAdapter()
                }
            } else {
                updateAdapter()
            }
        }
    }

    private fun updateAdapter() {
        val emptyView = binding?.emptyView
        val list = SavedRepository.savedList
        listAdapter?.submitList(list.map { it.nomzod })
        emptyView?.visibleOrGone(list.isEmpty() && LocalUser.user.valid)
    }

    override fun viewCreated(bind: LikedFragmentBinding) {
        showBottomSheet = true
        bind.authView.apply {
            root.visibleOrGone(userViewModel.user.valid.not())

            authButton.setOnClickListener {
                findNavController().navigate(R.id.auth_graph)
            }
        }
        if (LocalUser.user.valid.not()) return
        bind.apply {
            listAdapter = SearchAdapter({}, {
                NomzodDetailsFragment.navigateToHere(this@LikedFragment, it)
            }, { _, _ ->
                updateAdapter()
            })
            recyclerView.apply {
                adapter = listAdapter
                layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            }
            SavedRepository.savedLoading.observe(viewLifecycleOwner) {
                progressBar.visibleOrGone(it)
            }
            loadSaved()
        }
    }


}