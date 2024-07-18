package com.uz.sovchi.ui.chat

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.databinding.ChatFragmentBinding
import com.uz.sovchi.loadAd
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseFragment

class ChatFragment : BaseFragment<ChatFragmentBinding>() {

    override val layId: Int
        get() = R.layout.chat_fragment

    private fun loadAd() {
        binding?.adView?.loadAd()
    }

    private val adapterChat: ChatsAdapter by lazy { ChatsAdapter().apply {
        click = {
            navigate(R.id.chatMessageFragment, Bundle().apply {
                putString("id",it.userId)
                putString("name",it.userName)
                putString("photo",it.userImage)
                putString("chatId",it.chatId)
            })
        }
    } }

    private val viewModel: ChatsViewModel by viewModels()

    private fun initAdapter() {
        binding?.recyclerView?.apply {
            itemAnimator = null
            adapter = adapterChat
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }

    private fun observe() {
        viewModel.chatsLoading.observe(viewLifecycleOwner) {
            binding?.progressBar?.isVisible = it
            if (it) {
                binding?.emptyView?.isVisible = false
            }
        }
        viewModel.chats.observe(viewLifecycleOwner) {
            adapterChat.submitList(it)
            if (it.isEmpty()) {
                binding?.emptyView?.isVisible = adapterChat.itemCount == 0 && viewModel.chatsLoading.value == false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.startObservingChats()
        if (userViewModel.repository.user.unreadChats > 0) {
            userViewModel.repository.setUnreadChatZero()
        }
        loadAd()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopListeningChats()
    }

    override fun viewCreated(bind: ChatFragmentBinding) {
        bind.apply {
            myToolBar.showArrowBack = false
            showBottomSheet = true
            initAdapter()
            observe()
        }
    }

}