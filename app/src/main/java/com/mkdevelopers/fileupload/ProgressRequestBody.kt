package com.mkdevelopers.fileupload

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class ProgressRequestBody(
    private val file: File,
    private val contentType: String,
    private val listener: ProgressCallback
) : RequestBody() {

    override fun contentType(): MediaType? {
        return contentType.toMediaTypeOrNull()
    }

    override fun contentLength(): Long {
        return file.length()
    }

    override fun writeTo(sink: BufferedSink) {
        val fileLength = file.length()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

        try {
            FileInputStream(file).use { fis ->
                var uploaded: Long = 0
                var read: Int
                val handler = Handler(Looper.getMainLooper())
                var lastUpdate: Long = 0
                while (fis.read(buffer).also { read = it } != -1) {
                    if (System.currentTimeMillis() > lastUpdate + PROGRESS_DEBOUNCE_MILLIS) {
                        // update progress on UI thread
                        handler.post(
                            ProgressUpdater(
                                uploaded,
                                fileLength
                            )
                        )
                        lastUpdate = System.currentTimeMillis()
                    }
                    uploaded += read.toLong()
                    sink.write(buffer, 0, read)
                }
                handler.post(
                    ProgressUpdater(
                        fileLength,
                        fileLength
                    )
                )
            }
        } catch (e: IOException) {
            listener.onError(e)
        }
    }

    interface ProgressCallback {
        suspend fun onProgressUpdate(percentage: Int)
        fun onError(exception: Exception)
    }

    inner class ProgressUpdater(private val uploaded: Long, private val total: Long) : Runnable {
        override fun run() {
            if (uploaded > 0) {
                CustomScope.getApplicationScope().launch {
                    listener.onProgressUpdate((100 * uploaded / total).toInt())
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_BUFFER_SIZE = 2048
        const val PROGRESS_DEBOUNCE_MILLIS = 10
    }
}