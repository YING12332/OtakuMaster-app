package com.example.otakumaster.data.repository

import com.example.otakumaster.core.AppInfo
import com.example.otakumaster.data.db.dao.AppVersionDao
import com.example.otakumaster.data.db.entities.AppVersionEntity

class AppVersionRepository(private val dao: AppVersionDao) {

    /** 启动时调用：确保版本表存在，并更新 lastLaunchAt；暂不做云端校验 */
    suspend fun initOnAppStart(now: Long = System.currentTimeMillis()) {
        val existing = dao.get()//获取当前版本信息
        if (existing == null) {
            // 第一次启动：插入单例记录；lastVersionCode 与当前一致，表示“之前没有版本”
            dao.upsert(AppVersionEntity(id = 1, versionCode = AppInfo.VERSION_CODE, versionName = AppInfo.VERSION_NAME, lastVersionCode = AppInfo.VERSION_CODE, lastLaunchAt = now, showOptionalUpdate = 1, extraJson = "{}"))
        } else {
            // 非第一次启动：把“旧 versionCode”写进 lastVersionCode，再写入当前版本（目前固定，但结构正确）
            dao.upsert(existing.copy(lastVersionCode = existing.versionCode, versionCode = AppInfo.VERSION_CODE, versionName = AppInfo.VERSION_NAME, lastLaunchAt = now))
        }
    }
}
