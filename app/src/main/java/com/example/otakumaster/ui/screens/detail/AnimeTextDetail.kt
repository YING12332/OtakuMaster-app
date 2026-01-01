package com.example.otakumaster.ui.screens.detail

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.otakumaster.OtakuMasterApp
import com.example.otakumaster.data.db.entities.AnimeTextEntryEntity
import com.example.otakumaster.utils.CopyToClipboard.copyTextToClipboard
import com.example.otakumaster.utils.TimeUtils.formatDate
import com.example.otakumaster.utils.lightAndDarkColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AnimeTextDetail(
    animeId: String
) {
    val context = LocalContext.current
    val app = context.applicationContext as OtakuMasterApp
    val animeRepo = app.animeRepository
    val textRepo = app.animeTextEntryRepository
    val scope = rememberCoroutineScope()

    var animeTitle by remember { mutableStateOf("") }
    var showAddText by remember { mutableStateOf(false) }
    var newTextInput by remember { mutableStateOf("") }

    var showMenuId by remember { mutableStateOf<String?>(null) }
    var showDelete by remember { mutableStateOf(false) }
    var deleteTextId by remember { mutableStateOf<String?>(null) }
    var showEdit by remember { mutableStateOf(false) }
    var editTextId by remember { mutableStateOf<String?>(null) }
    var editTextContent by remember { mutableStateOf("") }

    var textList by remember { mutableStateOf<List<AnimeTextEntryEntity>>(emptyList()) }

    fun reloadText(animeId: String) {
        scope.launch {
            try {
                val (list, title) = withContext(Dispatchers.IO) {
                    val list = textRepo.listByAnimeTimeDesc(animeId)
                    val title = animeRepo.getById(animeId)?.title ?: ""
                    list to title
                }
                textList = list
                animeTitle = title
            } catch (e: Exception) {
                Toast.makeText(context, "痕迹获取失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    LaunchedEffect(animeId) { reloadText(animeId) }
    TextButton(onClick = { showAddText = true }) {
        Text(text = "添加痕迹")
    }
    textList.forEach { item ->
        Box{
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = { copyTextToClipboard(context,if (animeTitle.isNotBlank()) "${item.content}   ——$animeTitle" else item.content) }, onLongClick = { showMenuId = item.id }),
                colors = CardDefaults.cardColors(containerColor = lightAndDarkColor(lightColor = Color(0xFFFFFFFF), darkColor = Color(0xFF222222)))
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
                        if (animeTitle.isNotEmpty()) {
                            Text(text = "——$animeTitle", fontSize = 18.sp)
                        }
                    }
                    Text(text = if (item.isEdited==1)"编辑于"+formatDate(item.timeAt) else formatDate(item.timeAt), fontSize = 12.sp, color = Color(0xFF999999))
                }
            }

            DropdownMenu(
                expanded = showMenuId == item.id,
                onDismissRequest = { showMenuId = null },
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.surface)
                    .width(70.dp)
            ) {
                DropdownMenuItem(
                    text = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "编辑",
                                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            )
                        }
                    },
                    onClick = {
                        showMenuId = null
                        showEdit = true
                        editTextId = item.id
                        editTextContent = item.content
                    },
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(horizontal = 1.dp, vertical = 1.dp)
                )
                DropdownMenuItem(
                    text = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "删除",
                                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            )
                        }
                    },
                    onClick = {
                        showMenuId = null
                        showDelete = true
                        deleteTextId = item.id
                    },
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(horizontal = 1.dp, vertical = 1.dp)
                )
            }
        }
    }

    if (showAddText) {
        AlertDialog(
            onDismissRequest = { showAddText = false },
            title = { Text(text = "添加痕迹") },
            text = {
                OutlinedTextField(
                    value = newTextInput,
                    onValueChange = { newTextInput = it },
                    label = { Text("添加痕迹") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val t = newTextInput.trim()
                    if (t.isNotEmpty()) {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO){
                                    textRepo.addText(animeId, t)
                                }
                                Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show()
                                reloadText(animeId)
                                newTextInput = ""
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "保存失败：${e.message ?: "未知错误"}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    showAddText = false
                }) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddText = false }) { Text("取消") }
            }
        )
    }
    if (showDelete) {
        AlertDialog(
            modifier = Modifier.width(200.dp),
            onDismissRequest = { showDelete = false },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "是否删除？",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO){
                                deleteTextId?.let { textRepo.softDeleteText(it) }
                            }
                            Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
                            reloadText(animeId)
                            deleteTextId = null
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "删除失败：${e.message ?: "未知错误"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    showDelete = false
                }) {
                    Text(text = "确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("取消") }
            }
        )
    }
    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text(text = "编辑痕迹") },
            text = {
                OutlinedTextField(
                    value = editTextContent,
                    onValueChange = { editTextContent = it },
                    label = { Text("编辑痕迹") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val t = editTextContent.trim()
                    if (t.isNotEmpty()) {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO){
                                    editTextId?.let { textRepo.editText(it, t) }
                                }
                                Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                                reloadText(animeId)
                                editTextContent = ""
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "保存失败：${e.message ?: "未知错误"}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    showEdit = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showEdit = false }) { Text("取消") }
            }
        )
    }
}