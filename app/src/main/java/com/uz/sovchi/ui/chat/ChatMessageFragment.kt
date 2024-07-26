package com.uz.sovchi.ui.chat

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.uz.sovchi.R
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.databinding.BlockAlertBinding
import com.uz.sovchi.databinding.ChatMessageFragmentBinding
import com.uz.sovchi.loadAd
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.photo.PickPhotoFragment
import com.uz.sovchi.visibleOrGone

class ChatMessageFragment : BaseFragment<ChatMessageFragmentBinding>() {

    override val layId: Int
        get() = R.layout.chat_message_fragment

    private var nomzodId: String? = null
    private var chatId: String? = null

    private val viewModel: ChatMessagesViewModel by viewModels()

    private val adapterL: ChatMessagesAdapter by lazy {
        ChatMessagesAdapter().apply {
            loadNext = {
                viewModel.loadMessages()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            nomzodId = it.getString("id") ?: ""
            chatId = it.getString("chatId")
            viewModel.nomzodId = nomzodId!!
            viewModel.nomzodPhoto = it.getString("photo", "")
            viewModel.nomzodName = it.getString("name", "")
        }
        if (chatId.isNullOrEmpty().not()) {
            viewModel.loadByChatId(chatId!!)
        } else {
            if (nomzodId.isNullOrEmpty().not()) {
                viewModel.loadByNomzodId(nomzodId!!)
            }
        }
    }

    private fun setBlockedYou(me: Boolean) {
        binding?.apply {
            progressBar.visibleOrGone(false)
            blockedText.isVisible = true
            constraintLayout2.isVisible = false
            sendButton.isVisible = false
            imageUploadButton.visibleOrGone(false)
            infoView.isVisible = false
            if (me) {
                blockedText.text = "Siz bloklagansiz, boshqa yozolmaysiz"
            }
        }
    }

    private fun setYouBlocked() {

    }

    private fun blockUser() {
        viewModel.blockUser()
        closeFragment()
    }

    private fun showBlockAlert() {
        val binding = BlockAlertBinding.inflate(layoutInflater, null, false)
        val dialog = BottomSheetDialog(requireContext(), R.style.SheetStyle)
        dialog.setContentView(binding.root)
        binding.apply {
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
            okButton.setOnClickListener {
                dialog.dismiss()
                blockUser()
            }
        }
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.setUnreadCountZero()
    }

    private fun openDetails() {
        navigate(R.id.nomzodDetailsFragment, Bundle().apply {
            putString("nomzodId", nomzodId)
        })
    }

    override fun onResume() {
        super.onResume()
        binding?.adView?.loadAd()
    }

    private fun send() {
        if (MyNomzodController.nomzod.id.isEmpty()) {
            navigate(R.id.addNomzodFragment)
        }
        val text = binding?.editText?.text?.toString() ?: return
        binding?.editText?.text = null
        if (text.trim().isEmpty()) return
        viewModel.sendMessage(text.trim(), "")
    }

    private fun initView() {
        binding?.recyclerView?.apply {
            adapter = adapterL
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)
        }
        binding?.sendButton?.setOnClickListener {
            send()
        }
        binding?.imageUploadButton?.setOnClickListener {
            PickPhotoFragment(multiple = false) {
                if (it.isEmpty()) return@PickPhotoFragment
                viewModel.sendMessage("", it.first().path)
            }.open(mainActivity()!!)
        }
    }

    fun observe() {
        viewModel.loadingMessages.observe(viewLifecycleOwner) {
            binding?.progressBar?.isVisible = it && adapterL.itemCount == 0
        }
        viewModel.loadingAll.observe(viewLifecycleOwner) {
            binding?.apply {
                progressBar.isVisible = it
            }
        }
        viewModel.messages.observe(viewLifecycleOwner) {
            adapterL.submitList(it)
            checkScrollToLastMessage()
        }
        viewModel.blocked.observe(viewLifecycleOwner) {
            if (it) {
                setBlockedYou(viewModel.meBlocked)
            }
        }
        viewModel.chatModel.observe(viewLifecycleOwner) {
            if (it != null) {
                binding?.apply {
                    Glide.with(imageView).load(it.userImage).into(imageView)
                    titleView.text = it.userName
                }
            }
        }
        viewModel.lastOnlineDate.observe(viewLifecycleOwner) {
            binding?.lastSeenView?.text = it
        }
    }

    private fun scrollToLastMessage() {
        binding?.recyclerView?.apply {
            post {
                smoothScrollToPosition(0)
            }
        }
    }

    private fun checkScrollToLastMessage() {
        if (binding == null) return
        val layoutManager = binding?.recyclerView?.layoutManager as LinearLayoutManager
        if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
            scrollToLastMessage()
        }
    }

    private fun setTemp() {
        binding?.apply {
            Glide.with(imageView).load(viewModel.nomzodPhoto).into(imageView)
            titleView.text = viewModel.nomzodName
        }
    }

    override fun viewCreated(bind: ChatMessageFragmentBinding) {
        bind.apply {
            backButton.setOnClickListener {
                closeFragment()
            }
            initView()
            observe()
            setTemp()
            editText.doOnTextChanged { text, start, before, count ->
                val message = text.toString()
                sendButton.isVisible = message.isNotEmpty()
                imageUploadButton.isVisible = message.isEmpty()
            }
            toolbar.setOnClickListener {
                openDetails()
            }
            blockView.setOnClickListener {
                showBlockAlert()
            }
        }
    }
}