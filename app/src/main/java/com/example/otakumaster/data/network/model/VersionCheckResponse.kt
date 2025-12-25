package com.example.otakumaster.data.network.model

/**
 * /api/v1/version 返回的数据结构（与后端 JSON 字段保持一致）
 */
data class VersionCheckResponse(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val minSupportedVersionCode: Int,
    val minSupportedVersionName: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val forceUpdate: Boolean,
    val forceUpdateMessage: String,
    val checksumSha256: String?,
    val apkSizeBytes: Long
)