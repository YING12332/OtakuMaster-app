package com.example.otakumaster.data.repository

import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.otakumaster.core.IdGenerator
import com.example.otakumaster.data.db.OtakuDatabase
import com.example.otakumaster.data.db.entities.AnimeEntity
import com.example.otakumaster.data.db.entities.AnimeStatusEventEntity
import com.example.otakumaster.data.query.AnimeQueryParams
import com.example.otakumaster.data.query.AnimeScope
import com.example.otakumaster.data.query.AnimeSortField
import com.example.otakumaster.data.query.SortDirection

/**
 * AnimeRepository：番剧主表的“业务入口”
 * 作用：把前端传来的 AnimeQueryParams 转成安全的 SQL（白名单），调用 AnimeDao.rawQueryList 执行查询；同时封装新增/更新/软删除/恢复/重复检查等操作。
 * 注意：当前版本只处理 Anime 主表；后续做时间线/文本时，会在此增加事务（插入 Anime + 插入第一条状态事件等）。
 */
class AnimeRepository(private val db: OtakuDatabase) {

    private val animeDao = db.animeDao() // 统一从数据库拿 Dao，避免到处 new
    private val statusDao = db.animeStatusEventDao() // 状态事件 Dao（时间线只新增不删除）

    /** 获取所有在看的番剧列表*/
    suspend fun getWatchingAnime():List<AnimeEntity>{
        return animeDao.getWatchingAnime()
    }
    /** 通过ID获取番剧信息*/
    suspend fun getById(animeId: String): AnimeEntity? {
        return animeDao.getById(animeId)
    }
    /** 修改番剧集数*/
    suspend fun changeEpisode(animeId: String,toEpisode:Int):Boolean{
        val current=animeDao.getActiveById(animeId) ?: return false
        db.withTransaction {
            animeDao.update(current.copy(episode = toEpisode))
        }
        return true
    }

    suspend fun listBySeriesId(
        seriesId: String,
        sortField: AnimeSortField = AnimeSortField.CREATED_AT,
        sortDirection: SortDirection = SortDirection.DESC
    ): List<AnimeEntity> {
        val sql = StringBuilder()
        val args = ArrayList<Any>()

        sql.append("SELECT * FROM anime WHERE isDeleted = 0")
        sql.append(" AND seriesId = ?")
        args.add(seriesId)

        // ORDER BY：白名单映射
        val orderBy = when (sortField) {
            AnimeSortField.CREATED_AT -> "createdAt"
            AnimeSortField.TITLE -> "title COLLATE NOCASE"
        }
        val direction = when (sortDirection) {
            SortDirection.ASC -> "ASC"
            SortDirection.DESC -> "DESC"
        }

        sql.append(" ORDER BY ").append(orderBy).append(" ").append(direction)

        val query = SimpleSQLiteQuery(sql.toString(), args.toArray())
        return animeDao.rawQueryList(query)
    }



    /**
     * 获取番剧列表（核心方法）
     * - scope=ALL：查询全部未删除番剧
     * - scope=BY_STATUS：查询某个状态下的未删除番剧
     * - keyword：模糊搜索（title LIKE %keyword%），支持“全部范围”和“状态内范围”
     * - sortField + sortDirection：排序字段与方向（只允许白名单字段，避免 SQL 注入）
     * - limit/offset：可选预留（将来做“真正分页/分段加载”时可用；V1 可以不传）
     */
    suspend fun list(params: AnimeQueryParams, limit: Int? = null, offset: Int? = null): List<AnimeEntity> {
        val query = buildListQuery(params, limit, offset)
        return animeDao.rawQueryList(query)
    }

    /**
     * V1 完全重复检查（精确 title）
     * true 表示已存在同名且未删除的番剧；V1 你也可以在前端遍历做，但数据库检查更稳（不会漏软删除过滤规则）。
     */
    suspend fun existsExactTitle(title: String): Boolean {
        return animeDao.countByExactTitleActive(title) > 0
    }

    /**
     * 新增番剧（只写 Anime 主表）
     * - 自动生成 UUID（id）
     * - 默认 currentStatus 由你传入（通常是 plan）
     * - createdAt 使用当前时间（毫秒）
     * - tags 默认空列表，seriesId 默认 null（由调用者决定是否传入）
     */
    suspend fun createAnime(
        title: String,
        description: String = "目前没有简介哦",
        currentStatus: String,
        tags: List<String> = listOf(),
        seriesId: String? = null,
        now: Long = System.currentTimeMillis()
    ): AnimeEntity {
        val anime = AnimeEntity(
            id = IdGenerator.newId(), // 统一用 UUID，导入/合并不会冲突
            title = title,
            description = description,
            currentStatus = currentStatus,
            tags = tags,
            seriesId = seriesId,
            createdAt = now,
            isDeleted = false,
            deletedAt = null,
            extraJson = "{}"
        )
        animeDao.insert(anime)
        return anime
    }

