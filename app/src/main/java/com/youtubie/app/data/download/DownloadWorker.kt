package com.youtubie.app.data.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.io.OutputStream

class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString("url") ?: return Result.failure()
        val fileName = inputData.getString("fileName") ?: "video.mp4"
        val mimeType = inputData.getString("mimeType") ?: "video/mp4"

        return try {
            downloadFile(downloadUrl, fileName, mimeType)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun downloadFile(downloadUrl: String, fileName: String, mimeType: String) {
        val client = OkHttpClient()
        val request = Request.Builder().url(downloadUrl).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) throw Exception("Failed to download file")

        val inputStream: InputStream = response.body?.byteStream() ?: throw Exception("Empty body")
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/YouTubie")
            }
        }

        val contentResolver = applicationContext.contentResolver
        val uri: Uri? = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            val outputStream: OutputStream? = contentResolver.openOutputStream(it)
            outputStream?.use { os ->
                inputStream.use { isr ->
                    isr.copyTo(os)
                }
            }
        }
    }
}
