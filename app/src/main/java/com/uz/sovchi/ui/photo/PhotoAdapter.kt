package com.uz.sovchi.ui.photo

import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.uz.sovchi.R
import com.uz.sovchi.databinding.PhotoMiniItemBinding
import com.uz.sovchi.ui.base.BaseAdapter
import com.uz.sovchi.ui.base.EmptyDiffUtil
import jp.wasabeef.glide.transformations.BlurTransformation

class PhotoAdapter(private val click: (delete: Boolean, pos: Int, model: PickPhotoFragment.Image, view: ImageView) -> Unit) :
    BaseAdapter<PickPhotoFragment.Image, PhotoMiniItemBinding>(
        R.layout.photo_mini_item,
        EmptyDiffUtil()
    ) {

    var showPhotos = true
    var deleteShown = true
    var matchParent = false

    override fun bind(holder: ViewHolder<*>, model: PickPhotoFragment.Image, pos: Int) {
        holder.apply {
            binding.apply {
                (this as PhotoMiniItemBinding)
                deleteButton.isVisible = deleteShown
                if (showPhotos.not()) {
                    Glide.with(imageView).load(model.path).transform(BlurTransformation(80)).into(imageView)
                } else {
                    Glide.with(imageView).load(model.path).into(imageView)
                }
            }
        }
    }

    override fun onViewCreated(holder: ViewHolder<PhotoMiniItemBinding>, viewType: Int) {
        holder.apply {
            binding.apply {
                if (matchParent) {
                    root.updateLayoutParams<LayoutParams> {
                        width = LayoutParams.MATCH_PARENT
                        height = LayoutParams.MATCH_PARENT
                    }
                    imageView.updateLayoutParams<LayoutParams> {
                        width = LayoutParams.MATCH_PARENT
                        height = LayoutParams.MATCH_PARENT
                    }
                }
                root.setOnClickListener {
                    click.invoke(false, adapterPosition, getItem(adapterPosition), imageView)
                }
                deleteButton.setOnClickListener {
                    click.invoke(true, adapterPosition, getItem(adapterPosition), imageView)
                }
            }
        }
    }
}