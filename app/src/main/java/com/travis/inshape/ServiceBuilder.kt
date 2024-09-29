package com.travis.inshape

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ServiceBuilder {
    private const val BASE_URL = "https://api.jsonbin.io"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun <T> buildService(serviceType: Class<T>): T {
        return retrofit.create(serviceType)
    }
}