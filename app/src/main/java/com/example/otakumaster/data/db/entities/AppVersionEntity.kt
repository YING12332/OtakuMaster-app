package com.example.otakumaster.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * App 版本信息表（单例表）
 * 用于记录当前安装版本、上一次运行版本、启动时间，以及是否提示非强制更新
 * 本表与账号无关，整台设备只会有一条记录
 */
@Entity(tableName = "app_version")
data class AppVersionEntity(

    @PrimaryKey
    val id: Int = 1,
    // 固定为 1，用于保证这是一张“单例表”，永远只会有一行数据
    val versionCode: Int,
    // 当前安装的 App 版本号（用于与云端版本比较，判断是否有更新）
    val versionName: String,
    // 当前安装的 App 版本名（用于展示给用户，例如 1.0.0）
    val lastVersionCode: Int,
    // 上一次启动时记录的版本号
    // 若本次 versionCode != lastVersionCode，说明这是升级/降级后的首次启动
    val lastLaunchAt: Long,
    // 上一次启动 App 的时间（UTC 毫秒）
    // 可用于统计、排错，或未来提示“多久没启动/没备份”
    val showOptionalUpdate: Int,
    // 是否提示“非强制更新”
    // 1 = 有新版本时提示用户（默认）
    // 0 = 即使有新版本也不提示（强制更新除外）
    val extraJson: String="{}"
    // 扩展字段（JSON 字符串）
    // 用于未来存放云端返回的版本信息、上次检查时间等，而不修改表结构
)
