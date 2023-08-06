package com.mkdevelopers.fileupload

import okhttp3.MultipartBody
import okio.IOException
import retrofit2.HttpException
import java.io.File

class FileRepository {

    suspend fun uploadFile(requestBody: ProgressRequestBody, file: File): Boolean {
        return try {
            FileApi.instance.uploadFile(
                image = MultipartBody.Part
                    .createFormData(
                        "videoplayback.mp4",
                        file.name,
                        requestBody
                    )
            )
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        } catch (e: HttpException) {
            e.printStackTrace()
            false
        }
    }
}