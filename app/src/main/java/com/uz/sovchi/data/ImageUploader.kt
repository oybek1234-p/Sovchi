package com.uz.sovchi.data

import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.uz.sovchi.appContext
import com.uz.sovchi.handleException
import com.uz.sovchi.imageKitt.ImageKitUtils
import com.uz.sovchi.ui.photo.PickPhotoFragment
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import id.zelory.compressor.constraint.destination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object ImageUploader {

    private const val TAG = "images"

    private val imagesStorage = FirebaseStorage.getInstance().getReference("test")

    init {
        FirebaseStorage.getInstance().maxUploadRetryTimeMillis = 900000
    }

    private fun getRandomImageId() = "sovchiImage${UUID.randomUUID()}"

    object UploadImageTypes {
        const val NOMZOD_PHOTO = "nomzodPhoto"
        const val PASSPORT_PHOTO = "passportPhoto"
        const val PREMIUM_CHECK_PHOTO = "premiumCheckPhoto"
        const val SELFIE_PHOTO = "selfiePhoto"
        const val CHAT_PHOTO = "chatPhoto"
    }

    suspend fun uploadImage(
        image: PickPhotoFragment.Image, type: String, done: (url: String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        val path = image.path
        if (path.startsWith("https://firebasestorage")) {
            done.invoke(path)
            return@withContext
        }
        if (path.startsWith(ImageKitUtils.imageKitIndex)) {
            done.invoke(path)
            return@withContext
        }
        if (path.isEmpty()) {
            done.invoke(null)
            return@withContext
        }
        val realFile = PickPhotoFragment.getRealFileNew(path)
        var compressedFile: File? = null

        val upload = {
            var loadUri = compressedFile?.toUri()
            if (loadUri == null) {
                loadUri = Uri.parse(path)
            }
            imagesStorage.child(getRandomImageId()).putFile(
                loadUri!!,
                StorageMetadata.Builder().setCustomMetadata("type", type)
                    .setCustomMetadata("userId", LocalUser.user.uid).build()
            ).addOnSuccessListener { it ->
                it.storage.downloadUrl.addOnSuccessListener {
                    done.invoke(it.toString())
                }.addOnFailureListener {
                    done.invoke(null)
                }
            }.addOnFailureListener {
                done.invoke(null)
            }
        }
        if (realFile != null) {
            try {
                val destiny = PickPhotoFragment.createNewPhotoFile()
                val compressed = Compressor.compress(appContext, realFile) {
                    default(format = Bitmap.CompressFormat.WEBP, width = 412, height = 624)
                    destination(destiny)
                }
                compressedFile = compressed
                upload.invoke()
            } catch (e: Exception) {
                handleException(e)
                upload.invoke()
            }
        } else {
            upload.invoke()
        }
    }
}