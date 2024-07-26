package com.uz.sovchi.ui.like

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.like.LikeState
import com.uz.sovchi.databinding.LikedFragmentBinding
import com.uz.sovchi.showToast
import com.uz.sovchi.ui.base.BaseFragment

class LikedFragment : BaseFragment<LikedFragmentBinding>() {

    override val layId: Int
        get() = R.layout.liked_fragment

    private var likeAdapter: LikeAdapter? = null
    private val viewModel: LikeViewModel by activityViewModels()

    override fun onDestroyView() {
        viewModel.selectedTabPos = binding?.tabLayout?.selectedTabPosition ?: 0
        super.onDestroyView()
    }

    fun setType(newType: Int) {
            likeAdapter?.type = newType
            viewModel.setType(newType)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setType(LikeState.LIKED_ME)
    }

    override fun viewCreated(bind: LikedFragmentBinding) {
        showBottomSheet = true
        bind.apply {
            if (likeAdapter == null) {
                likeAdapter = LikeAdapter {
                    viewModel.loadNext()
                }.apply {
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
            seeLiked.setOnClickListener {
                mainActivity()?.showPremiumSheet()
            }
            viewModel.apply {
                loading.observe(viewLifecycleOwner) {
                    progressBar.isVisible = it && viewModel.allList.isEmpty()
                    val empty = allList.isEmpty() && it.not()
                    bind.emptyView.isVisible = empty
                    bind.seeLiked.isVisible = false
                    if (empty) {
                        bind.emptyView.text = when(viewModel.typeTab()) {
                            LikeState.LIKED_ME-> "Sizga hali hech kim so'rov yubormagan"
                            LikeState.MATCH -> "Ro'yxat bo'sh"
                            LikeState.LIKED ->  "Ro'yxat bo'sh"
                            LikeState.DISLIKED-> "Ro'yxat bo'sh"
                            else -> ""
                        }
                    }
                }
                allListLive.observe(viewLifecycleOwner) {
                    likeAdapter?.submitList(it)
                }
            }
            recyclerView.apply {
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
                    setType(type)
                }

                override fun onTabUnselected(p0: TabLayout.Tab?) {

                }
            })
            if (viewModel.selectedTabPos != binding?.tabLayout?.selectedTabPosition) {
                binding?.tabLayout?.getTabAt(viewModel.selectedTabPos)?.select()
            }
        }
    }
}