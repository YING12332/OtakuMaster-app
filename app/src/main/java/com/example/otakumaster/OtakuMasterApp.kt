package com.example.otakumaster

import android.app.Application
import com.example.otakumaster.data.db.OtakuDatabase
import com.example.otakumaster.data.repository.AppVersionRepository

class OtakuMasterApp :Application(){
    val database:OtakuDatabase by lazy { OtakuDatabase.get(this) }
    val appVersionRepository: AppVersionRepository by lazy { AppVersionRepository(database.appVersionDao()) } // Repository 只依赖 Dao，不让 UI 直接碰 Room

    override fun onCreate() {
        super.onCreate()
        // 现在什么都不用做
        // 以后可以在这里初始化日志、Repository、全局配置等
    }

}