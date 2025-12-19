package com.example.otakumaster.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.otakumaster.data.db.entities.AnimeStatusEventEntity

@Dao
interface AnimeStatusEventDao {

    // ---------- 写入 ----------

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: AnimeStatusEventEntity) // 插入一条状态事件（你的规则：不可删除、不可覆盖）

    // ---------- 时间线查询（按时间排序） ----------

    @Query("SELECT * FROM anime_status_event WHERE animeId = :animeId ORDER BY changedAt ASC")
    suspend fun getTimelineAsc(animeId: String): List<AnimeStatusEventEntity> // 时间线正序：最早→最新（展示时间线常用）

    @Query("SELECT * FROM anime_status_event WHERE animeId = :animeId ORDER BY changedAt DESC")
    suspend fun getTimelineDesc(animeId: String): List<AnimeStatusEventEntity> // 时间线倒序：最新→最早（展示最近操作常用）

    // ---------- 便捷查询（可选，但很实用） ----------

    @Query("SELECT * FROM anime_status_event WHERE animeId = :animeId ORDER BY changedAt DESC LIMIT 1")
    suspend fun getLatestEvent(animeId: String): AnimeStatusEventEntity? // 取最新一次状态事件（核对 currentStatus 或显示“最近变更”时用）

    @Query("SELECT COUNT(1) FROM anime_status_event WHERE animeId = :animeId")
    suspend fun countByAnimeId(animeId: String): Int // 统计某番状态变更次数（未来做统计/调试用）
}
