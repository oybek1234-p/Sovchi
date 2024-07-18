package com.uz.sovchi.ui.requests

import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.messages.RequestModel
import com.uz.sovchi.data.messages.RequestStatus
import com.uz.sovchi.databinding.RequestItemBinding
import com.uz.sovchi.ui.base.BaseAdapter
import com.uz.sovchi.ui.nomzod.setNomzod

class RequestAdapter(val fragment: RequestsFragment) :
    BaseAdapter<RequestModel, RequestItemBinding>(
        R.layout.request_item,
        object : DiffUtil.ItemCallback<RequestModel>() {
            override fun areContentsTheSame(oldItem: RequestModel, newItem: RequestModel): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(oldItem: RequestModel, newItem: RequestModel): Boolean {
                return oldItem.id == newItem.id
            }
        }) {

    var acceptCallback: (RequestModel) -> Unit = {}
    var rejectCallback: (RequestModel) -> Unit = {}
    var connectCallback: (RequestModel) -> Unit = {}
    var onItemClick: (model: RequestModel) -> Unit = {}

    override fun onViewCreated(holder: ViewHolder<RequestItemBinding>, viewType: Int) {
        holder.apply {
            binding.apply {
                acceptButton.setOnClickListener {
                    val model = getItem(adapterPosition)
                    acceptCallback(model)
                }
                rejectButton.setOnClickListener {
                    val model = getItem(adapterPosition)
                    rejectCallback(model)
                }
                connectButton.setOnClickListener {
                    val model = getItem(adapterPosition)
                    connectCallback(model)
                }
                root.setOnClickListener {
                    val model = getItem(adapterPosition)
                    onItemClick.invoke(model)
                }
            }
        }
    }

    override fun bind(holder: ViewHolder<*>, model: RequestModel, pos: Int) {
        holder.apply {
            binding.apply {
                (this as RequestItemBinding)
                nomzodItem.newBadge.isVisible = false

                statusView.isVisible = true
                if (model.status == RequestStatus.requested) {
                    acceptButton.isVisible = true
                    rejectButton.isVisible = true
                    connectButton.isVisible = false
                } else {
                    acceptButton.isVisible = false
                    rejectButton.isVisible = false
                    if (model.status == RequestStatus.rejected) {
                        connectButton.isVisible = false
                    }
                    if (model.status == RequestStatus.accepted) {
                        connectButton.isVisible = true
                    }
                }
                if (model.requestedUserId == LocalUser.user.uid) {
                    if (model.requestedUserId != model.nomzodUserId) {
                        acceptButton.isVisible = false
                        rejectButton.isVisible = false
                    }
                    nomzodItem.setNomzod(model.nomzod, true, forDetails = false,false)
                } else {
                    nomzodItem.setNomzod(model.requestedNomzod, true, forDetails = false,true)
                }
                statusView.text = when (model.status) {
                    RequestStatus.requested -> "So'rov jo'natildi"
                    RequestStatus.accepted -> "Qabul qilindi"
                    RequestStatus.rejected -> "Rad etildi"
                    else -> ""
                }
                val color = when (model.status) {
                    RequestStatus.requested -> R.color.yellow
                    RequestStatus.accepted -> R.color.green
                    RequestStatus.rejected -> R.color.rejected
                    else -> -1
                }
                statusView.setTextColor(root.context.getColor(color))
            }
        }
    }
}