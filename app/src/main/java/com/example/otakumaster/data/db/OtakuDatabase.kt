package com.example.otakumaster.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
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
}
