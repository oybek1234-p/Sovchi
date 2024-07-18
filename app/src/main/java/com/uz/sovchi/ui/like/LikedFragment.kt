package com.uz.sovchi.ui.like

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.like.LikeState
import com.uz.sovchi.databinding.LikedFragmentBinding
import com.uz.sovchi.ui.base.BaseFragment

class LikedFragment : BaseFragment<LikedFragmentBinding>() {

    override val layId: Int
        get() = R.layout.liked_fragment

    private var likeAdapter: LikeAdapter? = null

    private var type = -1
    private val viewModel: LikeViewModel by viewModels()

    private var selectedTabPos = 0

    override fun onDestroyView() {
        selectedTabPos = binding?.tabLayout?.selectedTabPosition ?: 0
        super.onDestroyView()
    }

    fun setType(newType: Int) {
        if (type != newType) {
            type = newType
            likeAdapter?.type = newType
            viewModel.setType(type)
        }
    }

    override fun onResume() {
        super.onResume()
        if (selectedTabPos != binding?.tabLayout?.selectedTabPosition) {
            binding?.tabLayout?.getTabAt(selectedTabPos)?.select()
        }
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
                        navigate(R.id.chatMessageFragment, Bundle().apply {
                            putString("id", nom!!.id)
                            putString("name", nom.name)
                            putString("photo", nom.photos.firstOrNull() ?: "")
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
                        if ((type == LikeState.LIKED_ME || type == LikeState.DISLIKED || type == LikeState.LIKED) && LocalUser.user.premium.not()) {
                            bind.seeLiked.isVisible = true
                            bind.emptyView.isVisible = false
                        } else {
                            bind.emptyView.text = getString(R.string.not_found_users)
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
            tabLayout.isSaveEnabled = true
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
        }
    }
}