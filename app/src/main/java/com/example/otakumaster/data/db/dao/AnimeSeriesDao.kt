package com.example.otakumaster.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.otakumaster.data.db.entities.AnimeSeriesEntity

@Dao
interface AnimeSeriesDao {

    // ---------- 写入/更新 ----------

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(series: AnimeSeriesEntity) // 新增系列（UUID 主键，避免误覆盖）

    @Update
    suspend fun update(series: AnimeSeriesEntity) // 更新整行（改 name/软删等）

    @Query("UPDATE anime_series SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long) // 软删除系列（不物理删除）

    @Query("UPDATE anime_series SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: String) // 恢复系列（未来回收站/误删恢复用）

    // ---------- 单条查询 ----------

    @Query("SELECT * FROM anime_series WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AnimeSeriesEntity? // 不过滤删除（调试/回收站用）

    @Query("SELECT * FROM anime_series WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getActiveById(id: String): AnimeSeriesEntity? // 正常显示用

    // ---------- 列表查询 ----------

    @Query("SELECT * FROM anime_series WHERE isDeleted = 0 ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllActiveByNameAsc(): List<AnimeSeriesEntity> // 系列列表按名字A→Z（不区分大小写）

    @Query("SELECT * FROM anime_series WHERE isDeleted = 0 ORDER BY name COLLATE NOCASE DESC")
    suspend fun getAllActiveByNameDesc(): List<AnimeSeriesEntity> // 系列列表按名字Z→A

    // ---------- V1 完全重复检查（系列名精确匹配，可选但建议） ----------

    @Query("SELECT COUNT(1) FROM anime_series WHERE isDeleted = 0 AND name = :name")
    suspend fun countByExactNameActive(name: String): Int // 系列名完全重复检查（未来创建系列时可用）
}
