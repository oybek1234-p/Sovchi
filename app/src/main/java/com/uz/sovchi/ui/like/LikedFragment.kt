package com.uz.sovchi.ui.like

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.like.LikeController
import com.uz.sovchi.data.like.LikeState
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.data.valid
import com.uz.sovchi.databinding.LikedFragmentBinding
import com.uz.sovchi.postVal
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LikedFragment : BaseFragment<LikedFragmentBinding>() {

    override val layId: Int
        get() = R.layout.liked_fragment

    private var likeAdapter: LikeAdapter? = null
    private val viewModel: LikeViewModel by activityViewModels()

    private var likeType = LikeState.LIKED_ME

    override fun onDestroyView() {
        viewModel.selectedTabPos = binding?.tabLayout?.selectedTabPosition ?: 0
        super.onDestroyView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        likeType = arguments?.getInt("type") ?: LikeState.LIKED_ME
    }

    fun setType(newType: Int) {
        likeAdapter?.type = newType
        viewModel.applyType(newType)
    }

    private var likesCount = LocalUser.user.liked

    override fun viewCreated(bind: LikedFragmentBinding) {
        showBottomSheet = likeType == LikeState.LIKED_ME
        bind.apply {
            recyclerView.itemAnimator = null
            if (likeAdapter == null) {
                likeAdapter = LikeAdapter({ like, nomzod ->
                    if (LocalUser.user.valid.not()) return@LikeAdapter
                    if (LocalUser.user.hasNomzod.not() || MyNomzodController.nomzod.id.isEmpty()) {
                        navigate(R.id.addNomzodFragment)
                        return@LikeAdapter
                    }
                    LikeController.likeOrDislikeNomzod(
                        LocalUser.user.uid,
                        nomzod,
                        if (like) LikeState.LIKED else LikeState.DISLIKED
                    )
                    viewModel.removeNomzod(nomzod)
                    viewLifecycleOwner.lifecycleScope.launch {
                        if (likeAdapter?.currentList?.size == 0) {
                            viewModel.loadNext()
                        }
                    }
                    if (like) {
                        navigate(R.id.matchedFragment, Bundle().apply {
                            putString("nomzodId", nomzod.id)
                            putString("nomzodPhoto", nomzod.photos.firstOrNull())
                        })
                    }
                }) {
                    viewModel.loadNext()
                }.apply {
                    this.type = likeType
                    onChatClick = { nom ->
                        val nomzod = nom!!
                        navigate(R.id.chatMessageFragment, Bundle().apply {
                            putString("id", nomzod.id)
                            putString("name", nomzod.name)
                            putString("photo", nomzod.photos.firstOrNull() ?: "")
                        })
                    }
                    onClick = { nom ->
                        navigate(R.id.nomzodDetailsFragment, Bundle().apply {
                            putString("nomzodId", nom!!.id)
                        })
                    }
                }
            }
            if (likeType == LikeState.LIKED_ME) {
                tabLayout.removeTabAt(1)
            } else {
                tabLayout.removeTabAt(0)
            }
            seeLiked.setOnClickListener {
                if (LocalUser.user.valid.not()) {
                    navigate(R.id.authFragment)
                    return@setOnClickListener
                }
                mainActivity()?.showPremiumSheet()
            }
            LocalUser.userLive.observe(viewLifecycleOwner) {
                if (it.liked > likesCount) {
                    likesCount = it.liked
                    if (viewModel.allList.isEmpty() && viewModel.loading.value == false) {
                        viewModel.loadNext()
                    } else {
                        viewModel.loadNew()
                    }
                }
            }
            viewModel.apply {
                loading.observe(viewLifecycleOwner) {
                    progressBar.isVisible = it && viewModel.allList.isEmpty()
                    if (it) {
                        emptyView.isVisible = false
                    }
                }
                allListLive.observe(viewLifecycleOwner) {
                    likeAdapter?.submitList(it.distinctBy { it.id })

                    if (newAdded) {
                        newAdded = false
                        viewLifecycleOwner.lifecycleScope.launch {
                            delay(100)
                            recyclerView.smoothScrollToPosition(0)
                        }
                    }
                    val empty = it.isEmpty() && viewModel.loading.value == false
                    bind.emptyView.isVisible = empty
                    bind.seeLiked.isVisible = false
                    if (empty) {
                        bind.emptyText.text = when (viewModel.typeTab()) {
                            LikeState.LIKED_ME -> "Sizga yuborilgan so'rovlar shu yerda ko'rinadi"
                            LikeState.MATCH -> "Ro'yxat bo'sh"
                            LikeState.LIKED -> "Ro'yxat bo'sh"
                            LikeState.DISLIKED -> "Ro'yxat bo'sh"
                            else -> ""
                        }
                    }
                }
            }
            recyclerView.apply {
                itemAnimator = DefaultItemAnimator()
                adapter = likeAdapter
                layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            }
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(p0: TabLayout.Tab?) {

                }

                override fun onTabSelected(p0: TabLayout.Tab?) {
                    val position = p0?.position ?: return

                    val type = when (position) {
                        0 -> LikeState.LIKED_ME
                        1 -> LikeState.MATCH
                        2 -> LikeState.LIKED
                        3 -> LikeState.DISLIKED
                        else -> return
                    }
                    if ((type == LikeState.LIKED || type == LikeState.DISLIKED) && LocalUser.user.premium.not()) {
                        seeLiked.isVisible = true
                        progressBar.isVisible = false
                        viewModel.allList.clear()
                        viewModel.allListLive.postVal(viewModel.allList)
                        emptyView.isVisible = false
                        viewModel.stopLoading()
                        viewModel.type = type
                        return
                    } else {
                        seeLiked.isVisible = false
                    }
                    setType(type)
                    showToast("On tab")
                }

                override fun onTabUnselected(p0: TabLayout.Tab?) {

                }
            })
            if (viewModel.selectedTabPos != binding?.tabLayout?.selectedTabPosition) {
                binding?.tabLayout?.getTabAt(viewModel.selectedTabPos)?.select()
            }
            setType(likeType)
        }
    }
}