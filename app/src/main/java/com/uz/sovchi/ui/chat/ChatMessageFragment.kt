package com.uz.sovchi.ui.chat

import android.app.AlertDialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.REQUEST_MAX
import com.uz.sovchi.data.UserRepository
import com.uz.sovchi.data.chat.ChatController
import com.uz.sovchi.data.chat.ChatMessageModel
import com.uz.sovchi.data.nomzod.KELIN
import com.uz.sovchi.data.nomzod.KUYOV
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.data.nomzod.Nomzod
import com.uz.sovchi.data.nomzod.NomzodState
import com.uz.sovchi.data.utils.DateUtils
import com.uz.sovchi.databinding.BlockAlertBinding
import com.uz.sovchi.databinding.ChatMessageFragmentBinding
import com.uz.sovchi.databinding.MessageDeleteSheetBinding
import com.uz.sovchi.databinding.RejectInfoSheetBinding
import com.uz.sovchi.handleException
import com.uz.sovchi.hideSoftInput
import com.uz.sovchi.loadPhoto
import com.uz.sovchi.showKeyboard
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.photo.PickPhotoFragment
import com.uz.sovchi.visibleOrGone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            onChatClick = {
                showDeleteMessage(it)
            }
        }
    }

    private fun showDeleteMessage(message: ChatMessageModel) {
        activity?.let {
            hideSoftInput(it)
        }
        val sheet = AlertDialog.Builder(requireContext(), R.style.RoundedCornersDialog)
        val binding = MessageDeleteSheetBinding.inflate(layoutInflater, null, false)
        sheet.setView(binding.root)
        val alert = sheet.create()
        binding.apply {
            textview.text = message.message
            deleteButton.setOnClickListener {
                viewModel.deleteMessage(message)
                alert.dismiss()
            }
            undoButton.setOnClickListener {
                alert.dismiss()
            }
        }
        alert.show()
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
        viewModel.loadByNomzodId(nomzodId!!)
    }

    private fun onBlocked() {
        binding?.apply {
            progressBar.visibleOrGone(false)
            blockedText.isVisible = true
            constraintLayout2.isVisible = false
            sendButton.isVisible = false
            blockView.isVisible = true
            imageUploadButton.visibleOrGone(false)
            infoView.isVisible = false
            emptyView.isVisible = false
            recyclerView.isVisible = false

            if (viewModel.meBlocked) {
                blockedText.text = "Siz blokladingiz, blokdan ochish uchun tugmani bosing"
                unblockButton.text = "Blockdan ochish"
                unblockButton.setOnClickListener {
                    viewModel.unblockUser()
                }
            } else {
                blockedText.text = "Bu odam sizni blokladi, endi hech nima qilolmaysiz"
                unblockButton.text = "Chatni o'chirish"
                unblockButton.setOnClickListener {
                    blockUser("")
                }
            }
        }
    }

    private fun blockUser(report: String) {
        viewModel.blockUser(report)
        closeFragment()
    }

    private fun showBlockAlert() {
        val binding = BlockAlertBinding.inflate(layoutInflater, null, false)
        val dialog = BottomSheetDialog(requireContext(), R.style.SheetStyle)
        dialog.setContentView(binding.root)
        binding.apply {
            textView2.text = viewModel.chatModel.value?.userName
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
            okButton.setOnClickListener {
                dialog.dismiss()
                showRejectInfoInputSheet {
                    blockUser(it)
                    showToast("Nomzod bloklandi")
                }
            }
        }
        dialog.show()
    }

    private fun openDetails() {
        navigate(R.id.nomzodDetailsFragment, Bundle().apply {
            putString("nomzodId", nomzodId)
        })
    }

    override fun onResume() {
        super.onResume()
        // binding?.adView?.loadAd()
        ChatController.currentOpenedChatNomzodId = nomzodId ?: ""
        binding?.recyclerView?.scrollToPosition(0)
    }

    override fun onPause() {
        super.onPause()
        ChatController.currentOpenedChatNomzodId = ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ChatController.currentOpenedChatNomzodId = ""
    }

    private fun needPremium(): Boolean {
        return LocalUser.user.requests >= REQUEST_MAX && UserRepository.user.premium.not() && MyNomzodController.nomzod.type == KUYOV && viewModel.messagesList.none { it.senderId == LocalUser.user.uid }
    }

    private fun send() {
        if (MyNomzodController.nomzod.id.isEmpty()) {
            navigate(R.id.addNomzodFragment)
            return
        }
        if (UserRepository.user.blocked) {
            showToast("Siz ilovada bloklangansiz! Bizga aloqaga chiqing")
            return
        }
        if (needPremium()) {
            mainActivity()?.showPremiumSheet()
            return
        }
        val text = binding?.editText?.text?.toString() ?: return
        binding?.editText?.text = null
        if (text.trim().isEmpty()) return
        if (activity == null) return
        viewModel.sendMessage(text.trim(), "") {
            if (it.not()) {
                showToast(appContext.getString(R.string.xatolik_yuz_berdi))
            }
        }
    }

    private fun initView() {
        binding?.recyclerView?.apply {
            itemAnimator = null
            adapter = adapterL
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)
            setHasFixedSize(false)
        }
        binding?.sendButton?.setOnClickListener {
            send()
        }

        binding?.emptyView?.setOnClickListener {
            binding?.editText?.showKeyboard()
        }
        binding?.imageUploadButton?.setOnClickListener {
            if (needPremium()) {
                mainActivity()?.showPremiumSheet()
                return@setOnClickListener
            }
            if (MyNomzodController.nomzod.id.isEmpty()) {
                navigate(R.id.addNomzodFragment)
                return@setOnClickListener
            }
            if (MyNomzodController.nomzod.state == NomzodState.CHECKING && viewModel.messagesList.isEmpty()) {
                mainActivity()?.showNotVerifiedAccountDialog()
                return@setOnClickListener
            }
            if (UserRepository.user.blocked) {
                showToast("Siz ilovada bloklangansiz! Bizga aloqaga chiqing")
                return@setOnClickListener
            }
            var messageSent = false
            activity?.let {
                hideSoftInput(it)
            }
            PickPhotoFragment(multiple = false) {
                if (it.isEmpty()) return@PickPhotoFragment
                if (activity == null) return@PickPhotoFragment
                if (messageSent) return@PickPhotoFragment
                viewModel.sendMessage("", it.first().path) {
                    if (it.not()) {
                        showToast(appContext.getString(R.string.xatolik_yuz_berdi))
                    }
                }
                messageSent = true
            }.open(mainActivity()!!)
        }
    }

    fun observe() {
        viewModel.loadingMessages.observe(viewLifecycleOwner) {
            binding?.progressBar?.visibility =
                if (it && adapterL.currentList.isEmpty()) View.VISIBLE else View.INVISIBLE
        }
        viewModel.loadingAll.observe(viewLifecycleOwner) {
            binding?.progressBar?.visibility =
                if (it && adapterL.currentList.isEmpty()) View.VISIBLE else View.INVISIBLE
        }
        var first = true
        ChatController.uploadingPhotosLive.observe(viewLifecycleOwner) {
            if (first) {
                first = false
                return@observe
            }
            binding?.recyclerView?.layoutManager?.apply {
                (this as LinearLayoutManager)
                try {
                    val firstItem = findFirstVisibleItemPosition()
                    val lastItem = findLastVisibleItemPosition()
                    (firstItem..lastItem).forEach {
                        adapterL.notifyItemChanged(it)
                    }
                } catch (e: Exception) {
                    handleException(e)
                }
            }
        }
        if (viewModel.messagesList.isNotEmpty()) {
            postponeEnterTransition()
        }
        var updateJob: Job? = null
        viewModel.messages.observe(viewLifecycleOwner) update@{
            updateJob?.cancel()
            updateJob = lifecycleScope.launch(Dispatchers.Default) {
                val readyList = ArrayList<ChatMessageModel>()
                val list = it.toMutableList()
                var lastDate = DateUtils.parseDateMillis(list.firstOrNull()?.date ?: 0)
                list.forEachIndexed { _, chatMessageModel ->
                    if (isActive.not()) return@launch
                    val todayDate = DateUtils.parseDateMillis(chatMessageModel.date ?: 0)
                    val sameDay = DateUtils.isSameDay(todayDate, lastDate)
                    if (!sameDay) {
                        readyList.add(
                            ChatMessageModel(
                                lastDate.toString(),
                                "",
                                ChatMessagesAdapter.DATE_TYPE,
                                "",
                                "",
                                lastDate,
                                "",
                                "",
                                false
                            )
                        )
                        lastDate = todayDate
                    }
                    if (chatMessageModel.deleted.not()) {
                        readyList.add(chatMessageModel)
                    }
                }
                if (readyList.isNotEmpty()) {
                    readyList.add(
                        ChatMessageModel(
                            lastDate.toString(),
                            "",
                            ChatMessagesAdapter.DATE_TYPE,
                            "",
                            "",
                            lastDate.toString(),
                            "",
                            "",
                            false
                        )
                    )
                }
                withContext(Dispatchers.Main) {
                    if (isActive.not()) return@withContext
                    if (readyList.isNotEmpty()) {
                        binding?.progressBar?.isVisible = false
                    }
                    if (readyList.isNotEmpty()) {
                        binding?.emptyView?.isVisible = false
                    }
                    binding?.emptyView?.isVisible =
                        readyList.isEmpty() && viewModel.blocked.value == false && viewModel.loadingMessages.value == false

                    adapterL.submitList(readyList.toMutableList()) {
                        if (viewModel.newMessageAdded) {
                            viewModel.newMessageAdded = false
                            doOnLayout {
                                checkScrollToLastMessage()
                            }
                        }
                        startPostponedEnterTransition()
                    }
                }
            }
        }
        viewModel.blocked.observe(viewLifecycleOwner) {
            if (it) {
                binding?.optionsView?.isVisible = false
                onBlocked()
            } else {
                binding?.apply {
                    optionsView.isVisible = true
                    blockView.isVisible = false
                    constraintLayout2.isVisible = true
                    recyclerView.isVisible = true
                }
            }
        }
        viewModel.chatModel.observe(viewLifecycleOwner) {
            if (it != null) {
                binding?.apply {
                    imageView.loadPhoto(it.userImage.ifEmpty {
                        if (MyNomzodController.nomzod.type == KELIN) Nomzod.KUYOV_TEXT else Nomzod.KELIN_TEXT
                    })
                    titleView.text =
                        it.userName.ifEmpty { appContext.getString(R.string.o_chirilgan) }
                }
            }
        }
        viewModel.lastOnlineDate.observe(viewLifecycleOwner) {
            binding?.lastSeenView?.apply {
                text = it
                setTextColor(
                    if (it == appContext.getString(R.string.onlayn)) appContext.getColor(R.color.green) else MaterialColors.getColor(
                        this, com.google.android.material.R.attr.colorOnSurfaceVariant
                    )
                )
            }
        }
    }

    private fun doOnLayout(done: () -> Unit) {
        binding?.recyclerView?.viewTreeObserver?.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding?.recyclerView?.viewTreeObserver?.removeOnGlobalLayoutListener(
                    this
                )
                done.invoke()
            }
        })
    }

    private fun scrollToLastMessage() {
        binding?.recyclerView?.apply {
            // Get the last position
            val lastPosition = 0
            // Check if the layout manager is available
            layoutManager?.apply {
                // Custom smooth scroller with adjusted speed
                val smoothScroller = object : LinearSmoothScroller(context) {
                    override fun getVerticalSnapPreference(): Int {
                        return 0 // Ensures smooth alignment to the last item
                    }

                    override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                        // Adjust speed per pixel for extra smoothness
                        return 0.06f
                    }

                    override fun calculateTimeForScrolling(dx: Int): Int {
                        // Increase the time for scrolling for smoother effect
                        return super.calculateTimeForScrolling(dx) * 3
                    }
                }

                // Check if scrolling to the exact position is needed
                smoothScroller.targetPosition = lastPosition

                // Start scrolling to the target position smoothly
                startSmoothScroll(smoothScroller)
            }
        }
    }


    private fun checkScrollToLastMessage() {
        if (binding == null) return
        val layoutManager = binding?.recyclerView?.layoutManager as LinearLayoutManager
        if (layoutManager.findFirstCompletelyVisibleItemPosition() <= 5) {
            scrollToLastMessage()
        }
    }

    private fun setTemp() {
        binding?.apply {
            imageView.loadPhoto(viewModel.nomzodPhoto.ifEmpty {
                if (MyNomzodController.nomzod.type == KELIN) Nomzod.KUYOV_TEXT else Nomzod.KELIN_TEXT
            })
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
            imageUploadButton.isVisible = editText.text.isNullOrEmpty().also {
                sendButton.isVisible = it.not()
            }
            editText.doOnTextChanged { text, start, before, count ->
                val message = text.toString()
                sendButton.isVisible = message.isNotEmpty()
                imageUploadButton.isVisible = message.isEmpty()
            }
            editText.requestFocus()
            toolbar.setOnClickListener {
                openDetails()
            }
            optionsView.setOnClickListener {
                showBlockAlert()
            }
        }
    }


    private fun showRejectInfoInputSheet(result: (string: String) -> Unit) {
        if (context == null) return
        val sheet = BottomSheetDialog(requireContext())
        val binding = RejectInfoSheetBinding.inflate(layoutInflater, null, false)
        sheet.setContentView(binding.root)
        binding.apply {
            send.setOnClickListener {
                val text = binding.editText.text.toString()
                result.invoke(text)
                sheet.dismiss()
            }

            sheet.show()
        }

    }
}