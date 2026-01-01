package com.example.otakumaster.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.otakumaster.data.db.entities.AnimeTextEntryEntity

@Dao
interface AnimeTextEntryDao {

    // ---------- 写入/更新 ----------

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(text: AnimeTextEntryEntity) // 新增一条文本（UUID 主键）

    @Update
    suspend fun update(text: AnimeTextEntryEntity) // 更新整行（编辑文本时用：覆盖 timeAt + isEdited=1）

    @Query("UPDATE anime_text_entry SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long) // 软删除文本（未来回收站恢复）

    @Query("UPDATE anime_text_entry SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: String) // 恢复文本（未来回收站用）

    // ---------- 单条查询 ----------

    @Query("SELECT * FROM anime_text_entry WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AnimeTextEntryEntity? // 不过滤删除（回收站/调试用）

    @Query("SELECT * FROM anime_text_entry WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getActiveById(id: String): AnimeTextEntryEntity? // 正常详情用（未删除）

    // ---------- 列表查询：某番的文本（按 timeAt 排序） ----------

    @Query("SELECT * FROM anime_text_entry WHERE animeId = :animeId AND isDeleted = 0 ORDER BY timeAt ASC")
    suspend fun getByAnimeIdTimeAsc(animeId: String): List<AnimeTextEntryEntity> // 文本正序：最早→最新

    @Query("SELECT * FROM anime_text_entry WHERE animeId = :animeId AND isDeleted = 0 ORDER BY timeAt DESC")
    suspend fun getByAnimeIdTimeDesc(animeId: String): List<AnimeTextEntryEntity> // 文本倒序：最新→最早

    // ---------- 全部查询：所有番的文本（按 timeAt 排序） ----------
    @Query("SELECT * FROM anime_text_entry WHERE isDeleted = 0 ORDER BY timeAt ASC")
    suspend fun getAllByAnimeIdTimeAsc():List<AnimeTextEntryEntity>// 文本正序：最早→最新

    @Query("SELECT * FROM anime_text_entry WHERE isDeleted = 0 ORDER BY timeAt DESC")
    suspend fun getAllByAnimeIdTimeDesc():List<AnimeTextEntryEntity> // 文本倒序：最新→最早
    // ---------- 可选：批量软删除（当你以后做“删番=隐藏其文本”策略时很方便） ----------

    @Query("UPDATE anime_text_entry SET isDeleted = 1, deletedAt = :deletedAt WHERE animeId = :animeId AND isDeleted = 0")
    suspend fun softDeleteByAnimeId(animeId: String, deletedAt: Long) // 批量软删除某番所有文本（可选策略）
}
