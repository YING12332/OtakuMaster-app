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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import com.example.otakumaster.utils.lightAndDarkColor
import com.example.otakumaster.utils.CopyToClipboard.copyTextToClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.otakumaster.OtakuMasterApp
import com.example.otakumaster.data.db.entities.AnimeEntity
import com.example.otakumaster.data.db.entities.AnimeSeriesEntity
import com.example.otakumaster.data.db.entities.AnimeTextEntryEntity
import com.example.otakumaster.ui.navigation.AppRoute
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
    val textRepo=app.animeTextEntryRepository
    val scope = rememberCoroutineScope()

    // ---------- UI state ----------
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var series by remember { mutableStateOf<AnimeSeriesEntity?>(null) }
    var animeList by remember { mutableStateOf<List<AnimeEntity>>(emptyList()) }
    var animeIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var seriesTextList by remember{mutableStateOf<List<AnimeTextEntryEntity>>(emptyList())}

    var showEditDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var isSavingName by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    var showAddTextDialog by remember { mutableStateOf(false) }
    var newTextInput by remember { mutableStateOf("") }
    var selectedAnimeIdForText by remember { mutableStateOf<String?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

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

            animeIds=animeList.map { it.id }.toSet()
            seriesTextList = withContext(Dispatchers.IO) {
                textRepo.allListByAnimeTimeDesc().filter { it.animeId in animeIds }
            }
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
                title = { 
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(series?.name ?: "系列详情")
                        if (!isLoading && series != null) {
                            Text(
                                text = "共 ${animeList.size} 部",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    TextButton(
                        enabled = !isSavingName && !isDeleting,
                        onClick = { navController.popBackStack() }
                    ) { Text("返回") }
                },
                actions = {
                    TextButton(
                        enabled = !isLoading && !isSavingName && !isDeleting && series != null,
                        onClick = { showDeleteDialog = true }
                    ) { Text("删除", color = MaterialTheme.colorScheme.error) }
                    
                    TextButton(
                        enabled = !isLoading && !isSavingName && !isDeleting && series != null,
                        onClick = {
                            showEditDialog = true
                            nameError = null
                            nameInput = series?.name.orEmpty()
                        }
                    ) { Text("编辑") }
                },
                windowInsets = WindowInsets(0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
//                .verticalScroll(rememberScrollState())
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
                                         navController.navigate(AppRoute.AnimeDetail.create(anime.id))
                                    }
                                )
                            }

                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    TextButton(onClick = { 
                                        showAddTextDialog = true 
                                        selectedAnimeIdForText = null // 每次打开弹窗默认清空选择
                                    }) {
                                        Text(text = "添加痕迹", fontSize = 16.sp)
                                    }
                                }
                            }

                            if (seriesTextList.isNotEmpty()) {
                                items(
                                    items = seriesTextList,
                                    key = { "text_${it.id}" },
                                    span = { GridItemSpan(maxLineSpan) }
                                ) { item ->
                                    val seriesTitle = series?.name ?: ""
                                    
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                copyTextToClipboard(
                                                    context, 
                                                    if (seriesTitle.isNotBlank()) "${item.content}   ——$seriesTitle" else item.content
                                                ) 
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = lightAndDarkColor(
                                                lightColor = Color(0xFFFFFFFF), 
                                                darkColor = Color(0xFF222222)
                                            )
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 20.dp, bottom = 10.dp, start = 20.dp, end = 20.dp)
                                        ) {
                                            Text(
                                                text = item.content,
                                                fontSize = 18.sp
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                if (seriesTitle.isNotEmpty()) {
                                                    Text(text = "——$seriesTitle", fontSize = 18.sp)
                                                }
                                            }
                                            Text(
                                                text = if (item.isEdited == 1) "编辑于" + com.example.otakumaster.utils.TimeUtils.formatDate(item.timeAt) else com.example.otakumaster.utils.TimeUtils.formatDate(item.timeAt),
                                                fontSize = 12.sp,
                                                color = Color(0xFF999999)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ===== 编辑系列名称弹窗 =====
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSavingName) showEditDialog = false },
            title = { Text("修改系列名称") },
            text = {
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
                    enabled = !isSavingName,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isSavingName && nameInput.trim().isNotEmpty(),
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
                                showEditDialog = false
                                toast("已保存")
                            } catch (e: Exception) {
                                toast("保存失败：${e.message ?: "未知错误"}")
                            } finally {
                                isSavingName = false
                            }
                        }
                    }
                ) { Text(if (isSavingName) "保存中…" else "保存") }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSavingName,
                    onClick = { showEditDialog = false }
                ) { Text("取消") }
            }
        )
    }

    // ===== 添加痕迹弹窗 =====
    if (showAddTextDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAddTextDialog = false }) {
            androidx.compose.material3.Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "添加痕迹",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = newTextInput,
                        onValueChange = { newTextInput = it },
                        label = { Text("添加痕迹") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧：选择番剧下拉框
                        Box {
                            TextButton(onClick = { isDropdownExpanded = true }) {
                                Text(
                                    text = animeList.find { it.id == selectedAnimeIdForText }?.title ?: "选择番剧",
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 120.dp)
                                )
                            }
                            androidx.compose.material3.DropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false }
                            ) {
                                animeList.forEach { anime ->
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = anime.title, 
                                                maxLines = 1, 
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            ) 
                                        },
                                        onClick = {
                                            selectedAnimeIdForText = anime.id
                                            isDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // 右侧：取消/添加按钮
                        Row(horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddTextDialog = false }) { Text("取消") }
                            TextButton(
                                enabled = newTextInput.trim().isNotEmpty() && selectedAnimeIdForText != null,
                                onClick = {
                                    val t = newTextInput.trim()
                                    val aId = selectedAnimeIdForText
                                    if (t.isNotEmpty() && aId != null) {
                                        scope.launch {
                                            try {
                                                withContext(Dispatchers.IO) {
                                                    textRepo.addText(aId, t)
                                                }
                                                toast("添加成功")
                                                showAddTextDialog = false
                                                newTextInput = ""
                                                selectedAnimeIdForText = null
                                                reload()
                                            } catch (e: Exception) {
                                                toast("添加失败：${e.message ?: "未知错误"}")
                                            }
                                        }
                                    }
                                }
                            ) { Text("添加") }
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
                                    animeRepo.softDelSeriesId(seriesId)
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
