package com.example.otakumaster.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.otakumaster.data.db.dao.AppVersionDao
import com.example.otakumaster.data.db.entities.AppVersionEntity

/**
 * OtakuMaster 的 Room 数据库主入口
 * 所有数据表（Entity）都需要在这里注册
 * version 用于数据库结构升级（V1 先固定为 1）
 */
@Database(
    entities = [
        AppVersionEntity::class // 当前阶段只注册版本表，后续会逐步加入 Anime / Text / Series 等
    ],
    version = 1
)
abstract class OtakuDatabase : RoomDatabase() {
    // 后续会在这里声明 Dao，例如 appVersionDao()
    // 版本信息表 Dao
    abstract fun appVersionDao(): AppVersionDao

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
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
