package com.example.otakumaster.ui.screens.add

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.otakumaster.OtakuMasterApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSeriesScreen(
    navController: NavHostController
) {
    // -----------------------------
    // 0) 拿到 Repository（通过 Application 单例）
    // -----------------------------
    val app = LocalContext.current.applicationContext as OtakuMasterApp
    val repo = app.animeSeriesRepository

    // -----------------------------
    // 1) UI 状态
    // -----------------------------
    var name by remember { mutableStateOf("") }

    // 表单校验与保存状态
    var nameError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val canSave = name.isNotBlank() && !isSaving

    // Snackbar（用于提示错误/提醒：仿 AddAnime）
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
                title = { Text("添加系列") },
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
                            val n = name.trim()
                            if (n.isEmpty()) {
                                nameError = "系列名不能为空"
                                scope.launch { snackbarHostState.showSnackbar("请先填写系列名") }
                                return@TextButton
                            }

                            // 清掉旧错误
                            nameError = null

                            // 开始保存：禁用按钮
                            isSaving = true

                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        val exists = repo.existsExactName(n)
                                        if (exists) return@withContext "DUPLICATE"

                                        repo.createSeries(name = n)
                                        "OK"
                                    }

                                    when (result) {
                                        "DUPLICATE" -> {
                                            nameError = "系列名已存在（完全重复）"
                                            Toast.makeText(
                                                context,
                                                "已存在同名系列，请修改系列名",
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
                windowInsets = WindowInsets(0) // 贴顶：避免 TopAppBar 偏下
            )
        }
    ) { innerPadding ->

        // -----------------------------
        // 3) 表单内容（保持美观：卡片 + 间距）
        // -----------------------------
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ===== 卡片 1：系列信息 =====
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("系列信息", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            // 用户一改就清错误（体验一致）
                            if (nameError != null) nameError = null
                        },
                        singleLine = true,
                        label = { Text("系列名（必填）") },
                        placeholder = { Text("例如：Fate / 进击的巨人 / CLANNAD") },
                        isError = nameError != null,
                        supportingText = {
                            if (nameError != null) {
                                Text(nameError!!, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("系列名将用于重复检查（完全一致才算重复）")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    )

                    Text(
                        text = "提示：后续在添加番剧时可选择加入该系列，用于首页折叠展示。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
