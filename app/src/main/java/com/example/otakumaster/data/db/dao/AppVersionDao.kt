package com.example.otakumaster.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.otakumaster.data.db.entities.AppVersionEntity

@Dao
interface AppVersionDao {

    // 读取单例版本信息（id 固定为 1）
    @Query("SELECT * FROM app_version WHERE id = 1 LIMIT 1")
    suspend fun get(): AppVersionEntity?

    // 插入或更新版本信息（REPLACE 用于单例 upsert）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppVersionEntity)
}
