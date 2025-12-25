package com.example.otakumaster.data.network.api

import com.example.otakumaster.data.network.model.VersionCheckResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface VersionApi {

    @GET("/api/v1/version")
    suspend fun checkVersion(
        @Query("platform") platform: String,
        @Query("channel") channel: String,
        @Query("currentVersionCode") currentVersionCode: Int
    ): VersionCheckResponse
}
