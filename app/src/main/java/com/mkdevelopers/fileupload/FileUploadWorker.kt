package com.mkdevelopers.fileupload

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

@HiltWorker
class FileUploadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    private val workerScope = CoroutineScope(Dispatchers.IO)

    val repository = FileRepository()

    override suspend fun doWork(): Result {

        setForeground(createForegroundInfo())

        println("$TAG >> upload worker called")

        /*val cancellationInterceptor = CancellationInterceptor()

        val job = workerScope.launch {
            uploadFile(cancellationInterceptor)
        }

        job.join()*/

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
            FileViewModel.MIME_TYPE_MP4,
            object : ProgressRequestBody.ProgressCallback {
                override suspend fun onProgressUpdate(percentage: Int) {
                    println("Uploading: ${file.name} PROGRESS $percentage")
                }

                override fun onError(exception: Exception) {
                    println("Uploading: ${file.name} cancelled")
                }
            }
        )

        repository.uploadFile(
            progressRequestBody,
            file
        )

        val outputData = Data.Builder()
            .putString("message", "Media files uploaded successfully.")

        println("$TAG >> output data $outputData")
        return Result.success(outputData.build())
    }

    private suspend fun uploadFile(file: File, cancellationInterceptor: CancellationInterceptor) {

        FileUploadHelper.uploadFile(
            "http://10.0.2.2:8080/file",
            file,
            UploadMediaTypeEnum.VIDEO,
            successListener = {
                println("$TAG >> Uploading: success - ${it.body}")
            },
            cancellationInterceptor = cancellationInterceptor,
            failureListener = {
                println("$TAG >> Uploading: failed - $it")
            },
            workerScope = workerScope
        ) {
            if(isStopped) {
                cancellationInterceptor.cancel()
                workerScope.cancel(CancellationException("Job was cancelled by user"))
            }
            println("$TAG >> Uploading: ${file.name} PROGRESS $it")
        }
    }

    /**
     * Creates an instance of Foreground info which can be used to update the
     * ongoing notification
     */
    private fun createForegroundInfo(): ForegroundInfo {
        val id = applicationContext.getString(R.string.message_worker_notifications_channel_id)
        val title = "Uploading a file"

        val notificationId = getId()

        /**
         * This pending intent can be used to cancel the worker
         */
        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(notificationId)

        /**
         * Create a notification channel if necessary
         */
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        /**
         * Also, add more options
         * .setTicket(title)
         * .setContextText(progress)
         *
         * Add the cancel action to the notification which can
         * be used to cancel the worker
         * .addAction(R.drawable.ic_delete, cancel, intent)
         */
        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setProgress(100, 10, true)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    /**
     * Create a notification channel
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager
        val channelId = context.getString(R.string.message_worker_notifications_channel_id)
        val channelName = context.getString(R.string.title_message_worker_notifications)
        val channelDesc = context.getString(R.string.desc_message_worker_notifications)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        channel.description = channelDesc

        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIFICATION_ID = 103
        const val WORKER_NAME = "file_upload_worker"
        private val TAG = FileUploadWorker::class.java.simpleName
    }
}