package com.example.otakumaster.ui.screens.home.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.otakumaster.OtakuMasterApp
import com.example.otakumaster.R
import com.example.otakumaster.data.db.entities.AnimeEntity
import com.example.otakumaster.data.query.AnimeQueryParams
import com.example.otakumaster.data.query.AnimeScope
import com.example.otakumaster.data.query.AnimeStatus
import com.example.otakumaster.ui.screens.home.model.AnimeStatusTab
import com.example.otakumaster.ui.theme.OtakuPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeFilterBar(
    selectedTab: AnimeStatusTab,
    onTabChange: (AnimeStatusTab) -> Unit,

    folded: Boolean,
    onFoldChange: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OtakuMasterApp
    val animeRepo = app.animeRepository
    val seriesRepo = app.animeSeriesRepository
    val scope = rememberCoroutineScope()

    var showRandomCard by remember { mutableStateOf(false) }
    var pickedAnime by remember { mutableStateOf<AnimeEntity?>(null) }
    var pickedSeriesName by remember { mutableStateOf<String?>(null) }
    var isPicking by remember { mutableStateOf(false) }
    var lastPickedId by remember { mutableStateOf<String?>(null) }

    fun pickRandomAnime() {
        if (isPicking) return
        isPicking = true
        scope.launch {
            try {
                val planList = withContext(Dispatchers.IO) {
                    animeRepo.list(
                        AnimeQueryParams(
                            scope = AnimeScope.BY_STATUS,
                            status = AnimeStatus.PLAN
                        )
                    )
                }

                if (planList.isEmpty()) {
                    Toast.makeText(context, "想看列表为空，请先添加番剧", Toast.LENGTH_SHORT).show()
                    showRandomCard = false
                } else {
                    // 如果列表大于1，排除上一次抽到的，避免连续重复
                    val candidates = if (planList.size > 1 && lastPickedId != null) {
                        planList.filter { it.id != lastPickedId }
                    } else {
                        planList
                    }
                    val randomItem = candidates.random()
                    pickedAnime = randomItem
                    lastPickedId = randomItem.id

                    // 查询系列名称
                    if (!randomItem.seriesId.isNullOrBlank()) {
                        pickedSeriesName = withContext(Dispatchers.IO) {
                            seriesRepo.getActiveById(randomItem.seriesId)?.name
                        }
                    } else {
                        pickedSeriesName = null
                    }
                    showRandomCard = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "抽取失败", Toast.LENGTH_SHORT).show()
            } finally {
                isPicking = false
            }
        }
    }
    Row(
        modifier = Modifier
            .height(45.dp)
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(3f)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimeStatusTab.entries.forEachIndexed { index, tab ->//遍历所有按钮的文字并获取下标
                val isSelected = tab == selectedTab//如果tab被选中则isSelected为真
                Text(
                    text = tab.label,
                    color = OtakuPrimary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable { onTabChange(tab) })
                //如果当前不是最后一个，则在text右边加空间
                if (index != AnimeStatusTab.entries.lastIndex) {
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = null,
                tint = OtakuPrimary,
                modifier = Modifier
                    .width(20.dp)
                    .height(20.dp)
                    .clickable(onClick = {
                        pickRandomAnime()
                })
            )
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_fold),
                contentDescription = "折叠",
                tint = if (folded) OtakuPrimary else Color(0xFF888888),
                modifier = Modifier
                    .width(20.dp)
                    .height(20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onFoldChange() })
        }
    }
    if (showRandomCard && pickedAnime != null) {
        AlertDialog(
            onDismissRequest = { showRandomCard = false },
            title = {
                Text(text = "随机番剧", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = pickedAnime!!.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (pickedSeriesName != null) {
                        Text(
                            text = "系列: $pickedSeriesName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = "状态: 想看",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { pickRandomAnime() }) {
                    Text("换一个")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRandomCard = false }) {
                    Text("取消")
                }
            }
        )
    }
}
