package com.example.otakumaster.data.query

// 此文件为番剧表的查询字段，做到前端可以自定义查询，不需要为每种查询都写一次dao
enum class AnimeScope {
    ALL,        // 全部番剧
    BY_STATUS   // 仅某一个状态下的番剧
}
enum class AnimeStatus(val value: String) {
    PLAN("plan"),          // 想看
    WATCHING("watching"),  // 在看
    COMPLETED("completed"),// 看过
    DROPPED("dropped")     // 弃番
}
enum class AnimeSortField {
    CREATED_AT,  // 按创建时间排序
    TITLE        // 按标题排序（字母顺序）
}
enum class SortDirection {
    ASC,   // 正序
    DESC   // 倒序
}
// keyword == null 或 blank → 不搜索
// keyword 有值 → 启用模糊搜索
data class AnimeQueryParams(
    val scope: AnimeScope = AnimeScope.ALL,// AnimeScope.ALL时为查询所有，AnimeScope.BY_STATUS时为按状态查询并启用status字段
    val status: AnimeStatus? = null,     // 仅当 scope = BY_STATUS 时使用
    val sortField: AnimeSortField = AnimeSortField.CREATED_AT,
    val sortDirection: SortDirection = SortDirection.DESC,
    val keyword: String? = null           // 模糊搜索关键字，可空
)
