package com.example.otakumaster.ui.navigation

sealed class AppRoute(val route: String, val label: String) {
    data object Home : AppRoute("home", "首页")
    data object MiddlePage : AppRoute("middle_page", "中间页")
    data object Profile : AppRoute("profile", "我的")
    data object AddAnime : AppRoute("add_anime", "添加番剧")
    data object AddSeries : AppRoute("add_series", "添加系列")
    data object SeriesDetail : AppRoute("series_detail/{seriesId}","系列详情") {
        fun create(seriesId: String) = "series_detail/$seriesId"
    }
    data object AnimeDetail : AppRoute("anime_detail/{animeId}","番剧详情") {
        fun create(animeId: String) = "anime_detail/$animeId"
    }

}

val bottomRoutes = listOf(
    AppRoute.Home,
    AppRoute.MiddlePage,
    AppRoute.Profile
)
