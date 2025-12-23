package com.example.otakumaster.ui.screens.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.otakumaster.OtakuMasterApp
import com.example.otakumaster.R
import com.example.otakumaster.data.db.entities.AnimeEntity
import com.example.otakumaster.data.query.AnimeQueryParams
import com.example.otakumaster.data.query.AnimeScope
import com.example.otakumaster.data.query.AnimeSortField
import com.example.otakumaster.data.query.AnimeStatus
import com.example.otakumaster.data.query.SortDirection
import com.example.otakumaster.ui.screens.home.model.AnimeStatusTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Step 4-1：支持 folded 折叠
 * - folded=false：全部显示为单番卡片
 * - folded=true：有 seriesId 的合并为系列卡片；无 seriesId 仍单番显示
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeAnimeList(
    selectedTab: AnimeStatusTab,
    query: String,
    folded: Boolean,
    onAnimeClick: (String) -> Unit,
    onSeriesClick: (String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OtakuMasterApp
    val animeRepo = app.animeRepository
    val seriesRepo = app.animeSeriesRepository

    // UI state：最终渲染用的 items（包含 Anime / Series 两种）
    var uiItems by remember { mutableStateOf<List<HomeListItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // 自适应列数
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val columns = rememberGridColumns(screenWidthDp)

    // 查询参数（tab + keyword）
    val params = remember(selectedTab, query) {
        AnimeQueryParams(
            scope = if (selectedTab == AnimeStatusTab.ALL) AnimeScope.ALL else AnimeScope.BY_STATUS,
            status = if (selectedTab == AnimeStatusTab.ALL) null else selectedTab.toAnimeStatus(),
            keyword = query.trim().ifBlank { null },
            sortField = AnimeSortField.CREATED_AT,
            sortDirection = SortDirection.DESC
        )
    }

    // 拉取 DB + 组装 uiItems
    LaunchedEffect(params, folded) {
        isLoading = true
        loadError = null
        try {
            val built = withContext(Dispatchers.IO) {
                val list = animeRepo.list(params)
                buildHomeListItems(
                    animeList = list,
                    folded = folded,
                    seriesNameProvider = { seriesId ->
                        seriesRepo.getActiveById(seriesId)?.name
                    }
                )
            }
            uiItems = built
        } catch (e: Exception) {
            loadError = e.message ?: "加载失败"
            uiItems = emptyList()
        } finally {
            isLoading = false
        }
    }

    // UI：空/加载/错误
    if (isLoading && uiItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("加载中…", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    if (loadError != null && uiItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("加载失败：$loadError", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    if (uiItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无番剧", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    // Grid 渲染两种 item
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(uiItems, key = { it.key }) { item ->
            when (item) {
                is HomeListItem.Anime -> {
                    AnimeCard(
                        title = item.anime.title,
                        sub = buildString { append(item.anime.currentStatus.toZhStatus()) },
                        onClick = { onAnimeClick(item.anime.id) }
                    )
                }

                is HomeListItem.Series -> {
                    SeriesCard(
                        title = item.seriesName,
                        sub = "共 ${item.count} 部",
                        onClick = { onSeriesClick(item.seriesId) }
                    )
                }
            }
        }
    }
}

/** ---------- 折叠合并：核心逻辑（纯函数，方便测试/维护） ---------- */
private suspend fun buildHomeListItems(
    animeList: List<AnimeEntity>,
    folded: Boolean,
    seriesNameProvider: suspend (String) -> String?
): List<HomeListItem> {
    if (!folded) {
        return animeList.map { HomeListItem.Anime(it) }
    }

    val withSeries = animeList.filter { !it.seriesId.isNullOrBlank() }
    val withoutSeries = animeList.filter { it.seriesId.isNullOrBlank() }

    // seriesId -> List<AnimeEntity>
    val grouped = withSeries.groupBy { it.seriesId!! }

    // 组装 series card
    val seriesItems = grouped.entries.map { (seriesId, list) ->
        val name = seriesNameProvider(seriesId) ?: "未命名系列"
        val latest = list.maxOf { it.createdAt }
        HomeListItem.Series(
            seriesId = seriesId,
            seriesName = name,
            count = list.size,
            latestCreatedAt = latest
        )
    }
    // 无系列的仍按原列表顺序（已按 createdAt DESC 排好）
    val animeItems = withoutSeries.map { HomeListItem.Anime(it) }

    // ✅ 混排：按 createdAt DESC 排序
    return (seriesItems + animeItems).sortedWith { a, b ->
        val ta = when (a) {
            is HomeListItem.Anime -> a.anime.createdAt
            is HomeListItem.Series -> a.latestCreatedAt
        }
        val tb = when (b) {
            is HomeListItem.Anime -> b.anime.createdAt
            is HomeListItem.Series -> b.latestCreatedAt
        }
        tb.compareTo(ta) // DESC
    }
}

/** ---------- UI 卡片 ---------- */

@Composable
private fun AnimeCard(
    title: String,
    sub: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun SeriesCard(
    title: String,
    sub: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomStart)
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_series),
                contentDescription = "系列",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomEnd).height(10.dp).width(10.dp)
            )
        }
    }
}

/** ---------- Grid 列数 ---------- */
private fun rememberGridColumns(screenWidthDp: Int): Int {
    return when {
        screenWidthDp < 360 -> 3
        screenWidthDp < 600 -> 3
        screenWidthDp < 840 -> 4
        screenWidthDp < 1024 -> 5
        else -> 6
    }
}

/** ---------- Tab -> AnimeStatus ---------- */
private fun AnimeStatusTab.toAnimeStatus(): AnimeStatus {
    return when (this) {
        AnimeStatusTab.ALL -> error("ALL 不应转换为 AnimeStatus（scope=ALL 时 status 必须为 null）")
        AnimeStatusTab.PLAN -> AnimeStatus.PLAN
        AnimeStatusTab.WATCHING -> AnimeStatus.WATCHING
        AnimeStatusTab.COMPLETED -> AnimeStatus.COMPLETED
        AnimeStatusTab.DROPPED -> AnimeStatus.DROPPED
    }
}

/** ---------- Home 列表 item（两种） ---------- */
private sealed class HomeListItem(val key: String) {
    class Anime(val anime: AnimeEntity) : HomeListItem("anime_${anime.id}")
    class Series(
        val seriesId: String,
        val seriesName: String,
        val count: Int,
        val latestCreatedAt: Long
    ) : HomeListItem("series_$seriesId")
}

private fun String.toZhStatus(): String = when (this) {
    "plan" -> "想看"
    "watching" -> "在看"
    "completed" -> "看完"
    "dropped" -> "弃番"
    else -> this
}

