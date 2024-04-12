package com.uz.sovchi.ui.messages

import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.sovchi.R
import com.uz.sovchi.databinding.MessagesFragmentBinding
import com.uz.sovchi.ui.base.BaseFragment

class MessagesFragment : BaseFragment<MessagesFragmentBinding>() {

    override val layId: Int
        get() = R.layout.messages_fragment

    private val viewModel: MessagesViewModel by viewModels()
    private val listAdapter = MessagesAdapter(this)

    private fun initSwipeRefresh() {
        binding?.swipeRefresh?.apply {
            setOnRefreshListener {
                isRefreshing = false
                viewModel.refresh()
            }
        }
    }

    private fun initRecycler() {
        binding?.recyclerView?.adapter = listAdapter
        binding?.recyclerView?.itemAnimator = null
        binding?.recyclerView?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    override fun onPause() {
        super.onPause()
        mainActivity()?.unreadMessageChangedListener = null
    }

    private fun observe() {
        viewModel.apply {
            binding?.apply {
                messages.observe(viewLifecycleOwner) { it ->
                    listAdapter.submitList(it.sortedByDescending { it.date })
                }
                loading.observe(viewLifecycleOwner) {
                    progressBar.isVisible = viewModel.messagesList.size == 0 && it
                }
                empty.observe(viewLifecycleOwner) {
                    emptyView.isVisible = it
                }
            }
        }
    }

    init {
        showBottomSheet = true
    }

    override fun onResume() {
        super.onResume()
        userViewModel.repository.setUnreadZero()
        mainActivity()?.unreadMessageChangedListener = {
            viewModel.refresh()
        }
    }

    override fun viewCreated(bind: MessagesFragmentBinding) {
        bind.apply {
            toolbar.showArrowBack = false
            observe()
            initRecycler()
            initSwipeRefresh()
            viewModel.loadMessages()
        }
    }
}