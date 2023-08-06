package com.mkdevelopers.fileupload

import okhttp3.Interceptor
import okhttp3.Response

const val DEFAULT_USER_AGENT = "Android"

class UserAgentInterceptor(
    private val value: String = System.getProperty("http.agent") ?: DEFAULT_USER_AGENT
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        proceed(
            request()
                .newBuilder()
                .addHeader("User-Agent", value)
                .build()
        )
    }
}