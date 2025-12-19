package com.example.otakumaster.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

//番剧文本记录表
//一部番可以有多条文本；“时间字段可覆盖”为最后编辑时间，同时用一个标记表示是否编辑过。
@Entity(tableName = "anime_text_entry",
    foreignKeys = [
    ForeignKey(
        entity = AnimeEntity::class,
        parentColumns = ["id"],
        childColumns = ["animeId"]
    )
], indices = [
        Index(value = ["animeId", "timeAt"])
    ])
data class AnimeTextEntryEntity(
    @PrimaryKey
    val id:String,
    //每条文本记录唯一标识（导出/导入、编辑、删除用）。

    val animeId: String, //关联到anime.id
    //这条文本属于哪个番剧。

    val content: String,
    //文本内容（评价/观后感/台词/印象点都写这里）。

    val timeAt: Long,
    /*作用：时间展示用：
    第一次创建时 = 创建时间
    如果编辑过 = 被编辑的时间（覆盖原值）*/

    val isEdited: Int=0, //默认 0
    /*作用：是否编辑过：
    0 = 未编辑
    1 = 编辑过*/

    val isDeleted: Boolean=false,
    //软删除文本（正常列表/详情不显示，未来回收站可恢复）。

    val deletedAt: Long?=null,
    //文本软删除时间。

    val extraJson: String="{}"
    //扩展兜底字段（未来加：文本类型、心情、引用来源等）。
)
