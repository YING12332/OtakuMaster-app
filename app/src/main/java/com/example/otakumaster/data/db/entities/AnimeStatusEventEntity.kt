package com.example.otakumaster.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

//状态时间线表
//每一次状态变化新增一条记录，记录不可删除。
@Entity(tableName = "anime_status_event",
    foreignKeys = [
    ForeignKey(
        entity = AnimeEntity::class,
        parentColumns = ["id"],
        childColumns = ["animeId"]
    )
], indices = [
    Index(value = ["animeId", "changedAt"])
]
)
data class AnimeStatusEventEntity(
    @PrimaryKey
    val id:String,
    //每条状态事件唯一标识（导出/导入、同步合并时有用）。

    val animeId: String,//关联到anime.id
    //该状态事件属于哪个番剧。

    val status: String,//（enum: plan / watching / completed / dropped）
    //本次事件记录的状态。

    val changedAt: Long,
    //状态变更发生的时间，用于按时间线展示与统计。

    val extraJson: String="{}"
    //扩展兜底字段（例如未来加：操作来源、设备标识、同步冲突标记等）。
)
