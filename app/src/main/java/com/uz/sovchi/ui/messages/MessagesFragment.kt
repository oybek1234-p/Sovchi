package com.uz.sovchi.ui.messages

import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.databinding.MessagesFragmentBinding
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.nomzod.NomzodViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MessagesFragment : BaseFragment<MessagesFragmentBinding>() {

    override val layId: Int
        get() = R.layout.messages_fragment

    private val viewModel: MessagesViewModel by viewModels()
    private val nomzodViewModel: NomzodViewModel by viewModels()

    private val listAdapter by lazy { MessagesAdapter(this, {
        viewModel.loadMessages()
    }, nomzodRepository = nomzodViewModel.repository) }

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
        lifecycleScope.launch {
            delay(500)
            binding?.recyclerView?.scrollToPosition(0)
        }
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
        val unread = LocalUser.user.unreadMessages
        if (unread > 0) {
            viewModel.refresh()
        } else {
            viewModel.loadMessages()
        }
        userViewModel.repository.setUnreadZero()
    }

    override fun viewCreated(bind: MessagesFragmentBinding) {
        bind.apply {
            toolbar.showArrowBack = true
            toolbar.setUpBackButton(this@MessagesFragment)
            observe()
            initRecycler()
            initSwipeRefresh()
        }
    }
}