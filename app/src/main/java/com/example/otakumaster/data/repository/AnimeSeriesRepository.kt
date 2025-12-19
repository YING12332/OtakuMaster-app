package com.example.otakumaster.data.repository

import com.example.otakumaster.core.IdGenerator
import com.example.otakumaster.data.db.OtakuDatabase
import com.example.otakumaster.data.db.entities.AnimeSeriesEntity

/**
 * AnimeSeriesRepository：系列表的业务入口
 * 作用：封装 AnimeSeriesDao 的常用操作，统一软删除规则与创建逻辑；V1 不做“按创建时间排序”（你没有 createdAt 字段）。
 */
class AnimeSeriesRepository(private val db: OtakuDatabase) {

    private val dao = db.animeSeriesDao() // 系列表 Dao

    suspend fun listNameAsc(): List<AnimeSeriesEntity> = dao.getAllActiveByNameAsc() // 系列列表：名字A→Z

    suspend fun listNameDesc(): List<AnimeSeriesEntity> = dao.getAllActiveByNameDesc() // 系列列表：名字Z→A

    suspend fun getById(id: String): AnimeSeriesEntity? = dao.getById(id) // 单条：不过滤删除（回收站/调试）

    suspend fun getActiveById(id: String): AnimeSeriesEntity? = dao.getActiveById(id) // 单条：未删除（正常页面）

    suspend fun existsExactName(name: String): Boolean = dao.countByExactNameActive(name) > 0 // 完全重复检查（系列名）

    /**
     * 新增系列：默认未删除；extraJson 默认 {}
     * 说明：是否做“系列名重复检查”由你上层 UI 决定；这里提供 existsExactName 配合使用即可。
     */
    suspend fun createSeries(name: String): AnimeSeriesEntity {
        val series = AnimeSeriesEntity(
            id = IdGenerator.newId(),
            name = name,
            isDeleted = false,
            deletedAt = null,
            extraJson = "{}"
        )
        dao.insert(series)
        return series
    }

    suspend fun updateSeries(series: AnimeSeriesEntity) = dao.update(series) // 更新整行（改名等）

    suspend fun renameSeries(id: String, newName: String) { // 便捷改名：读取后 copy 更新（上层也可直接 updateSeries）
        val current = dao.getById(id) ?: return
        dao.update(current.copy(name = newName))
    }

    suspend fun softDeleteSeries(id: String, now: Long = System.currentTimeMillis()) = dao.softDelete(id, now) // 软删除系列

    suspend fun restoreSeries(id: String) = dao.restore(id) // 恢复系列
}
