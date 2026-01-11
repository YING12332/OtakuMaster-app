package com.example.otakumaster.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.otakumaster.OtakuMasterApp
import com.example.otakumaster.data.db.entities.AnimeEntity
import com.example.otakumaster.ui.navigation.AppRoute
import com.example.otakumaster.ui.screens.home.components.HomeAnimeList
import com.example.otakumaster.ui.screens.home.components.HomeFilterBar
import com.example.otakumaster.ui.screens.home.components.HomeSearchRow
import com.example.otakumaster.ui.screens.home.model.AnimeStatusTab

@Composable
fun HomeScreen(
    navController: NavHostController

) {
    var query by remember { mutableStateOf("") }//搜索框的输入内容
    var selectedTab by rememberSaveable { mutableStateOf(AnimeStatusTab.ALL) }//状态按钮的状态
    var folded by rememberSaveable { mutableStateOf(false) }//折叠按钮状态
    var sortLabel by remember { mutableStateOf("最近更新") }//排序按钮状态

    val app = LocalContext.current.applicationContext as OtakuMasterApp
    val repo = app.animeRepository

    var animeList by remember { mutableStateOf<List<AnimeEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
//                .background(Color.LightGray)
                .height(55.dp)
                .padding(start = 5.dp, end = 5.dp)
        ) {
            HomeSearchRow(
                query = query,
                onQueryChange = { query = it },
                onAddAnimeClick = { navController.navigate(AppRoute.AddAnime.route) },
                onAddSeriesClick = { navController.navigate(AppRoute.AddSeries.route) }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
//                .background(Color.Gray)
                .height(45.dp)
        ) {
            HomeFilterBar(
                selectedTab = selectedTab,
                onTabChange = { selectedTab = it },
                folded = folded,
                onFoldChange = { folded = !folded }
//                sortLabel = sortLabel,
//                sortOptions = listOf("最近更新", "最近观看", "名称 A-Z"),
//                onSortSelected = { sortLabel = it }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            HomeAnimeList(
                selectedTab = selectedTab,
                query = query,
                folded = folded,
                onAnimeClick = { animeId ->
                    navController.navigate(AppRoute.AnimeDetail.create(animeId))
                },
                onSeriesClick = { seriesId ->
                    navController.navigate(AppRoute.SeriesDetail.create(seriesId))
                }
            )
        }
    }
}
