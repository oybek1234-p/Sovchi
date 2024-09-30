package com.uz.sovchi.ui.photo

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.uz.sovchi.MainActivity
import com.uz.sovchi.PermissionController
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID.randomUUID


class PickPhotoFragment(multiple: Boolean, done: (list: List<Image>) -> Unit) : Fragment() {

    data class Image(val path: String, var thumbnail: String? = null)

    companion object {

        fun getRealFileNew(path: String): File? {
            try {
                val sourceUri = Uri.parse(path)
                return File(getPathFromUri(appContext, sourceUri)!!)
            } catch (e: Exception) {
                return null
            }
        }

        suspend fun createNewPhotoFile(): File = withContext(Dispatchers.IO) {
            val file = File(appContext.cacheDir, randomUUID().toString() + ".jpg")
            if (file.exists().not()) {
                file.createNewFile()
            }
            file
        }

        suspend fun getRealFile(path: String): File? = withContext(Dispatchers.IO) {
            try {
                // Create a reference to the file at the provided path
                val sourceUri = Uri.parse(path)
                val sourceFile =
                    File(getPathFromUri(appContext, sourceUri) ?: return@withContext null)
                // Ensure the file exists
                if (!sourceFile.exists()) {
                    showToast("File does not exist")
                    return@withContext null
                }
                // Create a cache directory inside the app's cache
                val cacheDir = File(appContext.cacheDir, "my_cache_dir")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs() // Create the directory if it doesn't exist
                }
                // Create a new file in the cache directory with the same name
                val cacheFile = File(cacheDir, System.currentTimeMillis().toString())

                // Copy the contents of the source file to the cache file
                sourceFile.copyTo(cacheFile, overwrite = true)

                return@withContext cacheFile
            } catch (e: IOException) {
                e.printStackTrace()
                return@withContext null
            }
        }


        private fun getPathFromUri(context: Context, uri: Uri): String? {
            val isKitKat = true

            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                return when {
                    isExternalStorageDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split =
                            docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val type = split[0]
                        if ("primary".equals(type, ignoreCase = true)) {
                            "${Environment.getExternalStorageDirectory()}/${split[1]}"
                        } else {
                            // Handle non-primary volumes if needed
                            null
                        }
                    }

                    isDownloadsDocument(uri) -> {
                        try {
                            val id = DocumentsContract.getDocumentId(uri)
                            val contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"), id.toLong()
                            )
                            getDataColumn(context, contentUri, null, null)
                        } catch (e: NumberFormatException) {
                            // Handle cases where the ID is not a valid number
                            null
                        }
                    }

                    isMediaDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split =
                            docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val type = split[0]
                        val contentUri = when (type) {
                            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else -> null
                        }
                        val selection = "_id=?"
                        val selectionArgs = arrayOf(split[1])
                        getDataColumn(context, contentUri, selection, selectionArgs)
                    }

                    else -> {
                        // Handle other cases
                        null
                    }
                }
            } else if ("content".equals(uri.scheme, ignoreCase = true)) {
                if (isGooglePhotosUri(uri)) {
                    return uri.lastPathSegment
                }
                return getDataColumn(context, uri, null, null)
            } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                return uri.path
            }

            return null
        }

        fun getDataColumn(
            context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?
        ): String? {
            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(
                column
            )

            try {
                cursor = context.contentResolver.query(
                    uri!!, projection, selection, selectionArgs, null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    val index: Int = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
            return null
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is ExternalStorageProvider.
         */
        fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is DownloadsProvider.
         */
        fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is MediaProvider.
         */
        fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is Google Photos.
         */
        fun isGooglePhotosUri(uri: Uri): Boolean {
            return "com.google.android.apps.photos.content" == uri.authority
        }
//        suspend fun getRealFile(path: String) =
//            withContext(Dispatchers.IO) {
//                try {
//                    val uri = Uri.parse(path)
//                    val inputStream = appContext.contentResolver.openInputStream(uri)
//                    val file = File(
//                        appContext.cacheDir,
//                        randomUUID().toString() + ".jpg"
//                    )
//                    if (file.exists().not()) {
//                        file.createNewFile()
//                    }
//                    val outPutStream = FileOutputStream(file)
//                    inputStream?.copyTo(outPutStream)
//                    inputStream?.close()
//                    outPutStream.flush()
//                    outPutStream.close()
//                    file
//                } catch (e: Exception) {
//                    handleException(e)
//                    null
//                }
//            }
    }

    private fun openAppSettings(activity: Activity) {
        Toast.makeText(activity, "Rasmlarni o'qishga ruxsat bering!", Toast.LENGTH_LONG).show()
        lifecycleScope.launch {
            delay(1000)
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
            activity.startActivity(intent)
        }
    }

    fun open(activity: MainActivity) {
        if (activity.isFinishing) return
        val open = {
            val manager = activity.supportFragmentManager
            manager.beginTransaction().add(R.id.container, this).commit()
        }
        PermissionController.getInstance().requestPermissions(activity,
            0,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES
            ) else arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            object : PermissionController.PermissionResult {
                override fun onGranted() {
                    open.invoke()
                }

                override fun onDenied() {
                    openAppSettings(activity)
                }
            })
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

