package com.example.otakumaster.ui.navigation

sealed class AppRoute(val route: String, val label: String) {
    data object Home : AppRoute("home", "首页")
    data object NewFeature : AppRoute("new_feature", "新功能")
    data object Profile : AppRoute("profile", "我的")
    data object AddAnime : AppRoute("add_anime", "添加番剧")
    data object AddSeries : AppRoute("add_series", "添加系列")
    object SeriesDetail : AppRoute("series_detail/{seriesId}","系列详情") {
        fun create(seriesId: String) = "series_detail/$seriesId"
    }
    object AnimeDetail : AppRoute("anime_detail/{animeId}","番剧详情") {
        fun create(animeId: String) = "anime_detail/$animeId"
    }

}

val bottomRoutes = listOf(
    AppRoute.Home,
    AppRoute.NewFeature,
    AppRoute.Profile
)
