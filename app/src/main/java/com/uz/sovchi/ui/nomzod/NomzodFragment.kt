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
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.visibleOrGone
import kotlinx.coroutines.launch

class NomzodFragment : BaseFragment<NomzodFragmentBinding>() {

    override val layId: Int
        get() = R.layout.nomzod_fragment

    private var searchAdapter: MyNomzodAdapter? = null

    private val viewModel: NomzodViewModel by activityViewModels()

    private fun loadMyNomzods() {
        binding?.emptyView?.visibleOrGone(false)
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
        if (nomzods.isEmpty()) {
            binding?.emptyView?.visibleOrGone(userViewModel.user.valid)
        }
    }

    override fun viewCreated(bind: NomzodFragmentBinding) {
        showBottomSheet = true
        bind.apply {
            val authed = userViewModel.user.valid
            authView.apply {
                authButton.setOnClickListener {
                    findNavController().navigate(R.id.authFragment)
                }
                boglanishButton.setOnClickListener {
                    mainActivity()?.showSupportSheet()
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
                searchAdapter = MyNomzodAdapter({
                    val bundle = Bundle().apply {
                        putString("value", it.id)
                        putString("tarif", it.tarif)
                        putInt("type",it.type)
                    }
                    navigate(R.id.paymentGetCheckFragment, bundle)
                }, loadNext = {

                }, { nomzod, setings ->
                    if (!setings) {
                        navigate(R.id.nomzodDetailsFragment, Bundle().apply {
                            putString("nomzodId", nomzod.id)
                        })
                    } else {
                        navigate(R.id.settingsFragment, Bundle().apply {
                            putString("nomzodId", nomzod.id)
                        })
                    }
                }).also {
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
        binding?.addNomzodButton?.visibleOrGone(showAddButton || LocalUser.user.phoneNumber == "+998971871415")
    }
}