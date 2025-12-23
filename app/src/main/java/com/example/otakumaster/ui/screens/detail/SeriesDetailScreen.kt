package com.example.otakumaster.ui.screens.detail

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.otakumaster.OtakuMasterApp
import com.example.otakumaster.data.db.entities.AnimeEntity
import com.example.otakumaster.data.db.entities.AnimeSeriesEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SeriesDetailScreen（稳定版）
 * - 不使用 return@Column（避免 Compose group 不平衡导致崩溃）
 * - 不在 onClick 内使用 LaunchedEffect（用 scope.launch）
 * - Column 内的 LazyVerticalGrid 使用 weight(1f)，不使用 fillMaxSize()
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SeriesDetailScreen(
    navController: NavHostController,
    seriesId: String
) {
    val context = LocalContext.current
    val app = context.applicationContext as OtakuMasterApp
    val seriesRepo = app.animeSeriesRepository
    val animeRepo = app.animeRepository
    val scope = rememberCoroutineScope()

    // ---------- UI state ----------
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var series by remember { mutableStateOf<AnimeSeriesEntity?>(null) }
    var animeList by remember { mutableStateOf<List<AnimeEntity>>(emptyList()) }

    var isEditing by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var isSavingName by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    suspend fun reload() {
        isLoading = true
        loadError = null
        try {
            val s = withContext(Dispatchers.IO) { seriesRepo.getActiveById(seriesId) }
            series = s
            nameInput = s?.name.orEmpty()

            animeList = withContext(Dispatchers.IO) { animeRepo.listBySeriesId(seriesId) }
        } catch (e: Exception) {
            loadError = e.message ?: "加载失败"
            series = null
            animeList = emptyList()
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(seriesId) { reload() }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val columns = rememberGridColumns(screenWidthDp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(series?.name ?: "系列详情") },
                navigationIcon = {
                    TextButton(
                        enabled = !isSavingName && !isDeleting,
                        onClick = { navController.popBackStack() }
                    ) { Text("返回") }
                },
                actions = {
                    TextButton(
                        enabled = !isLoading && !isSavingName && !isDeleting && series != null,
                        onClick = {
                            isEditing = true
                            nameError = null
                            nameInput = series?.name.orEmpty()
                        }
                    ) { Text(if (isEditing) "编辑中" else "编辑") }

                    TextButton(
                        enabled = !isLoading && !isSavingName && !isDeleting && series != null,
                        onClick = { showDeleteDialog = true }
                    ) { Text("删除", color = MaterialTheme.colorScheme.error) }
                },
                windowInsets = WindowInsets(0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                isLoading && series == null -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("加载中…", color = MaterialTheme.colorScheme.onBackground)
                    }
                }

                loadError != null && series == null -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("加载失败：$loadError", color = MaterialTheme.colorScheme.error)
                    }
                }

                series == null -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("系列不存在或已删除", color = MaterialTheme.colorScheme.onBackground)
                    }
                }

                else -> {
                    // ===== 卡片：系列信息 / 编辑 =====
                    ElevatedCard {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("系列信息", style = MaterialTheme.typography.titleMedium)

                            if (!isEditing) {
                                Text(
                                    text = series?.name.orEmpty(),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "共 ${animeList.size} 部（当前版本无法在系列中添加番剧）",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = {
                                        nameInput = it
                                        if (nameError != null) nameError = null
                                    },
                                    singleLine = true,
                                    label = { Text("系列名称") },
                                    isError = nameError != null,
                                    supportingText = {
                                        if (nameError != null) {
                                            Text(nameError!!, color = MaterialTheme.colorScheme.error)
                                        } else {
                                            Text("修改后将同步到首页折叠系列卡片名称")
                                        }
                                    },
                                    enabled = !isSavingName && !isDeleting,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        enabled = !isSavingName && !isDeleting,
                                        onClick = {
                                            isEditing = false
                                            nameError = null
                                            nameInput = series?.name.orEmpty()
                                        }
                                    ) { Text("取消") }

                                    TextButton(
                                        enabled = !isSavingName && !isDeleting && nameInput.trim().isNotEmpty(),
                                        onClick = {
                                            val newName = nameInput.trim()
                                            if (newName.isEmpty()) {
                                                nameError = "系列名不能为空"
                                                toast("请先填写系列名")
                                                return@TextButton
                                            }

                                            isSavingName = true
                                            scope.launch {
                                                try {
                                                    withContext(Dispatchers.IO) {
                                                        seriesRepo.renameSeries(seriesId, newName)
                                                    }
                                                    series = series?.copy(name = newName)
                                                    isEditing = false
                                                    toast("已保存")
                                                } catch (e: Exception) {
                                                    toast("保存失败：${e.message ?: "未知错误"}")
                                                } finally {
                                                    isSavingName = false
                                                }
                                            }
                                        }
                                    ) { Text(if (isSavingName) "保存中…" else "保存") }
                                }
                            }
                        }
                    }

                    // ===== 列表：该系列下番剧 =====
                    if (animeList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("该系列下暂无番剧", color = MaterialTheme.colorScheme.onBackground)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(animeList, key = { it.id }) { anime ->
                                AnimeCard(
                                    title = anime.title,
                                    sub = anime.currentStatus.toZhStatus(),
                                    onClick = {
                                        // TODO：跳转番剧详情页
                                        // navController.navigate(AppRoute.AnimeDetail.create(anime.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ===== 删除确认弹窗 =====
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text("删除系列") },
            text = { Text("确定要删除该系列吗？\n系列中的番剧不会被删除。") },
            confirmButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = {
                        isDeleting = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    seriesRepo.softDeleteSeries(seriesId)
                                }
                                toast("已删除")
                                showDeleteDialog = false
                                navController.popBackStack()
                            } catch (e: Exception) {
                                toast("删除失败：${e.message ?: "未知错误"}")
                                isDeleting = false
                            }
                        }
                    }
                ) {
                    Text(if (isDeleting) "删除中…" else "确认删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = { showDeleteDialog = false }
                ) { Text("取消") }
            }
        )
    }
}

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

private fun rememberGridColumns(screenWidthDp: Int): Int {
    return when {
        screenWidthDp < 360 -> 3
        screenWidthDp < 600 -> 3
        screenWidthDp < 840 -> 4
        screenWidthDp < 1024 -> 5
        else -> 6
    }
}

private fun String.toZhStatus(): String = when (this) {
    "plan" -> "想看"
    "watching" -> "在看"
    "completed" -> "看完"
    "dropped" -> "弃番"
    else -> this
}
