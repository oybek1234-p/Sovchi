package com.uz.sovchi.ui.requests

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.messages.RequestStatus
import com.uz.sovchi.databinding.RequestFragmentBinding
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.visibleOrGone

class RequestsFragment : BaseFragment<RequestFragmentBinding>() {

    override val layId: Int
        get() = R.layout.request_fragment

    private val requestAdapter: RequestAdapter by lazy {
        RequestAdapter(this)
    }

    private val viewModel: RequestsViewModel by activityViewModels()

    override fun viewCreated(bind: RequestFragmentBinding) {
        bind.apply {
            showBottomSheet = true

            recyclerView.adapter = requestAdapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            requestAdapter.apply {
                acceptCallback = {
                    viewModel.requestsRepository.updateRequestStatus(it.id, RequestStatus.accepted)
                    currentList.toMutableList().apply {
                        val id = it.id
                        find { model-> model.id == id }?.status = RequestStatus.accepted
                        submitList(this)
                        notifyDataSetChanged()
                    }
                }
                rejectCallback = {
                    viewModel.requestsRepository.updateRequestStatus(it.id, RequestStatus.rejected)
                    val list = currentList.toMutableList()
                    val id = it.id
                    list.find { it.id == id }?.status = RequestStatus.rejected
                    submitList(list)
                }
                connectCallback = {

                }
                onItemClick = {
                    val nomzod =
                        if (LocalUser.user.uid == it.requestedUserId) it.nomzod else it.requestedNomzod
                    navigate(R.id.nomzodDetailsFragment, Bundle().apply {
                        putString("nomzodId", nomzod.id)
                    })
                }
            }
            viewModel.apply {
                requestsLoading.observe(viewLifecycleOwner) {
                    progressBar.visibleOrGone(it)
                    //    val isMy = binding?.tabLayout?.selectedTabPosition ?: return@observe
                    val emptyVisible = it.not() && viewModel.requests.isEmpty()
                    emptyView.visibleOrGone(emptyVisible)
                }
                requestsLive.observe(viewLifecycleOwner) {
                    requestAdapter.submitList(it)

                }
                getRequests(false)
            }
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(p0: TabLayout.Tab?) {

                }

                override fun onTabSelected(p0: TabLayout.Tab?) {
                    val tab = p0 ?: return
                    val position = tab.position
                    if (position == 0) {
                        viewModel.getRequests(false)
                    } else {
                        viewModel.getRequests(true)
                    }
                }

                override fun onTabUnselected(p0: TabLayout.Tab?) {

                }
            })
        }
    }
}