package com.uz.sovchi.data

import androidx.core.net.toUri
import com.google.firebase.storage.FirebaseStorage
import com.uz.sovchi.appContext
import com.uz.sovchi.ui.photo.PickPhotoFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.zibin.luban.Luban
import top.zibin.luban.OnCompressListener
import java.io.File

object ImageUploader {

    private const val TAG = "images"

    private val imagesStorage = FirebaseStorage.getInstance().getReference(TAG)

    private fun getRandomImageId() = "sovchiImage${System.nanoTime()}"

    suspend fun uploadImage(image: PickPhotoFragment.Image, done: (url: String?) -> Unit) =
        withContext(Dispatchers.IO) {
            val path = image.path
            if (path.startsWith("https://firebasestorage")) {
                done.invoke(path)
                return@withContext
            }
            if (path.isEmpty()) {
                done.invoke(null)
                return@withContext
            }
            var uploadFile = PickPhotoFragment.getRealFile(path)

            val upload = {
                if (uploadFile != null) {
                    imagesStorage.child(getRandomImageId()).putFile(uploadFile!!.toUri())
                        .addOnCompleteListener { it ->
                            it.result.storage.downloadUrl.addOnCompleteListener {
                                done.invoke(it.result.toString())
                            }
                        }
                } else {
                    done.invoke(null)
                }
            }
            Luban.with(appContext).load(uploadFile)
                .setCompressListener(object : OnCompressListener {
                    override fun onError(e: Throwable?) {
                        //null
                        upload.invoke()
                    }

                    override fun onStart() {

                    }

                    override fun onSuccess(file: File?) {
                        uploadFile = file
                        upload.invoke()
                    }
                }).launch()
        }
}