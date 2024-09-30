package com.uz.sovchi.ui.chat

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.sovchi.R
import com.uz.sovchi.data.UserRepository
import com.uz.sovchi.data.chat.ChatController
import com.uz.sovchi.databinding.ChatFragmentBinding
import com.uz.sovchi.postVal
import com.uz.sovchi.ui.base.BaseFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatFragment : BaseFragment<ChatFragmentBinding>() {

    override val layId: Int
        get() = R.layout.chat_fragment

    private fun loadAd() {
        //  binding?.adView?.loadAd()
        binding?.adView?.isVisible = false
    }

    private val adapterChat: ChatsAdapter by lazy {
        ChatsAdapter().apply {
            click = {
                navigate(R.id.chatMessageFragment, Bundle().apply {
                    putString("id", it.userId)
                    putString("name", it.userName)
                    putString("photo", it.userImage)
                    putString("chatId", it.chatId)
                })
            }
        }
    }

    private val viewModel: ChatsViewModel by viewModels()
    private fun initAdapter() {
        binding?.recyclerView?.apply {
            itemAnimator = null
            adapter = adapterChat
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            setHasFixedSize(true)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observe() {
        viewModel.chatsLoading.observe(viewLifecycleOwner) {
            binding?.progressBar?.isVisible = it && adapterChat.currentList.isEmpty()
            if (it) {
                binding?.emptyView?.isVisible = false
            }
        }
        viewModel.apply {
            chatsList.clear()
            chatsList.addAll(ChatController.chats)
            chats.postVal(chatsList)
        }
        postponeEnterTransition()
        var first = true
        var job: Job? = null
        viewModel.chats.observe(viewLifecycleOwner) {
            val update = {
                job?.cancel()
                job = lifecycleScope.launch {
                    delay(15)
                    adapterChat.submitList(it)
                    adapterChat.notifyDataSetChanged()
                    startPostponedEnterTransition()
                    if (it.isEmpty()) {
                        binding?.emptyView?.isVisible =
                            adapterChat.itemCount == 0 && viewModel.chatsLoading.value == false
                    } else {
                        binding?.emptyView?.isVisible = false
                    }
                }
            }
            if (first) {
                first = false
                binding?.recyclerView?.apply {
                    viewTreeObserver.addOnGlobalLayoutListener(object :
                        ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            viewTreeObserver.removeOnGlobalLayoutListener(this)
                            update.invoke()
                        }
                    })
                }
            } else {
                update.invoke()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (UserRepository.user.unreadChats > 0) {
            UserRepository.setUnreadChatZero()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.startObservingChats()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopListeningChats()
    }

    override fun viewCreated(bind: ChatFragmentBinding) {
        bind.apply {
            showBottomSheet = true
            initAdapter()
            observe()
            loadAd()
        }
    }

}