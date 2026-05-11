package com.tracker.bustracker.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor(private val apiKey: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url = original.url.newBuilder()
            .addQueryParameter("app_key", apiKey)
            .build()
        return chain.proceed(original.newBuilder().url(url).build())
    }
}
