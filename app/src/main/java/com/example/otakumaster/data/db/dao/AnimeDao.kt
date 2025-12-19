package com.example.otakumaster.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.otakumaster.data.db.entities.AnimeEntity

@Dao
interface AnimeDao {

    // ---------- 写入/更新 ----------

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(anime: AnimeEntity) // 新增番剧（UUID 主键，避免误覆盖）

    @Update
    suspend fun update(anime: AnimeEntity) // 更新整行（改简介/标签/系列/状态/软删字段等）

    @Query("UPDATE anime SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long) // 软删除：不物理删

    @Query("UPDATE anime SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: String) // 恢复：未来回收站用

    // ---------- 单条查询 ----------

    @Query("SELECT * FROM anime WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AnimeEntity? // 不过滤删除（回收站/调试可能用）

    @Query("SELECT * FROM anime WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getActiveById(id: String): AnimeEntity? // 正常详情用（未删除）

    // ---------- V1 完全重复检查（精确 title） ----------

    @Query("SELECT COUNT(1) FROM anime WHERE isDeleted = 0 AND title = :title")
    suspend fun countByExactTitleActive(title: String): Int // 统计未删除且 title 完全相同数量

    // ---------- 动态列表查询（B方案核心） ----------
    // 由 Repository 拼接 SQL（过滤：isDeleted、可选 status、可选 keyword；排序：createdAt/title + ASC/DESC）
    // 注意：Room 不支持 ORDER BY 列名/方向使用参数绑定，所以必须 RawQuery + 白名单拼 SQL
    @RawQuery(observedEntities = [AnimeEntity::class])
    suspend fun rawQueryList(query: SupportSQLiteQuery): List<AnimeEntity>
}
