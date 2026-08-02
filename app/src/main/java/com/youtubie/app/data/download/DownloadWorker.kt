package com.youtubie.app.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.youtubie.app.data.model.DownloadHistoryItem
import com.youtubie.app.util.PreferenceManager
import com.youtubie.app.R
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink

/**
 * WorkManager task that downloads a media URL into the public Downloads/YouTubie directory.
 *
 * The worker prefers a four-part HTTP range download when the server supports byte ranges,
 * then falls back to a single stream download for sources that do not advertise range support.
 *
 * @param appContext application context supplied by WorkManager.
 * @param params worker parameters containing the input data used to name and describe the download.
 */
class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val CHANNEL_ID = "download_channel"
        private const val CHANNEL_NAME = "Downloads"

        // Shared OkHttpClient for connection reuse and performance optimization
        val sharedClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
                .build()
        }
    }

    private var lastUpdateTimestamp = 0L
    private var lastProgress = -1

    /**
     * Executes the download, updates foreground progress, and records successful downloads in history.
     *
     * @return [Result.success] when the file is written successfully; [Result.failure] when required
     * input is missing or the download cannot complete.
     */
    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString("url") ?: return Result.failure()
        val fileName = inputData.getString("fileName") ?: "video.mp4"
        val mimeType = inputData.getString("mimeType") ?: "video/mp4"
        val videoId = inputData.getString("videoId") ?: ""
        val title = inputData.getString("title") ?: "Video"
        val channelTitle = inputData.getString("channelTitle") ?: "Unknown"
        val thumbnailUrl = inputData.getString("thumbnailUrl") ?: ""
        val formatType = inputData.getString("formatType") ?: "Video"
        val viewCount = inputData.getString("viewCount")
        val duration = inputData.getString("duration")

        try {
            setForeground(createForegroundInfo(title, fileName, 0))
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        setProgress(workDataOf("progress" to 0))

        return try {
            val success = downloadFile(downloadUrl, fileName, mimeType) { progress ->
                val now = System.currentTimeMillis()
                if (progress != lastProgress && (now - lastUpdateTimestamp >= 300 || progress == 100 || progress == 0)) {
                    lastProgress = progress
                    lastUpdateTimestamp = now
                    try {
                        setProgress(workDataOf("progress" to progress))
                        setForeground(createForegroundInfo(title, fileName, progress))
                    } catch (t: Throwable) {
                        // Ignore if foreground/progress update fails
                    }
                }
            }

            if (success) {
                // Save to history upon successful download
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val cleanThumbnailUrl = if (thumbnailUrl.contains("ytimg.com")) {
                    thumbnailUrl.replace("/sddefault.", "/mqdefault.")
                        .replace("/hqdefault.", "/mqdefault.")
                        .replace("/default.", "/mqdefault.")
                } else thumbnailUrl

                val historyItem = DownloadHistoryItem(
                    videoId = videoId,
                    title = title,
                    channelTitle = channelTitle,
                    thumbnailUrl = cleanThumbnailUrl,
                    downloadDate = dateStr,
                    format = formatType,
                    viewCount = viewCount,
                    duration = duration
                )
                PreferenceManager(applicationContext).addHistoryItem(historyItem)

                setProgress(workDataOf("progress" to 100))
                showCompletionNotification(title, fileName, success = true)
                Result.success()
            } else {
                showCompletionNotification(title, fileName, success = false)
                Result.failure()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showCompletionNotification(title, fileName, success = false)
            Result.failure()
        }
    }

    /**
     * Builds the foreground-service notification used while a download is active.
     *
     * @param title user-visible video title shown in the notification.
     * @param fileName destination file name shown in the notification body.
     * @param progress current download progress from 0 to 100.
     * @return foreground metadata passed to WorkManager.
     */
    private fun createForegroundInfo(title: String, fileName: String, progress: Int): ForegroundInfo {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of active YouTube downloads"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val destinationPath = "Download/YouTubie/$fileName"

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ytlogo)
            .setContentTitle("Downloading: $title")
            .setContentText("Destination: $destinationPath")
            .setSubText("$progress%")
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationId = (id.hashCode() and 0x7FFFFFFF) % 10000 + 1000

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                else 0
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    /**
     * Displays a terminal notification after the worker finishes or fails.
     *
     * @param title user-visible video title.
     * @param fileName file name used for successful save messages.
     * @param success true when the file was saved successfully, false for failure messaging.
     */
    private fun showCompletionNotification(title: String, fileName: String, success: Boolean) {
        try {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val destinationPath = "Download/YouTubie/$fileName"

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ytlogo)
                .setContentTitle(if (success) applicationContext.getString(R.string.download_complete) else applicationContext.getString(R.string.download_failed))
                .setContentText(if (success) "$title saved to $destinationPath" else "Failed to download $title")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            val notificationId = (id.hashCode() and 0x7FFFFFFF) % 10000 + 2000
            notificationManager.notify(notificationId, notification)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    /**
     * Downloads a file with parallel byte-range requests when possible.
     *
     * @param downloadUrl direct media URL to fetch.
     * @param fileName destination display name.
     * @param mimeType media MIME type stored with the downloaded item.
     * @param onProgressUpdate callback invoked with integer progress from 0 to 100.
     * @return true when all chunks are written and flushed; false when a chunk fails or cancellation occurs.
     * @throws Exception when the destination cannot be opened or a recoverable download failure must be
     * reported to WorkManager.
     */
    private suspend fun downloadFile(
        downloadUrl: String,
        fileName: String,
        mimeType: String,
        onProgressUpdate: suspend (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val client = sharedClient

        // 1. Initial request to check range support and get total size
        val initialRequest = Request.Builder()
            .url(downloadUrl)
            .addHeader("Range", "bytes=0-0")
            .build()

        var totalBytes = -1L
        var acceptRanges = false

        try {
            client.newCall(initialRequest).execute().use { response ->
                if (response.isSuccessful || response.code == 206) {
                    val contentRange = response.header("Content-Range")
                    if (contentRange != null) {
                        totalBytes = contentRange.substringAfterLast("/").toLongOrNull() ?: -1L
                        acceptRanges = true
                    } else {
                        totalBytes = response.body.contentLength()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (totalBytes <= 0 || !acceptRanges) {
            return@withContext downloadFileSingleThreaded(client, downloadUrl, fileName, mimeType, onProgressUpdate)
        }

        // 2. Prepare destination
        val contentResolver = applicationContext.contentResolver
        var localFile: File? = null
        var downloadedUri: Uri? = null
        val pfd: android.os.ParcelFileDescriptor?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/YouTubie")
            }
            downloadedUri = DownloadWorkerApi29.insertDownload(contentResolver, contentValues)
            pfd = downloadedUri?.let { contentResolver.openFileDescriptor(it, "rw") }
        } else {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val youTubieDir = File(downloadsDir, "YouTubie")
            if (!youTubieDir.exists()) youTubieDir.mkdirs()
            val file = File(youTubieDir, fileName)
            localFile = file
            pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_WRITE or android.os.ParcelFileDescriptor.MODE_CREATE)
        }

        if (pfd == null) throw Exception("Failed to open file descriptor")

        return@withContext try {
            val fileChannel = FileOutputStream(pfd.fileDescriptor).channel
            val numThreads = 4
            val chunkSize = totalBytes / numThreads
            val atomicTotalRead = AtomicLong(0L)

            val deferreds = (0 until numThreads).map { i ->
                val start = i * chunkSize
                val end = if (i == numThreads - 1) totalBytes - 1 else (i + 1) * chunkSize - 1

                async(Dispatchers.IO) {
                    try {
                        val request = Request.Builder()
                            .url(downloadUrl)
                            .addHeader("Range", "bytes=$start-$end")
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful && response.code != 206) return@async false
                            val source = response.body.source()
                            val buffer = ByteArray(256 * 1024) // Optimized 256KB buffer
                            var currentPos = start

                            while (true) {
                                if (isStopped) return@async false
                                val bytesRead = source.read(buffer)
                                if (bytesRead == -1) break

                                val byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead)
                                synchronized(fileChannel) {
                                    fileChannel.write(byteBuffer, currentPos)
                                }
                                currentPos += bytesRead.toLong()
                                val totalReadSoFar = atomicTotalRead.addAndGet(bytesRead.toLong())
                                if (totalBytes > 0) {
                                    onProgressUpdate(((totalReadSoFar * 100) / totalBytes).toInt())
                                }
                            }
                            true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }
            }

            val results = deferreds.map { it.await() }
            fileChannel.force(true)
            fileChannel.close()
            pfd.close()

            if (results.all { it }) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && localFile != null) {
                    android.media.MediaScannerConnection.scanFile(applicationContext, arrayOf(localFile.absolutePath), arrayOf(mimeType), null)
                }
                true
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    downloadedUri?.let { contentResolver.delete(it, null, null) }
                } else {
                    localFile?.delete()
                }
                false
            }
        } catch (e: Exception) {
            pfd.close()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadedUri?.let { contentResolver.delete(it, null, null) }
            } else {
                localFile?.delete()
            }
            throw e
        }
    }

    /**
     * Downloads a file sequentially for hosts that do not support byte-range requests.
     *
     * @param client HTTP client used to execute the request.
     * @param downloadUrl direct media URL to fetch.
     * @param fileName destination display name.
     * @param mimeType media MIME type stored with the downloaded item.
     * @param onProgressUpdate callback invoked with integer progress from 0 to 100.
     * @return true when the stream is written successfully; false when WorkManager cancels the worker.
     * @throws Exception when the HTTP request fails or the stream cannot be written.
     */
    private suspend fun downloadFileSingleThreaded(
        client: OkHttpClient,
        downloadUrl: String,
        fileName: String,
        mimeType: String,
        onProgressUpdate: suspend (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(downloadUrl).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) throw Exception("Failed to download file")

        val responseBody = response.body
        val totalBytes = responseBody.contentLength()

        val contentResolver = applicationContext.contentResolver
        var localFile: File? = null
        var downloadedUri: Uri? = null

        val outputStream: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/YouTubie")
            }
            downloadedUri = DownloadWorkerApi29.insertDownload(contentResolver, contentValues)
            downloadedUri?.let { contentResolver.openOutputStream(it) }
        } else {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val youTubieDir = File(downloadsDir, "YouTubie")
            if (!youTubieDir.exists()) {
                youTubieDir.mkdirs()
            }
            val file = File(youTubieDir, fileName)
            localFile = file
            FileOutputStream(file)
        }

        try {
            outputStream?.let { os ->
                val source = responseBody.source()
                val sink = os.sink().buffer()
                val segmentSize = 256 * 1024L // Optimized 256KB buffer
                var totalRead = 0L

                while (true) {
                    if (isStopped) {
                        sink.close()
                        source.close()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            downloadedUri?.let { contentResolver.delete(it, null, null) }
                        } else {
                            localFile?.delete()
                        }
                        return@withContext false
                    }

                    val bytesRead = source.read(sink.buffer, segmentSize)
                    if (bytesRead == -1L) break
                    totalRead += bytesRead
                    sink.emitCompleteSegments()
                    if (totalBytes > 0) {
                        val progress = ((totalRead * 100) / totalBytes).toInt()
                        onProgressUpdate(progress)
                    }
                }
                sink.flush()
                sink.close()
                source.close()
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && localFile != null) {
                android.media.MediaScannerConnection.scanFile(
                    applicationContext,
                    arrayOf(localFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
            }
            true
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadedUri?.let { contentResolver.delete(it, null, null) }
            } else {
                localFile?.delete()
            }
            throw e
        }
    }
}

@androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
private object DownloadWorkerApi29 {
    /**
     * Inserts a pending download row into the Android 10+ downloads collection.
     *
     * @param contentResolver resolver used to access MediaStore.
     * @param contentValues metadata for the download row.
     * @return URI for the inserted row, or null if MediaStore rejects the insert.
     */
    fun insertDownload(contentResolver: android.content.ContentResolver, contentValues: ContentValues): Uri? {
        return contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    }
}
