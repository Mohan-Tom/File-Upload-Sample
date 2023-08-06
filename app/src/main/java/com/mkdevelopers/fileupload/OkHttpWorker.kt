package com.mkdevelopers.fileupload

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okio.IOException
import java.io.File

@HiltWorker
class OkHttpWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    private val okHttpClient: OkHttpClient = createOkHttpClient()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        val file = File(context.externalCacheDir, "my_videoplayback.mp4")
        withContext(Dispatchers.IO) {
            file.createNewFile()
        }
        file.outputStream().use {
            //assets.open("beautiful-workplace.jpg").copyTo(it)
            context.assets.open("videoplayback.mp4").copyTo(it)
        }

        val progressRequestBody = ProgressRequestBody(
            file,
            UploadMediaTypeEnum.VIDEO.mediaType,
            object : ProgressRequestBody.ProgressCallback {
                override suspend fun onProgressUpdate(percentage: Int) {
                    println("OkHttpWorker >> Uploading: ${file.name} PROGRESS $percentage")
                }

                override fun onError(exception: Exception) {
                    println("OkHttpWorker >> Uploading: failed $exception")
                }
            }
        )

        val request = Request.Builder()
            .url("http://10.0.2.2:8080/file")
            .put(file.asRequestBody(UploadMediaTypeEnum.VIDEO.mediaType.toMediaType()))
            .put(progressRequestBody)
            .build()

        // Make the OkHttp call
        val call = okHttpClient.newCall(request)

        // Register a cancellation listener to cancel the OkHttp call if the coroutine is cancelled
        coroutineContext[Job]?.invokeOnCompletion {
            if (it is CancellationException && !call.isCanceled()) {
                call.cancel()
            }
        }

        return@withContext try {
            // Enqueue the OkHttp call and handle the response in the callback
            val response = call.execute()
            println("OkHttpWorker Response: ${response.body?.string()}")
            response.close()

            Result.success()
        } catch (e: CancellationException) {
            // OkHttp call was canceled, return success
            Result.success()
        } catch (e: IOException) {
            println("OkHttpWorker Error: ${e.message}")
            Result.failure()
        }
    }

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .build()
    }
}