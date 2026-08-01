package com.youtubie.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Utilities for creating destination URIs in the public Downloads/YouTubie location.
 */
object StorageUtils {
    /**
     * Creates a MediaStore or legacy file URI for a download destination.
     *
     * @param context context used to access the content resolver.
     * @param fileName display name for the target file.
     * @param mimeType MIME type associated with the target file.
     * @return inserted MediaStore URI or legacy file URI insert result, or null if creation fails.
     */
    fun getDownloadUri(context: Context, fileName: String, mimeType: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/YouTubie")
            }
        }

        val contentResolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            // Legacy for < Android 10
            Uri.fromFile(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "YouTubie/$fileName"))
        }

        return contentResolver.insert(collection, contentValues)
    }
}
