package com.uz.sovchi.ui.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

open class BaseAdapter<Model, Binding : ViewDataBinding>(
    private val itemResId: Int, diffUtil: DiffUtil.ItemCallback<Model>
) : ListAdapter<Model, ViewHolder>(diffUtil) {

    private var clickListeners = mutableSetOf<ListClickListener<Model>>()

    fun addClickListener(clickListener: ListClickListener<Model>) {
        clickListeners.add(clickListener)
    }

    open fun getLayoutId(position: Int): Int = itemResId

    override fun getItemViewType(position: Int): Int {
        return getLayoutId(position)
    }

    fun removeClickListener(clickListener: ListClickListener<Model>) {
        clickListeners.remove(clickListener)
    }

    private var recycler: RecyclerView? = null

    class ViewHolder<T : ViewDataBinding>(val binding: T) : RecyclerView.ViewHolder(binding.root)

    open fun bind(holder: ViewHolder<*>, model: Model, pos: Int) {}
    open fun onViewCreated(holder: ViewHolder<Binding>, viewType: Int) {}

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recycler = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = DataBindingUtil.inflate<Binding>(
            LayoutInflater.from(parent.context), viewType, parent, false
        )
        val viewHolder = ViewHolder<Binding>(binding)
        viewHolder.itemView.apply {
            setOnClickListener {
                clickListeners.forEach {
                    it.onClick(viewHolder, getItem(viewHolder.adapterPosition))
                }
            }
            setOnLongClickListener {
                clickListeners.forEach {
                    it.onLongClick(viewHolder, getItem(viewHolder.adapterPosition))
                }
                return@setOnLongClickListener true
            }
        }
        onViewCreated(viewHolder, viewType)
        return viewHolder
    }

    override fun submitList(list: List<Model?>?) {
        super.submitList(list?.filterNotNull())
    }

    fun submitList(list: ArrayList<Model?>?) {
        super.submitList(list?.filterNotNull())
    }

    override fun submitList(list: MutableList<Model>?, commitCallback: Runnable?) {
        super.submitList(list, commitCallback)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder<*>) {
            val item = getItem(position)
            if (item != null) {
                bind(holder, getItem(position), position)
            }
        }
    }
}

class EmptyDiffUtil<T> : DiffUtil.ItemCallback<T>() {
    override fun areContentsTheSame(oldItem: T & Any, newItem: T & Any): Boolean {
        return oldItem == newItem
    }

    override fun areItemsTheSame(oldItem: T & Any, newItem: T & Any): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }
}