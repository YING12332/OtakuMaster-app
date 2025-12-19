package com.example.otakumaster.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.otakumaster.data.db.dao.AnimeDao
import com.example.otakumaster.data.db.dao.AnimeSeriesDao
import com.example.otakumaster.data.db.dao.AnimeStatusEventDao
import com.example.otakumaster.data.db.dao.AnimeTextEntryDao
import com.example.otakumaster.data.db.dao.AppVersionDao
import com.example.otakumaster.data.db.entities.AnimeEntity
import com.example.otakumaster.data.db.entities.AnimeSeriesEntity
import com.example.otakumaster.data.db.entities.AnimeStatusEventEntity
import com.example.otakumaster.data.db.entities.AnimeTextEntryEntity
import com.example.otakumaster.data.db.entities.AppVersionEntity

/**
 * OtakuMaster 的 Room 数据库主入口
 * 所有数据表（Entity）都需要在这里注册
 * version 用于数据库结构升级（V1 先固定为 1）
 */
@Database(
    entities = [
        AppVersionEntity::class,        // 版本表
        AnimeEntity::class,             // 番剧主表
        AnimeSeriesEntity::class,       // 系列表
        AnimeStatusEventEntity::class,  // 状态时间线表
        AnimeTextEntryEntity::class     // 文本表
    ],
    version = 2
)

@TypeConverters(Converters::class) // tags: List<String> 需要转换器
abstract class OtakuDatabase : RoomDatabase() {
    // 后续会在这里声明 Dao，例如 appVersionDao()
    // 版本信息表 Dao
    abstract fun appVersionDao(): AppVersionDao
    abstract fun animeDao(): AnimeDao
    abstract fun animeSeriesDao(): AnimeSeriesDao
    abstract fun animeStatusEventDao(): AnimeStatusEventDao
    abstract fun animeTextEntryDao(): AnimeTextEntryDao

    companion object {
        @Volatile
        private var INSTANCE: OtakuDatabase? = null
        // @Volatile：保证多线程下 INSTANCE 的可见性，防止拿到旧值

        fun get(context: Context): OtakuDatabase {
            return INSTANCE ?: synchronized(this) {
                // synchronized：保证同一时间只有一个线程创建数据库
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, // 必须用 applicationContext，避免内存泄漏
                    OtakuDatabase::class.java,
                    "otaku_master.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also {INSTANCE = it}
            }
        }
    }
}
