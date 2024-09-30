package com.uz.sovchi.ui.photo

import android.os.Bundle
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import com.uz.sovchi.R
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.nomzod.MyNomzodController
import com.uz.sovchi.data.valid
import com.uz.sovchi.databinding.PhotoMiniItemBinding
import com.uz.sovchi.getMainActivity
import com.uz.sovchi.handleException
import com.uz.sovchi.loadPhoto
import com.uz.sovchi.ui.base.BaseAdapter

class PhotoAdapter(private val click: (delete: Boolean, pos: Int, model: PickPhotoFragment.Image, view: ImageView) -> Unit) :
    BaseAdapter<PickPhotoFragment.Image, PhotoMiniItemBinding>(R.layout.photo_mini_item,
        object : DiffUtil.ItemCallback<PickPhotoFragment.Image>() {
            override fun areContentsTheSame(
                oldItem: PickPhotoFragment.Image, newItem: PickPhotoFragment.Image
            ): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(
                oldItem: PickPhotoFragment.Image, newItem: PickPhotoFragment.Image
            ): Boolean {
                return oldItem.path == newItem.path
            }
        }) {

    var showPhotos = true
    var deleteShown = true
    var matchParent = false
    var clickable = true

    override fun bind(holder: ViewHolder<*>, model: PickPhotoFragment.Image, pos: Int) {
        holder.apply {
            binding.apply {
                (this as PhotoMiniItemBinding)
                deleteButton.isVisible = deleteShown

                var photoShown = false
                if (deleteShown.not()) {
                    val enoughPhotos = MyNomzodController.nomzod.photos.size >= 3
                    photoShown = if (showPhotos.not()) {
                        false
                    } else if (showPhotos && enoughPhotos.not() && pos > 2) {
                        false
                    } else {
                        true
                    }
                    addPhotoView.isVisible = photoShown.not() && showPhotos
                    addPhotoButton.setOnClickListener {
                        try {
                            root.context.getMainActivity()?.navcontroller?.apply {
                                if (LocalUser.user.valid) {
                                    navigate(R.id.addNomzodFragment, Bundle().apply {
                                        putString("nId", LocalUser.user.uid)
                                    })
                                } else {
                                    navigate(R.id.authFragment)
                                }
                            }
                        } catch (e: Exception) {
                            handleException(e)
                        }
                    }
                } else {
                    addPhotoView.isVisible = false
                }
                when (model.path) {
                    "kelin" -> {
                        imageView.scaleType = ImageView.ScaleType.CENTER
                    }

                    "kuyov" -> {
                        imageView.scaleType = ImageView.ScaleType.CENTER
                    }

                    else -> {
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                }
                if (photoShown.not() && deleteShown.not()) {
                    imageView.loadPhoto(model.path, true)
                } else {
                    imageView.loadPhoto(model.path, false)
                }
                //Clicks
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
                    try {
                        if (clickable) {
                            click.invoke(
                                false, layoutPosition, getItem(layoutPosition), imageView
                            )
                        }
                    } catch (e: Exception) {
                        handleException(e)
                    }
                }
                deleteButton.setOnClickListener {
                    try {
                        click.invoke(true, adapterPosition, getItem(layoutPosition), imageView)
                    } catch (e: Exception) {
                        handleException(e)
                    }
                }
            }
        }
    }
}