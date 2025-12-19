package com.example.otakumaster.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

//番剧系列表
@Entity(tableName = "anime_series", indices = [Index(value = ["name"])])
data class AnimeSeriesEntity(
    @PrimaryKey
    val id:String,
    //系列唯一标识，供 anime.seriesId 引用。

    val name: String,
    //系列展示名，例如“番剧A”。

    val isDeleted: Boolean=false,
    //软删除系列（未来可能用到：系列合并/删除）。

    val deletedAt: Long?=null,
    //系列软删除时间。

    val extraJson: String="{}"
    //扩展兜底字段（未来加系列封面、排序、备注等）。
)
