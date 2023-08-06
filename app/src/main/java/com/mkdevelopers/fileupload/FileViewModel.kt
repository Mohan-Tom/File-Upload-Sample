package com.mkdevelopers.fileupload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import okio.IOException
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

class FileViewModel(
    private val repository: FileRepository = FileRepository()
): ViewModel() {

    var job: Job? = null
    var uploadCall: Call? = null
    var scope: CoroutineScope? = null
    var childScope1: CoroutineScope = CoroutineScope(Dispatchers.IO)
    var childScope2: CoroutineScope = CoroutineScope(Dispatchers.IO)

    fun uploadFile(file: File) {
        scope = CoroutineScope(Dispatchers.IO)
        job = scope?.launch {

            childScope1.launch {
                println("Preparing upload ${file.name}")
                val progressRequestBody = ProgressRequestBody(
                    file,
                    MIME_TYPE_MP4,
                    object : ProgressRequestBody.ProgressCallback {
                        override suspend fun onProgressUpdate(percentage: Int) {
                            println("Uploading: ${file.name} PROGRESS $percentage")
                        }

                        override fun onError(exception: Exception) {
                            println("Uploading: ${file.name} cancelled")
                        }
                    }
                )

                delay(1000)

                /*FileUploadHelper.uploadFile(
                    "http://10.0.2.2:8080/file",
                    file,
                    UploadMediaTypeEnum.VIDEO,
                    successListener = {
                        println("Uploading: success - ${it.body}")
                    },
                    failureListener = {
                        println("Uploading: failed - $it")
                    },
                    workerScope = scope!!
                ) {
                    println("Uploading: ${file.name} PROGRESS $it")
                }*/
            }

            childScope2.launch{
                println("Preparing upload ${file.name}")
                val progressRequestBody = ProgressRequestBody(
                    file,
                    MIME_TYPE_MP4,
                    object : ProgressRequestBody.ProgressCallback {
                        override suspend fun onProgressUpdate(percentage: Int) {
                            println("Uploading: ${file.name} PROGRESS $percentage")
                        }

                        override fun onError(exception: Exception) {
                            println("Uploading: ${file.name} cancelled")
                        }
                    }
                )

                delay(1000)

                /*FileUploadHelper.uploadFile(
                    "http://10.0.2.2:8080/file",
                    file,
                    UploadMediaTypeEnum.VIDEO,
                    successListener = {
                        println("Uploading: success - ${it.body}")
                    },
                    failureListener = {
                        println("Uploading: failed - $it")
                    },
                    workerScope = scope!!
                ) {
                    println("Uploading: ${file.name} PROGRESS $it")
                }*/
            }

            /*uploadJob(
                "http://10.0.2.2:8080/file",
                file,
                UploadMediaTypeEnum.VIDEO,
                scope!!
            ) {
                println("Uploading: ${file.name} PROGRESS $it")
            }*/

            //repository.uploadFile(progressRequestBody, file)
        }
    }

    private suspend fun uploadJob(
        url: String,
        file: File,
        mediaTypeEnum: UploadMediaTypeEnum,
        scope: CoroutineScope,
        onProgress: suspend (progress: Int) -> Unit
    ) {
        val client = OkHttpClient()
            .newBuilder()
            .addInterceptor(UserAgentInterceptor())
            //.addInterceptor(CancellationInterceptor(scope))
            .connectTimeout(60, TimeUnit.SECONDS)
            .build()

        val progressRequestBody = ProgressRequestBody(
            file,
            mediaTypeEnum.mediaType,
            object : ProgressRequestBody.ProgressCallback {
                override suspend fun onProgressUpdate(percentage: Int) {
                    println(">> Uploading: ${file.name} PROGRESS $percentage")
                    onProgress(percentage)
                }

                override fun onError(exception: Exception) {
                    println(">> Uploading: failed $exception")
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

        uploadCall = client.newCall(request)

        scope.coroutineContext[Job]?.invokeOnCompletion {
            if(it is CancellationException) {
                uploadCall?.cancel()
                println("Cancellation job invoked")
            }
        }

        /*scope.coroutineContext[Job]?.invokeOnCompletion {
            if (it is CancellationException && !uploadCall!!.isCanceled()) {
                uploadCall?.cancel()
                println("OkHttp call was canceled in between the upload.")
            }
        }*/

        uploadCall?.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                println("Response: >> enqueue ${response.body?.string()}")
                response.close()
                //scope.cancel() // Cancel the coroutine after receiving the response
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Error: >> enqueue ${e.message}")
                //scope.cancel() // Cancel the coroutine on failure as well
            }
        })

        /*uploadCall!!.execute().use { response ->
            val success = response.isSuccessful
            val body = response.body
            val message = response.message

            println(">> isSuccess $success, body $body, message $message")
            return response
        }*/
    }

    class CancellationInterceptor(private val scope: CoroutineScope) : Interceptor {
        private val canceled = AtomicBoolean()

        fun cancel() {
            canceled.set(true)
        }

        override fun intercept(chain: Interceptor.Chain): Response {
            if (canceled.get()) {
                throw CancellationException("Call was canceled")
            }

            val request: Request = chain.request()

            // Create a new deferred to hold the OkHttp call
            val deferred = scope.async {
                chain.proceed(request)
            }

            // Register a cancellation listener to cancel the OkHttp call if the coroutine is cancelled
            scope.coroutineContext[Job]?.invokeOnCompletion {
                if (it is CancellationException && !deferred.isCompleted) {
                    deferred.cancel()
                }
            }

            // Wait for the OkHttp call to complete and return the result
            return runBlocking {
                try {
                    deferred.await()
                } catch (e: CancellationException) {
                    throw CancellationException("Call was canceled")
                }
            }
        }
    }

    companion object {
        const val MIME_TYPE_JPEG = "image/jpeg"
        const val MIME_TYPE_MP4 = "video/mp4"
    }
}