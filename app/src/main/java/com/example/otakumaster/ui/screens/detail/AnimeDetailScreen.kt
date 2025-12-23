package com.example.otakumaster.ui.screens.detail

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.otakumaster.OtakuMasterApp
import com.example.otakumaster.data.db.entities.AnimeEntity
import com.example.otakumaster.data.db.entities.AnimeSeriesEntity
import com.example.otakumaster.data.query.AnimeQueryParams
import com.example.otakumaster.data.query.AnimeScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AnimeDetailScreen：番剧详情页
 * - 顶部：状态下拉菜单（移动到 想看/在看/看完/弃番）
 * - 编辑：按钮文字 编辑↔取消；取消会回滚编辑内容
 * - 编辑模式下：允许删除（软删除 + 二次确认）
 * - 信息顺序：标题、标签、简介、添加时间、系列
 *   - 无系列：显示“添加到系列”按钮（不需要编辑模式）
 *   - 有系列：显示系列名；要修改系列必须进入编辑模式
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnimeDetailScreen(
    navController: NavHostController,
    animeId: String
) {
    val context = LocalContext.current
    val app = context.applicationContext as OtakuMasterApp
    val animeRepo = app.animeRepository
    val seriesRepo = app.animeSeriesRepository
    val scope = rememberCoroutineScope()

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    // ---------- Load states ----------
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var anime by remember { mutableStateOf<AnimeEntity?>(null) }
    var seriesName by remember { mutableStateOf<String?>(null) }

    // ---------- Edit states ----------
    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editDesc by remember { mutableStateOf("") }
    var editTags by remember { mutableStateOf(setOf<String>()) }

    // dialogs
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }

    // series picker dialog
    var showSeriesPicker by remember { mutableStateOf(false) }
    var seriesList by remember { mutableStateOf<List<AnimeSeriesEntity>>(emptyList()) }
    var createSeriesInput by remember { mutableStateOf("") }
    var isSeriesLoading by remember { mutableStateOf(false) }

    // status menu
    var statusMenuExpanded by remember { mutableStateOf(false) }

    // saving flags
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var isChangingStatus by remember { mutableStateOf(false) }

    suspend fun reload() {
        isLoading = true
        loadError = null
        try {
            // 临时单条读取：repo.list(ALL) 再 find
            val a = withContext(Dispatchers.IO) {
                animeRepo.list(AnimeQueryParams(scope = AnimeScope.ALL))
                    .firstOrNull { it.id == animeId }
            }
            anime = a
            if (a != null) {
                editTitle = a.title
                editDesc = a.description
                editTags = a.tags.toSet()

                seriesName = withContext(Dispatchers.IO) {
                    a.seriesId?.let { sid -> seriesRepo.getActiveById(sid)?.name }
                }
            } else {
                seriesName = null
            }
        } catch (e: Exception) {
            loadError = e.message ?: "加载失败"
            anime = null
            seriesName = null
        } finally {
            isLoading = false
        }
    }

    fun cancelEdit() {
        isEditing = false
        anime?.let {
            editTitle = it.title
            editDesc = it.description
            editTags = it.tags.toSet()
        }
    }

    // ✅ 必须在 TopAppBar 使用之前定义
    fun changeStatus(a: AnimeEntity?, to: String) {
        val current = a ?: return
        if (current.currentStatus == to) return
        if (isChangingStatus) return

        isChangingStatus = true
        scope.launch {
            try {
                val now = System.currentTimeMillis()
                withContext(Dispatchers.IO) {
                    animeRepo.changeStatus(current.id, to, now)
                }
                toast("已移动到：${to.toZhStatus()}")
                reload()
            } catch (e: Exception) {
                toast("修改状态失败：${e.message ?: "未知错误"}")
            } finally {
                isChangingStatus = false
            }
        }
    }

    fun openSeriesPicker() {
        isSeriesLoading = true
        scope.launch {
            try {
                val list = withContext(Dispatchers.IO) { seriesRepo.listNameAsc() }
                seriesList = list
                showSeriesPicker = true
            } catch (e: Exception) {
                toast("加载系列失败：${e.message ?: "未知错误"}")
            } finally {
                isSeriesLoading = false
            }
        }
    }

    LaunchedEffect(animeId) { reload() }

    // ---------- UI ----------
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("番剧详情") },
                navigationIcon = {
                    TextButton(
                        enabled = !isSaving && !isDeleting && !isChangingStatus,
                        onClick = { navController.popBackStack() }
                    ) { Text("返回") }
                },
                actions = {
                    // ① 状态下拉（放顶部）
                    TextButton(
                        enabled = anime != null && !isSaving && !isDeleting && !isChangingStatus,
                        onClick = { statusMenuExpanded = true }
                    ) {
                        Text(
                            text = anime?.currentStatus?.toZhStatus() ?: "状态",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false }
                    ) {
                        StatusMenuItem("移动到想看", enabled = anime?.currentStatus != "plan") {
                            statusMenuExpanded = false
                            changeStatus(anime, "plan")
                        }
                        StatusMenuItem("移动到在看", enabled = anime?.currentStatus != "watching") {
                            statusMenuExpanded = false
                            changeStatus(anime, "watching")
                        }
                        StatusMenuItem(
                            "移动到看完",
                            enabled = anime?.currentStatus != "completed"
                        ) {
                            statusMenuExpanded = false
                            changeStatus(anime, "completed")
                        }
                        StatusMenuItem("移动到弃番", enabled = anime?.currentStatus != "dropped") {
                            statusMenuExpanded = false
                            changeStatus(anime, "dropped")
                        }
                    }

                    // ② 编辑/取消（按你要求：编辑时文字变取消，再点取消编辑）
                    TextButton(
                        enabled = anime != null && !isSaving && !isDeleting && !isChangingStatus,
                        onClick = {
                            if (!isEditing) isEditing = true else cancelEdit()
                        }
                    ) { Text(if (isEditing) "取消" else "编辑") }
                },
                windowInsets = WindowInsets(0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                isLoading -> {
                    Text("加载中…", color = MaterialTheme.colorScheme.onBackground)
                }

                loadError != null -> {
                    Text("加载失败：$loadError", color = MaterialTheme.colorScheme.error)
                }

                anime == null -> {
                    Text("番剧不存在或已删除", color = MaterialTheme.colorScheme.onBackground)
                }

                else -> {
                    val a = anime!!

                    ElevatedCard {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1) 标题
                            if (!isEditing) {
                                Text(a.title, style = MaterialTheme.typography.titleLarge)
                            } else {
                                OutlinedTextField(
                                    value = editTitle,
                                    onValueChange = { editTitle = it },
                                    label = { Text("标题") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isSaving
                                )
                            }

                            Divider()

                            // 2) 标签
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("标签", style = MaterialTheme.typography.titleSmall)
                                if (isEditing) {
                                    TextButton(
                                        enabled = !isSaving,
                                        onClick = {
                                            newTagInput = ""
                                            showTagDialog = true
                                        }
                                    ) { Text("添加标签") }
                                }
                            }

                            if (editTags.isEmpty()) {
                                Text(
                                    "暂无标签",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    editTags.forEach { tag ->
                                        FilterChip(
                                            selected = true,
                                            onClick = {
                                                if (isEditing && !isSaving) {
                                                    editTags = editTags - tag
                                                }
                                            },
                                            label = { Text(tag) }
                                        )
                                    }
                                }
                                if (isEditing) {
                                    Text(
                                        "提示：编辑模式下点击标签可移除",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Divider()

                            // 3) 简介
                            Text("简介", style = MaterialTheme.typography.titleSmall)
                            if (!isEditing) {
                                Text(a.description, style = MaterialTheme.typography.bodyMedium)
                            } else {
                                OutlinedTextField(
                                    value = editDesc,
                                    onValueChange = { editDesc = it },
                                    label = { Text("简介") },
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isSaving
                                )
                            }

                            Divider()

                            // 4) 添加时间
                            Text("添加时间", style = MaterialTheme.typography.titleSmall)
                            Text(
                                formatDate(a.createdAt),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Divider()

                            // 5) 系列
                            Text("系列", style = MaterialTheme.typography.titleSmall)

                            val hasSeries = !a.seriesId.isNullOrBlank()
                            if (hasSeries) {
                                Text(seriesName ?: "未命名系列")

                                if (isEditing) {
                                    TextButton(
                                        enabled = !isSaving,
                                        onClick = { openSeriesPicker() }
                                    ) { Text("修改系列") }
                                } else {
                                    Text(
                                        "提示：如需修改系列，请进入编辑模式",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(
                                    "当前未加入系列",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    enabled = !isSaving,
                                    onClick = { openSeriesPicker() }
                                ) { Text("添加到系列") }
                            }

                            // 编辑模式操作区：删除 + 保存
                            if (isEditing) {
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        enabled = !isSaving,
                                        onClick = { showDeleteDialog = true }
                                    ) { Text("删除", color = MaterialTheme.colorScheme.error) }

                                    TextButton(
                                        enabled = !isSaving && editTitle.trim().isNotEmpty(),
                                        onClick = {
                                            val newTitle = editTitle.trim()
                                            if (newTitle.isEmpty()) {
                                                toast("标题不能为空")
                                                return@TextButton
                                            }
                                            isSaving = true
                                            scope.launch {
                                                try {
                                                    withContext(Dispatchers.IO) {
                                                        animeRepo.updateAnime(
                                                            a.copy(
                                                                title = newTitle,
                                                                description = editDesc.trim()
                                                                    .ifBlank { "目前没有简介哦" },
                                                                tags = editTags.toList()
                                                            )
                                                        )
                                                    }
                                                    toast("已保存")
                                                    isEditing = false
                                                    reload()
                                                } catch (e: Exception) {
                                                    toast("保存失败：${e.message ?: "未知错误"}")
                                                } finally {
                                                    isSaving = false
                                                }
                                            }
                                        }
                                    ) { Text(if (isSaving) "保存中…" else "保存") }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    // ---------- 删除确认 ----------
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text("删除番剧") },
            text = { Text("确定要删除该番剧吗？\n删除后将不会在列表中显示。") },
            confirmButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = {
                        val a = anime ?: return@TextButton
                        isDeleting = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    animeRepo.softDeleteAnime(a.id)
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
                    Text(
                        if (isDeleting) "删除中…" else "确认删除",
                        color = MaterialTheme.colorScheme.error
                    )
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

    // ---------- 选择/创建系列弹窗 ----------
    if (showSeriesPicker) {
        AlertDialog(
            onDismissRequest = { showSeriesPicker = false },
            title = { Text("选择系列") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!isSeriesLoading) {
                        // ✅ 仅编辑模式下允许“修改为无系列”
                        val canRemoveSeries = isEditing && !(anime?.seriesId.isNullOrBlank())

                        if (canRemoveSeries) {
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    val a = anime ?: return@TextButton
                                    isSaving = true
                                    showSeriesPicker = false
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                animeRepo.updateAnime(a.copy(seriesId = null))
                                            }
                                            toast("已移除系列")
                                            reload()
                                        } catch (e: Exception) {
                                            toast("操作失败：${e.message ?: "未知错误"}")
                                        } finally {
                                            isSaving = false
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    "移除系列（设为无系列）",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Divider()
                        }
                    }
                    if (isSeriesLoading) {
                        Text("加载系列中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        if (seriesList.isEmpty()) {
                            Text(
                                "暂无系列，你可以创建一个新的。",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text("已有系列", style = MaterialTheme.typography.titleSmall)
                            seriesList.forEach { s ->
                                TextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        val a = anime ?: return@TextButton
                                        isSaving = true
                                        showSeriesPicker = false
                                        scope.launch {
                                            try {
                                                withContext(Dispatchers.IO) {
                                                    animeRepo.updateAnime(a.copy(seriesId = s.id))
                                                }
                                                toast("已加入系列：${s.name}")
                                                createSeriesInput = ""
                                                reload()
                                            } catch (e: Exception) {
                                                toast("操作失败：${e.message ?: "未知错误"}")
                                            } finally {
                                                isSaving = false
                                            }
                                        }
                                    }
                                ) { Text(s.name) }
                            }
                        }

                        Divider()

                        Text("创建新系列", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = createSeriesInput,
                            onValueChange = { createSeriesInput = it },
                            singleLine = true,
                            label = { Text("新系列名称") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSeriesLoading && createSeriesInput.trim().isNotEmpty(),
                    onClick = {
                        val name = createSeriesInput.trim()
                        val a = anime ?: return@TextButton
                        isSaving = true
                        showSeriesPicker = false
                        scope.launch {
                            try {
                                val created = withContext(Dispatchers.IO) {
                                    val exists = seriesRepo.existsExactName(name)
                                    if (exists) null else seriesRepo.createSeries(name)
                                }
                                if (created == null) {
                                    toast("已存在同名系列，请换个名字")
                                    isSaving = false
                                    return@launch
                                }

                                withContext(Dispatchers.IO) {
                                    animeRepo.updateAnime(a.copy(seriesId = created.id))
                                }
                                toast("已创建并加入系列：${created.name}")
                                createSeriesInput = ""
                                reload()
                            } catch (e: Exception) {
                                toast("创建失败：${e.message ?: "未知错误"}")
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                ) { Text("创建并加入") }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = { showSeriesPicker = false }
                ) { Text("取消") }
            }
        )
    }

    // ---------- 添加标签弹窗 ----------
    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("添加标签") },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    singleLine = true,
                    label = { Text("标签名称") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val t = newTagInput.trim()
                    if (t.isNotEmpty()) editTags = editTags + t
                    showTagDialog = false
                }) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun StatusMenuItem(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        enabled = enabled,
        onClick = onClick
    )
}

private fun String.toZhStatus(): String = when (this) {
    "plan" -> "想看"
    "watching" -> "在看"
    "completed" -> "看完"
    "dropped" -> "弃番"
    else -> this
}

private fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}
