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
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink

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
        val viewCount = inputData.getString("viewCount")
        val duration = inputData.getString("duration")

        setProgress(workDataOf("progress" to 0))

        return try {
            val success = downloadFile(downloadUrl, fileName, mimeType) { progress ->
                setProgress(workDataOf("progress" to progress))
            }

            if (success) {
                // Save to history upon successful download
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val historyItem = DownloadHistoryItem(
                    videoId = videoId,
                    title = title,
                    channelTitle = channelTitle,
                    thumbnailUrl = thumbnailUrl,
                    downloadDate = dateStr,
                    format = formatType,
                    viewCount = viewCount,
                    duration = duration
                )
                PreferenceManager(applicationContext).addHistoryItem(historyItem)

                setProgress(workDataOf("progress" to 100))
                Result.success()
            } else {
                // If stopped/cancelled, we return failure.
                Result.failure()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun downloadFile(
        downloadUrl: String,
        fileName: String,
        mimeType: String,
        onProgressUpdate: suspend (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        
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
                        totalBytes = response.body?.contentLength() ?: -1L
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
                            val source = response.body?.source() ?: return@async false
                            val buffer = ByteArray(128 * 1024) // 128KB buffer
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

        val responseBody = response.body ?: throw Exception("Empty body")
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
                val segmentSize = 128 * 1024L // 128KB
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
    fun insertDownload(contentResolver: android.content.ContentResolver, contentValues: ContentValues): Uri? {
        return contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    }
}
