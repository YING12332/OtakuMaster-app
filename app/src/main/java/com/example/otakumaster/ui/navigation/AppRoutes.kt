package com.example.otakumaster.ui.navigation

sealed class AppRoute(val route: String, val label: String) {
    data object Home : AppRoute("home", "首页")
    data object NewFeature : AppRoute("new_feature", "新功能")
    data object Profile : AppRoute("profile", "我的")
}

val bottomRoutes = listOf(
    AppRoute.Home,
    AppRoute.NewFeature,
    AppRoute.Profile
)
