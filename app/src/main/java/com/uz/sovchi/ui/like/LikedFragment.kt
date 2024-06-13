package com.uz.sovchi.ui.like

import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.like.LikeState
import com.uz.sovchi.data.valid
import com.uz.sovchi.databinding.LikedFragmentBinding
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.nomzod.NomzodDetailsFragment

class LikedFragment : BaseFragment<LikedFragmentBinding>() {

    override val layId: Int
        get() = R.layout.liked_fragment

    private var likeAdapter: LikeAdapter? = null

    private var type = -1
    private val viewModel: LikeViewModel by viewModels()

    fun setType(newType: Int) {
        if (type != newType) {
            type = newType
            viewModel.setType(type)
        }
    }

    override fun viewCreated(bind: LikedFragmentBinding) {
        showBottomSheet = true

        if (LocalUser.user.valid.not()) return
        bind.apply {
            if (likeAdapter == null) {
                likeAdapter = LikeAdapter {
                    viewModel.loadNext()
                }.apply {
                    onClick = { nom->
                        NomzodDetailsFragment.navigateToHere(this@LikedFragment, nom!!, false)
                    }
                }
            }
            viewModel.apply {
                loading.observe(viewLifecycleOwner) {
                    progressBar.isVisible = it && allList.isEmpty()
                    val empty = allList.isEmpty() && it.not()
                    bind.emptyView.isVisible = empty
                    if (empty) {
                        bind.emptyView.text = getString(R.string.not_found_users)
                    }
                }
                allListLive.observe(viewLifecycleOwner) {
                    likeAdapter?.submitList(it.map { it.nomzod })
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
                        0 -> LikeState.LIKED
                        1 -> LikeState.LIKED_ME
                        2 -> LikeState.MATCH
                        3 -> LikeState.DISLIKED
                        else -> return
                    }
                    setType(type)
                }

                override fun onTabUnselected(p0: TabLayout.Tab?) {

                }
            })
            setType(LikeState.LIKED)
        }
    }
}