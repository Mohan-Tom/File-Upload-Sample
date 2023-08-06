package com.mkdevelopers.fileupload

import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface FileApi {

    @POST("/file")
    @Multipart
    suspend fun uploadFile(
        @Part image: MultipartBody.Part
    )

    companion object {
        val instance: FileApi by lazy {
            Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/")
                .build()
                .create(FileApi::class.java)
        }
    }
}