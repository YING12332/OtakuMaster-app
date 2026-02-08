package com.example.otakumaster.ui.screens.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.otakumaster.MainActivity
import com.example.otakumaster.data.backup.BackupRepository
import com.example.otakumaster.data.db.OtakuDatabase
import com.example.otakumaster.utils.TimeUtils
import kotlinx.coroutines.launch

private const val TAG = "ProfileScreen"

private fun softRestartToMain(context: Context) {
    val appContext = context.applicationContext

    OtakuDatabase.closeInstance()

    val intent = Intent(appContext, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    appContext.startActivity(intent)

    (context as? Activity)?.finishAffinity()
}

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var importPreview by remember { mutableStateOf<BackupRepository.ImportPreview?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    if (showImportDialog && importPreview != null) {
        val p = importPreview!!

        AlertDialog(
            onDismissRequest = { if (!isImporting) showImportDialog = false },
            title = { Text("最近存档：\n ${TimeUtils.formatDate(p.createdAt.timestamp)}") },
            text = {
                Column {
                    Text("备份文件：${p.backupFile.name}")
                    Text("\n文件对比（存档 / 本地）：")
                    Text("番剧：${p.backupStatistics.animeCount} / ${p.localStatistics.animeCount}")
                    Text("系列：${p.backupStatistics.seriesCount} / ${p.localStatistics.seriesCount}")
                    Text("事件：${p.backupStatistics.eventCount} / ${p.localStatistics.eventCount}")
                    Text("文本：${p.backupStatistics.textEntryCount} / ${p.localStatistics.textEntryCount}")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isImporting,
                    onClick = {
                        scope.launch {
                            isImporting = true
                            try {
                                val result = BackupRepository.importLatestBackup(context)
                                Toast.makeText(
                                    context,
                                    "导入完成，正在返回首页…",
                                    Toast.LENGTH_LONG
                                ).show()
                                showImportDialog = false
                                softRestartToMain(context)
                            } catch (e: com.example.otakumaster.data.backup.BackupException) {
                                Toast.makeText(context, e.userMessage, Toast.LENGTH_LONG).show()
                            } catch (e: Throwable) {
                                Log.e(TAG, "导入发生程序错误", e)
                                Toast.makeText(
                                    context,
                                    "导入错误",
                                    Toast.LENGTH_LONG
                                ).show()
                            } finally {
                                isImporting = false
                            }
                        }
                    }
                ) {
                    Text(if (isImporting) "正在导入…" else "确认导入")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isImporting,
                    onClick = { showImportDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Profile - 用户中心（占位）",
                color = MaterialTheme.colorScheme.onBackground
            )

            Button(
                modifier = Modifier.padding(top = 16.dp),
                onClick = {
                    scope.launch {
                        try {
                            val out = BackupRepository.exportBackup(context)
                            Toast.makeText(context, "存档已保存", Toast.LENGTH_LONG).show()
                        } catch (e: com.example.otakumaster.data.backup.BackupException) {
                            Toast.makeText(context, e.userMessage, Toast.LENGTH_LONG).show()
                        } catch (e: Throwable) {
                            Log.e(TAG, "导出发生程序错误", e)
                            Toast.makeText(
                                context,
                                "导出错误",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            ) {
                Text("导出存档")
            }

            Button(
                modifier = Modifier.padding(top = 12.dp),
                onClick = {
                    scope.launch {
                        try {
                            importPreview = BackupRepository.prepareLatestImportPreview(context)
                            showImportDialog = true
                        } catch (e: com.example.otakumaster.data.backup.BackupException) {
                            Toast.makeText(context, e.userMessage, Toast.LENGTH_LONG).show()
                        } catch (e: Throwable) {
                            Log.e(TAG, "读取存档发生程序错误", e)
                            Toast.makeText(
                                context,
                                "读取存档错误",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            ) {
                Text("导入最近存档（测试）")
            }
        }
    }
}
