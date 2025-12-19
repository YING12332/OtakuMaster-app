package com.example.otakumaster.data.repository

import com.example.otakumaster.core.IdGenerator
import com.example.otakumaster.data.db.OtakuDatabase
import com.example.otakumaster.data.db.entities.AnimeStatusEventEntity

/**
 * AnimeStatusEventRepository：状态时间线表的业务入口
 * 规则：事件不可删除、不可覆盖；每次状态变化插入一条新记录即可。
 */
class AnimeStatusEventRepository(private val db: OtakuDatabase) {

    private val dao = db.animeStatusEventDao() // 状态事件 Dao

    /**
     * 新增状态事件（时间线的一条记录）
     * - animeId：所属番剧
     * - status：plan/watching/completed/dropped
     * - changedAt：发生时间（默认当前时间）
     */
    suspend fun addEvent(animeId: String, status: String, changedAt: Long = System.currentTimeMillis()): AnimeStatusEventEntity {
        val event = AnimeStatusEventEntity(
            id = IdGenerator.newId(),
            animeId = animeId,
            status = status,
            changedAt = changedAt,
            extraJson = "{}"
        )
        dao.insert(event)
        return event
    }

    suspend fun timelineAsc(animeId: String): List<AnimeStatusEventEntity> = dao.getTimelineAsc(animeId) // 时间线正序（最早→最新）

    suspend fun timelineDesc(animeId: String): List<AnimeStatusEventEntity> = dao.getTimelineDesc(animeId) // 时间线倒序（最新→最早）

    suspend fun latest(animeId: String): AnimeStatusEventEntity? = dao.getLatestEvent(animeId) // 最新一次状态事件

    suspend fun count(animeId: String): Int = dao.countByAnimeId(animeId) // 状态变更次数（统计/调试）
}
