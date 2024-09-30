package com.uz.sovchi.ui.users

import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.sovchi.R
import com.uz.sovchi.data.UserRepository
import com.uz.sovchi.data.utils.DateUtils
import com.uz.sovchi.databinding.AllUsersFragmentBinding
import com.uz.sovchi.databinding.UserItemBinding
import com.uz.sovchi.ui.base.BaseAdapter
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.base.EmptyDiffUtil
import kotlinx.coroutines.launch

class AllUsersFragment : BaseFragment<AllUsersFragmentBinding>() {

    override val layId: Int
        get() = R.layout.all_users_fragment

    private val adapterUsers = object :
        BaseAdapter<com.uz.sovchi.data.User, UserItemBinding>(R.layout.user_item, EmptyDiffUtil()) {
        override fun bind(holder: ViewHolder<*>, model: com.uz.sovchi.data.User, pos: Int) {
            holder.binding.apply {
                (this as UserItemBinding)
                nameView.text = model.name
                numberView.text = model.phoneNumber
                dateView.text =
                    if (model.lastSeenTime == 0L) "Onlayn" else DateUtils.formatDate(model.lastSeenTime)
            }
        }
    }

    override fun viewCreated(bind: AllUsersFragmentBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@AllUsersFragment)
            recyclerView.apply {
                layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                adapter = adapterUsers
            }
            progressBar.isVisible = true
            UserRepository.loadAllUsers(lifecycleScope) {
                lifecycleScope.launch {
                    progressBar.isVisible = false
                    adapterUsers.submitList(it.sortedByDescending { it.lastSeenTime })
                }
            }
        }
    }
}