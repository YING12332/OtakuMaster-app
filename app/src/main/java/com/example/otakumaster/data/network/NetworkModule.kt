package com.example.otakumaster.data.network

import com.example.otakumaster.data.network.api.VersionApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    // ✅ 你后端的 baseUrl：必须以 / 结尾
    private const val BASE_URL = "http://ying12332.top:8080/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // 版本检查返回很小，但服务器可能慢一点，给个稳妥超时
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val versionApi: VersionApi by lazy {
        retrofit.create(VersionApi::class.java)
    }
}