    /**
     * 变更番剧状态（事务）：更新 Anime.currentStatus + 追加一条状态事件（时间线）
     * - 你的规则：状态变化记录为时间线，不覆盖旧记录；所以每次只 insert 一条 AnimeStatusEventEntity
     * - 注意：如果 anime 已软删除，你一般不应该再改状态；这里做了保护（取不到 active 就返回 false）
     */
    suspend fun changeStatus(animeId: String, toStatus: String, now: Long = System.currentTimeMillis()): Boolean {
        val current = animeDao.getActiveById(animeId) ?: return false
        if (current.currentStatus == toStatus) return true // 状态没变就不重复写事件（避免时间线被同状态刷屏）
        db.withTransaction {
            animeDao.update(current.copy(currentStatus = toStatus)) // 更新当前状态（用于列表分类筛选）
            statusDao.insert(
                AnimeStatusEventEntity(
                    id = IdGenerator.newId(),
                    animeId = animeId,
                    status = toStatus,
                    changedAt = now,
                    extraJson = "{}"
                )
            )
        }
        return true
    }


    /**
     * 更新番剧（整行更新）
     * 说明：你当前 Entity 没有 updatedAt，所以这里不处理“最近更新排序”；排序只看 createdAt（符合你当前设计）。
     */
    suspend fun updateAnime(anime: AnimeEntity) {
        animeDao.update(anime)
    }

    /**
     * 软删除番剧
     * - 不物理删除，只打 isDeleted 标记并记录 deletedAt
     * - 未来回收站功能可直接基于此实现
     */
    suspend fun softDeleteAnime(id: String, now: Long = System.currentTimeMillis()) {
        animeDao.softDelete(id, now)
    }

    /**
     * 恢复番剧（未来回收站用）
     * - 取消 isDeleted 标记，清空 deletedAt
     */
    suspend fun restoreAnime(id: String) {
        animeDao.restore(id)
    }

    // ============================
    // 内部：把前端参数安全映射为 SQL（白名单，防 SQL 注入）
    // ============================

    /**
     * 构建列表查询的 SupportSQLiteQuery（给 Room RawQuery 使用）
     * 规则：
     * 1) 永远过滤 isDeleted=0（正常列表不显示软删除）
     * 2) scope=BY_STATUS 时必须有 status，否则报错（避免生成无效 SQL）
     * 3) keyword 非空时启用 LIKE（%keyword%），同时支持全部/状态内
     * 4) ORDER BY 字段与方向只能来自 enum 白名单（CREATED_AT/TITLE + ASC/DESC）
     * 5) limit/offset 可选预留（将来分页；V1 不用也没问题）
     */
    private fun buildListQuery(params: AnimeQueryParams, limit: Int?, offset: Int?): SupportSQLiteQuery {
        val sql = StringBuilder()
        val args = ArrayList<Any>()

        sql.append("SELECT * FROM anime WHERE isDeleted = 0") // 软删除统一在数据库层过滤，前端不易漏

        // scope：全部 or 状态内
        if (params.scope == AnimeScope.BY_STATUS) {
            val status = params.status?.value ?: error("AnimeQueryParams.scope=BY_STATUS 时必须传 status")
            sql.append(" AND currentStatus = ?")
            args.add(status)
        }

        // keyword：模糊搜索（title LIKE %keyword%）
        val keyword = params.keyword?.trim()
        if (!keyword.isNullOrEmpty()) {
            sql.append(" AND title LIKE ?")
            args.add("%$keyword%") // 绑定参数，避免拼接用户输入导致注入
        }

        // ORDER BY：白名单映射（禁止前端传列名字符串）
        val orderBy = when (params.sortField) {
            AnimeSortField.CREATED_AT -> "createdAt"
            AnimeSortField.TITLE -> "title COLLATE NOCASE" // 标题排序不区分大小写，更符合用户直觉
        }
        val direction = when (params.sortDirection) {
            SortDirection.ASC -> "ASC"
            SortDirection.DESC -> "DESC"
        }
        sql.append(" ORDER BY ").append(orderBy).append(" ").append(direction)

        // limit/offset：预留分页（可选）
        if (limit != null) {
            require(limit > 0) { "limit 必须 > 0" }
            sql.append(" LIMIT ?")
            args.add(limit)
            if (offset != null) {
                require(offset >= 0) { "offset 必须 >= 0" }
                sql.append(" OFFSET ?")
                args.add(offset)
            }
        }

        return SimpleSQLiteQuery(sql.toString(), args.toArray())
    }
}
