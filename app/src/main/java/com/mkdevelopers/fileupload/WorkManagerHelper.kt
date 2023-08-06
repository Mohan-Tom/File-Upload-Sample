package com.mkdevelopers.fileupload

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class WorkManagerHelper(
    private val applicationContext: Context
) {

    private val tag = WorkManagerHelper::class.simpleName

    /**
     * File upload worker
     */
    fun scheduleToUploadFiles() {
        val constraintBuilder = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<FileUploadWorker>()
            .setConstraints(constraintBuilder)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            FileUploadWorker.WORKER_NAME,
            ExistingWorkPolicy.REPLACE,
            uploadWorkRequest
        )
    }

    fun cancelUploadWork() {
        WorkManager.getInstance(applicationContext).cancelUniqueWork(FileUploadWorker.WORKER_NAME)
    }
}