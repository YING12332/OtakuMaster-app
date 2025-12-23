package com.example.otakumaster.ui.screens.add

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.room.withTransaction
import com.example.otakumaster.OtakuMasterApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddAnimeScreen(
    navController: NavHostController
) {
    // -----------------------------
    // 0) 拿到 Repository（通过 Application 单例）
    // -----------------------------
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as OtakuMasterApp
    val repo = app.animeRepository

    val statusRepo = app.animeStatusEventRepository
    val db = app.database

    // -----------------------------
    // 1) UI 状态
    // -----------------------------
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    val presetTags = remember {
        listOf("治愈", "热血", "日常", "恋爱", "搞笑", "悬疑", "科幻", "冒险", "音乐", "运动")
    }

    // 加入系列：当前版本锁死 false
    var joinSeries by remember { mutableStateOf(false) }

    // 自定义时间
    var useCustomTime by remember { mutableStateOf(false) }
    var customCreatedAtMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    // 自定义标签弹窗
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }

    // 表单校验与保存状态
    var titleError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val canSave = title.isNotBlank() && !isSaving

    // Snackbar（主流 App 常用反馈）
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    // -----------------------------
    // 2) Scaffold
    // -----------------------------
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("添加番剧") },
                navigationIcon = {
                    TextButton(
                        enabled = !isSaving,
                        onClick = { navController.popBackStack() }
                    ) { Text("返回") }
                },
                actions = {
                    TextButton(
                        enabled = canSave,
                        onClick = {
                            // ---- 保存逻辑（完整）----
                            val t = title.trim()
                            if (t.isEmpty()) {
                                titleError = "标题不能为空"
                                scope.launch { snackbarHostState.showSnackbar("请先填写番剧标题") }
                                return@TextButton
                            }

                            // 清掉旧错误
                            titleError = null

                            // 计算 createdAt
                            val now = System.currentTimeMillis()
                            val createdAt = if (useCustomTime) (customCreatedAtMillis ?: now) else now

                            // 开始保存：禁用按钮
                            isSaving = true

                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        db.withTransaction {
                                            val exists = repo.existsExactTitle(t)
                                            if (exists) return@withTransaction "DUPLICATE"

                                            // 1) 插入番剧（拿到 id）
                                            val anime = repo.createAnime(
                                                title = t,
                                                description = description.trim().ifBlank { "目前没有简介哦" },
                                                currentStatus = "plan",             // 想看
                                                tags = selectedTags.toList(),
                                                seriesId = null,
                                                now = createdAt                      // createdAt
                                            )

                                            // 2) 插入状态事件：想看 + 时间=createdAt
                                            statusRepo.addEvent(
                                                animeId = anime.id,
                                                status = "plan",
                                                changedAt = createdAt
                                            )

                                            "OK"
                                        }
                                    }


                                    when (result) {
                                        "DUPLICATE" -> {
                                            titleError = "标题已存在（完全重复）"
                                            Toast.makeText(
                                                context,
                                                "已存在同名番剧，请修改标题",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            isSaving = false
                                        }
                                        "OK" -> {
                                            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "保存失败：${e.message ?: "未知错误"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    isSaving = false
                                }
                            }

                        }
                    ) { Text(if (isSaving) "保存中…" else "保存") }
                },
                windowInsets = WindowInsets(0)
            )
        }
    ) { innerPadding ->
        // -----------------------------
        // 3) 表单内容（保持美观）
        // -----------------------------
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ===== 卡片 1：基本信息（标题 + 标签 + 简介）=====
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("基本信息", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            // 用户一改标题，就先清掉错误提示（体验更像主流 App）
                            if (titleError != null) titleError = null
                        },
                        singleLine = true,
                        label = { Text("番剧标题（必填）") },
                        placeholder = { Text("例如：葬送的芙莉莲") },
                        isError = titleError != null,
                        supportingText = {
                            if (titleError != null) {
                                Text(titleError!!, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("标题将用于重复检查（完全一致才算重复）")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    )

                    // —— 标签：放在简介上面（按你要求）——
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("标签", style = MaterialTheme.typography.titleSmall)
                        TextButton(
                            enabled = !isSaving,
                            onClick = {
                                newTagInput = ""
                                showAddTagDialog = true
                            }
                        ) { Text("添加自定义") }
                    }

                    Text(
                        "预设标签",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetTags.forEach { tag ->
                            FilterChip(
                                selected = selectedTags.contains(tag),
                                onClick = {
                                    if (!isSaving) selectedTags = toggleSet(selectedTags, tag)
                                },
                                label = { Text(tag) }
                            )
                        }
                    }

                    if (selectedTags.isNotEmpty()) {
                        Text(
                            "已选择",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedTags.forEach { tag ->
                                AssistChip(
                                    onClick = { if (!isSaving) selectedTags = toggleSet(selectedTags, tag) },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    } else {
                        Text(
                            "暂无标签（可从预设选择或添加自定义）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("番剧简介") },
                        placeholder = { Text("写一点点简介，或者留空也可以") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    )
                }
            }

            // ===== 卡片 2：高级选项 =====
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("高级选项", style = MaterialTheme.typography.titleMedium)

                    // 加入系列（先锁死）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("加入系列", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "当前版本暂不支持选择系列（后续再做）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = joinSeries,
                            onCheckedChange = { joinSeries = false },
                            enabled = false
                        )
                    }

                    Divider()

                    // 自定义时间
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("选择创建时间", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (useCustomTime) {
                                    "当前：${formatTime(customCreatedAtMillis ?: System.currentTimeMillis())}"
                                } else {
                                    "默认使用当前时间"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useCustomTime,
                            onCheckedChange = { checked ->
                                if (isSaving) return@Switch
                                useCustomTime = checked
                                if (checked) showDatePicker = true else customCreatedAtMillis = null
                            }
                        )
                    }

                    if (useCustomTime) {
                        TextButton(
                            enabled = !isSaving,
                            onClick = { showDatePicker = true },
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("修改时间") }
                    }

                    // 提示：默认状态固定想看
                    Text(
                        text = "提示：新建番剧将默认加入「想看」",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // -----------------------------
    // 自定义标签弹窗
    // -----------------------------
    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text("添加自定义标签") },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    singleLine = true,
                    label = { Text("标签名称") },
                    placeholder = { Text("例如：神作 / 童年 / 泪目") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        val t = newTagInput.trim()
                        if (t.isNotEmpty()) selectedTags = selectedTags + t
                        showAddTagDialog = false
                    }
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = { showAddTagDialog = false }
                ) { Text("取消") }
            }
        )
    }

    // -----------------------------
    // 日期选择弹窗（浮层，不影响布局）
    // -----------------------------
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = customCreatedAtMillis ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selected ->
                        customCreatedAtMillis = selected
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun toggleSet(set: Set<String>, value: String): Set<String> =
    if (set.contains(value)) set - value else set + value

private fun formatTime(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}
