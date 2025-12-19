package com.example.otakumaster.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

//番剧主表
@Entity(tableName = "anime", indices = [
    Index(value = ["title"]),
    Index(value = ["createdAt"]),
    Index(value = ["currentStatus"])
])
data class AnimeEntity(
    @PrimaryKey
    val id:String,
    //番剧唯一标识；用于关联文本/时间线/系列；未来导出/导入 JSON 用这个稳定对齐。
    val title:String,
    //番剧标题（展示用）。
    val description:String="目前没有简介哦",
    //番剧简介区域。
    val currentStatus:String,
    //番剧“当前状态”，用于快速筛选（想看plan/在看watching/看过completed/弃番dropped）。
    val tags:List<String> = emptyList(),
    //自定义标签列表，例如 ["治愈","热血"]；默认空列表。
    val seriesId:String?=null,//关联到series.id
    //关联系列id,默认为空
    val createdAt:Long,
    //番剧记录创建时间（通常等于首次加入“想看”的时间）。
    val isDeleted: Boolean=false,
    //软删除标记。true = 在正常列表不显示（未来回收站用）。
    val deletedAt: Long?=null,
    //软删除发生的时间（回收站排序/清理策略用）。
    val extraJson: String="{}"
    //扩展兜底字段，未来新增功能但不想立刻改表结构时可以先存这里；导出/导入也能带走。
)
