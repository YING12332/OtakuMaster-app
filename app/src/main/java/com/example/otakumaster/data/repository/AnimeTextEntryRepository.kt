package com.example.otakumaster.data.repository

import com.example.otakumaster.core.IdGenerator
import com.example.otakumaster.data.db.OtakuDatabase
import com.example.otakumaster.data.db.entities.AnimeTextEntryEntity

/**
 * AnimeTextEntryRepository：文本表的业务入口
 * 规则：文本可编辑；你要求 timeAt 被编辑时覆盖成“编辑时间”，并用 isEdited 标记是否编辑过。
 */
class AnimeTextEntryRepository(private val db: OtakuDatabase) {

    private val dao = db.animeTextEntryDao() // 文本 Dao

    /**
     * 新增文本
     * - timeAt：自动记录当前时间（创建时间）
     * - isEdited：默认 0
     */
    suspend fun addText(animeId: String, content: String, now: Long = System.currentTimeMillis()): AnimeTextEntryEntity {
        val text = AnimeTextEntryEntity(
            id = IdGenerator.newId(),
            animeId = animeId,
            content = content,
            timeAt = now,
            isEdited = 0,
            isDeleted = false,
            deletedAt = null,
            extraJson = "{}"
        )
        dao.insert(text)
        return text
    }

    /**
     * 编辑文本（符合你规则：覆盖 timeAt 为编辑时间；isEdited=1）
     * - 先取出原文本（不管是否删除都能取到），再 copy 更新
     */
    suspend fun editText(id: String, newContent: String, now: Long = System.currentTimeMillis()): Boolean {
        val old = dao.getById(id) ?: return false
        val updated = old.copy(content = newContent, timeAt = now, isEdited = 1)
        dao.update(updated)
        return true
    }

    suspend fun getById(id: String): AnimeTextEntryEntity? = dao.getById(id) // 单条：不过滤删除（回收站/调试）

    suspend fun getActiveById(id: String): AnimeTextEntryEntity? = dao.getActiveById(id) // 单条：未删除（正常详情用）

    suspend fun listByAnimeTimeAsc(animeId: String): List<AnimeTextEntryEntity> = dao.getByAnimeIdTimeAsc(animeId) // 某番文本：最早→最新

    suspend fun listByAnimeTimeDesc(animeId: String): List<AnimeTextEntryEntity> = dao.getByAnimeIdTimeDesc(animeId) // 某番文本：最新→最早

    suspend fun allListByAnimeTimeAsc():List<AnimeTextEntryEntity> = dao.getAllByAnimeIdTimeAsc()//全部文本：最早→最新

    suspend fun allListByAnimeTimeDesc():List<AnimeTextEntryEntity> = dao.getAllByAnimeIdTimeDesc() // 全部文本：最新→最早

    suspend fun softDeleteText(id: String, now: Long = System.currentTimeMillis()) = dao.softDelete(id, now) // 软删除文本

    suspend fun restoreText(id: String) = dao.restore(id) // 恢复文本

    suspend fun softDeleteAllTextsOfAnime(animeId: String, now: Long = System.currentTimeMillis()) = dao.softDeleteByAnimeId(animeId, now) // 批量软删（可选策略）
}
