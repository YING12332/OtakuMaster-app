package com.example.otakumaster.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.otakumaster.data.db.entities.AnimeSeriesEntity
import com.example.otakumaster.data.repository.AnimeSeriesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SeriesQueryCard(
    visible: Boolean,
    repo: AnimeSeriesRepository,
    nowSeriesId: String?,
    onDismiss: () -> Unit,
    onConfirm: (AnimeSeriesEntity?) -> Unit
) {
    if (!visible) return

    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<AnimeSeriesEntity?>(null) }
    var list by remember { mutableStateOf<List<AnimeSeriesEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    suspend fun refreshList(query: String) {
        isLoading = true
        try {
            list = withContext(Dispatchers.IO) {
                repo.searchSeriesListByName(query)
            }
            if (nowSeriesId != null) {
                selected = repo.getActiveById(nowSeriesId)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "系列加载失败", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(query) {
        refreshList(query)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(

        ) {
            Column(
                modifier = Modifier.padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 20.dp,
                    bottom = 20.dp
                )
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("搜索系列名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Divider()
                Spacer(modifier = Modifier.height(6.dp))

                if (isLoading) {
                    Text("系列加载中...")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        if (list.isEmpty() && query.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("没有这个系列哦~")
                                    TextButton(onClick = {
                                        scope.launch {
                                            try {
                                                if (!repo.existsExactName(query)) {
                                                    repo.createSeries(query)
                                                    Toast.makeText(
                                                        context,
                                                        "创建系列成功",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "系列已存在",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                query = ""
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "创建系列失败",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }) {
                                        Text("点击添加\"$query\"到系列")
                                    }
                                }
                            }
                        } else if (list.isEmpty()) {
                            item { Text("当前无系列") }
                        } else {
                            items(list, key = { it.id }) { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = item.id == selected?.id,
                                        onClick = { selected = item },
                                        modifier = Modifier.size(26.dp).padding(end = 10.dp)
                                    )
                                    Text(item.name, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Divider()
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box() {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected?.id == null,
                                onClick = { selected = null },
                                modifier = Modifier.size(16.dp).padding(end = 10.dp)
                            )
                            Text("无系列", fontSize = 14.sp)
                        }
                    }
                    Box() {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismiss) { Text("取消") }
                            TextButton(onClick = { onConfirm(selected) }) { Text("保存") }
                        }
                    }
                }
            }
        }
    }
}