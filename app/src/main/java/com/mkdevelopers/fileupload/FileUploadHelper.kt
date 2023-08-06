package com.mkdevelopers.fileupload

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import okio.IOException
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object FileUploadHelper {

    private val TAG = FileUploadHelper::class.java.simpleName

    suspend fun uploadFile(
        url: String,
        file: File,
        mediaTypeEnum: UploadMediaTypeEnum,
        workerScope: CoroutineScope,
        cancellationInterceptor: CancellationInterceptor,
        successListener: (response: Response) -> Unit,
        failureListener: (exception: Exception) -> Unit,
        onProgress: suspend (progress: Int) -> Unit,
    ): Response? {

        println("$TAG >> url $url")
        println("$TAG >> extension ${file.extension}, ${file.name}")

        try {
            val client = OkHttpClient()
                .newBuilder()
                .addInterceptor(UserAgentInterceptor())
                .addInterceptor(cancellationInterceptor)
                .connectTimeout(60, TimeUnit.SECONDS)
                .build()

            val progressRequestBody = ProgressRequestBody(
                file,
                mediaTypeEnum.mediaType,
                object : ProgressRequestBody.ProgressCallback {
                    override suspend fun onProgressUpdate(percentage: Int) {
                        println("$TAG >> Uploading: ${file.name} PROGRESS $percentage")
                        onProgress(percentage)
                    }

                    override fun onError(exception: Exception) {
                        println("$TAG >> Uploading: failed $exception")
                    }
                }
            )

            val mediaType = mediaTypeEnum.mediaType.toMediaType()
            val requestBody = file.asRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .put(requestBody)
                .put(progressRequestBody)
                .build()

            val uploadCall = client.newCall(request)

            /*uploadCall.execute().use { response ->
                val success = response.isSuccessful
                val body = response.body
                val message = response.message

                println("$TAG >> isSuccess $success, body $body, message $message")
                return response
            }*/

            workerScope.coroutineContext[Job]?.invokeOnCompletion {
                if(it is CancellationException) {
                    uploadCall.cancel()
                    println("$TAG >> Cancellation job invoked")
                }
            }

            val future = CompletableFuture<Response>()

            val call = uploadCall.enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    println("$TAG >> Response: >> enqueue ${response.body?.string()}")
                    successListener(response)
                    response.close()
                    future.complete(response)
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("$TAG >> Error: >> enqueue ${e.message}")
                    failureListener(e)
                    future.complete(null)
                }
            })

            return withContext(Dispatchers.IO) {
                future.get()
            }

        } catch (e: Exception) {
            println("$TAG >> failed, body exception $e")
            failureListener(e)
            return null
        }
    }

    enum class FileUploadStatusEnum {
        UPLOAD_PENDING,
        UPLOAD_STARTED,
        UPLOADING,
        UPLOAD_COMPLETE,
        UPLOAD_FAILED
    }
}