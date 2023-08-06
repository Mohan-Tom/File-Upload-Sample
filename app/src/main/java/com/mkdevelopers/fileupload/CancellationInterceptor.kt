package com.mkdevelopers.fileupload

import okhttp3.Interceptor
import okhttp3.Response
import okio.IOException

class CancellationInterceptor : Interceptor {
    @Volatile
    private var canceled = false

    fun cancel() {
        canceled = true
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (canceled) {
            throw IOException("Call was canceled")
        }

        val request = chain.request()
        val response = chain.proceed(request)

        // Check for cancellation again after getting the response
        if (canceled) {
            response.close()
            throw IOException("Call was canceled")
        }

        return response
    }
}