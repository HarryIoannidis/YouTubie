package com.youtubie.app.data.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.youtubie.app.data.model.DownloadHistoryItem
import com.youtubie.app.util.PreferenceManager
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString("url") ?: return Result.failure()
        val fileName = inputData.getString("fileName") ?: "video.mp4"
        val mimeType = inputData.getString("mimeType") ?: "video/mp4"
        val videoId = inputData.getString("videoId") ?: ""
        val title = inputData.getString("title") ?: "Video"
        val channelTitle = inputData.getString("channelTitle") ?: "Unknown"
        val thumbnailUrl = inputData.getString("thumbnailUrl") ?: ""
        val formatType = inputData.getString("formatType") ?: "Video"

        setProgress(workDataOf("progress" to 0))

        return try {
            downloadFile(downloadUrl, fileName, mimeType) { progress ->
                setProgress(workDataOf("progress" to progress))
            }

            // Save to history upon successful download
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val historyItem = DownloadHistoryItem(
                videoId = videoId,
                title = title,
                channelTitle = channelTitle,
                thumbnailUrl = thumbnailUrl,
                downloadDate = dateStr,
                format = formatType
            )
            PreferenceManager(applicationContext).addHistoryItem(historyItem)

            setProgress(workDataOf("progress" to 100))
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun downloadFile(
        downloadUrl: String, 
        fileName: String, 
        mimeType: String,
        onProgressUpdate: suspend (Int) -> Unit
    ) {
        val client = OkHttpClient()
        val request = Request.Builder().url(downloadUrl).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) throw Exception("Failed to download file")

        @Suppress("UNNECESSARY_SAFE_CALL", "USELESS_ELVIS")
        val responseBody = response.body ?: throw Exception("Empty body")
        val totalBytes = responseBody.contentLength()
        val inputStream: InputStream = responseBody.byteStream()
        
        val contentResolver = applicationContext.contentResolver
        var localFile: java.io.File? = null

        val outputStream: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/YouTubie")
            }
            val uri = DownloadWorkerApi29.insertDownload(contentResolver, contentValues)
            uri?.let { contentResolver.openOutputStream(it) }
        } else {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val youTubieDir = java.io.File(downloadsDir, "YouTubie")
            if (!youTubieDir.exists()) {
                youTubieDir.mkdirs()
            }
            val file = java.io.File(youTubieDir, fileName)
            localFile = file
            java.io.FileOutputStream(file)
        }

        outputStream?.use { os ->
            inputStream.use { isr ->
                val buffer = ByteArray(8192)
                var bytesCopied = 0L
                var bytes = isr.read(buffer)
                while (bytes >= 0) {
                    os.write(buffer, 0, bytes)
                    bytesCopied += bytes
                    if (totalBytes > 0) {
                        val progress = ((bytesCopied * 100) / totalBytes).toInt()
                        kotlinx.coroutines.runBlocking {
                            onProgressUpdate(progress)
                        }
                    }
                    bytes = isr.read(buffer)
                }
            }
        }

        // Notify MediaScanner for older APIs
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && localFile != null) {
            android.media.MediaScannerConnection.scanFile(
                applicationContext,
                arrayOf(localFile.absolutePath),
                arrayOf(mimeType),
                null
            )
        }
    }
}

@androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
private object DownloadWorkerApi29 {
    fun insertDownload(contentResolver: android.content.ContentResolver, contentValues: ContentValues): Uri? {
        return contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    }
}
