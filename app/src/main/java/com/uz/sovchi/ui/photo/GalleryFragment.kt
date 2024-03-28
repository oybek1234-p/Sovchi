package com.uz.sovchi.ui.photo

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import coil.load
import com.igreenwood.loupe.Loupe
import com.uz.sovchi.MainActivity
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.handleException
import com.uz.sovchi.PermissionController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID.randomUUID

class PickPhotoFragment(multiple: Boolean, done: (list: List<Image>) -> Unit) :
    Fragment() {

    data class Image(val path: String, var thumbnail: String? = null)

    companion object {

        fun deleteFile(file: File) {
            try {
                if (!file.exists()) return
                file.delete()
            } catch (e: Exception) {
                handleException(e)
            }
        }

        suspend fun getRealFile(path: String) =
            withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(path)
                    val inputStream = appContext.contentResolver.openInputStream(uri)
                    val file = File(
                        appContext.cacheDir,
                        randomUUID().toString() + ".jpg"
                    )
                    if (file.exists().not()) {
                        file.createNewFile()
                    }
                    val outPutStream = FileOutputStream(file)
                    inputStream?.copyTo(outPutStream)
                    inputStream?.close()
                    outPutStream.flush()
                    outPutStream.close()
                    file
                } catch (e: Exception) {
                    handleException(e)
                    null
                }
            }
    }

    fun open(activity: MainActivity) {
        val open = {
            val manager = activity.supportFragmentManager
            manager.beginTransaction().add(R.id.container, this).commit()
        }
        PermissionController.getInstance().requestPermissions(
            activity,
            0,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES
                ) else arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), object : PermissionController.PermissionResult {
                override fun onGranted() {
                    open.invoke()
                }

                override fun onDenied() {
                    //Ignore
                    open.invoke()
                }
            }
        )
    }

    private fun close() {
        requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
    }

    private val pickMedia =
        registerForActivityResult(if (multiple) ActivityResultContracts.PickMultipleVisualMedia() else ActivityResultContracts.PickVisualMedia()) {
            when (it) {
                is List<*> -> {
                    val uris = it as List<Uri>
                    val images = uris.map { p -> Image(p.toString()) }
                    done.invoke(images)
                }

                is Uri -> {
                    done.invoke(listOf(Image(it.toString())))
                }

                else -> {
                    done.invoke(emptyList())
                }
            }
            close()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

fun showPhoto(imageView: ImageView,decorView: ViewGroup,url: String) {
    Loupe.create(imageView,decorView) {
        onViewTranslateListener = object : Loupe.OnViewTranslateListener {

            override fun onStart(view: ImageView) {
                view.load(url)
            }

            override fun onViewTranslate(view: ImageView, amount: Float) {
                // called whenever the view position changed
            }

            override fun onRestore(view: ImageView) {
                // called when the view drag gesture ended
            }

            override fun onDismiss(view: ImageView) {
                // called when the view drag gesture ended
            }
        }
    }
}
