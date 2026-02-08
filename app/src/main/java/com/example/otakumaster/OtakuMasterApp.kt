package com.example.otakumaster

import android.app.Application
import com.example.otakumaster.data.db.OtakuDatabase
import com.example.otakumaster.data.repository.AnimeRepository
import com.example.otakumaster.data.repository.AnimeSeriesRepository
import com.example.otakumaster.data.repository.AnimeStatusEventRepository
import com.example.otakumaster.data.repository.AnimeTextEntryRepository
import com.example.otakumaster.data.repository.AppVersionRepository

class OtakuMasterApp :Application(){
    val database: OtakuDatabase
        get() = OtakuDatabase.get(this)

    val appVersionRepository: AppVersionRepository
        get() = AppVersionRepository(database.appVersionDao())

    val animeRepository: AnimeRepository
        get() = AnimeRepository(database)

    val animeSeriesRepository: AnimeSeriesRepository
        get() = AnimeSeriesRepository(database)

    val animeStatusEventRepository: AnimeStatusEventRepository
        get() = AnimeStatusEventRepository(database)

    val animeTextEntryRepository: AnimeTextEntryRepository
        get() = AnimeTextEntryRepository(database)

    override fun onCreate() {
        super.onCreate()
        // 现在什么都不用做
        // 以后可以在这里初始化日志、Repository、全局配置等
    }
}
