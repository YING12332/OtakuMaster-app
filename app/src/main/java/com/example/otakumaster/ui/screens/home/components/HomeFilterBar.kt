package com.example.otakumaster.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.otakumaster.R
import com.example.otakumaster.ui.screens.home.model.AnimeStatusTab
import com.example.otakumaster.ui.theme.OtakuPrimary

@Composable
fun HomeFilterBar(
    selectedTab: AnimeStatusTab,
    onTabChange: (AnimeStatusTab) -> Unit,

    folded: Boolean,
    onFoldChange: () -> Unit,

//    sortLabel: String,
//    sortOptions: List<String>,
//    onSortSelected: (String) -> Unit,

    modifier: Modifier = Modifier
) {
//    var sortMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .height(45.dp)
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
//                .background(Color(0xFF999999))
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
            /*Spacer(modifier = Modifier.width(16.dp))
            Box {
                Icon(
                    painter = painterResource(id = R.drawable.ic_sort),
                    contentDescription = "排序",
                    tint = if (sortMenuExpanded) OtakuPrimary else Color(0xFF888888),
                    modifier = Modifier
                        .width(24.dp)
                        .height(24.dp)
                        .clickable { sortMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    // 菜单第一行：显示当前排序（可选）
                    DropdownMenuItem(
                        text = { Text(text = "当前：$sortLabel") },
                        onClick = { /* 不切换，仅提示 */ }
                    )

                    // 分隔线（可选：如果你不想要，可以删掉）
                    // DropdownMenuItem(text = { Text("—") }, onClick = {})

                    sortOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                sortMenuExpanded = false
                                onSortSelected(option)
                            }
                        )
                    }
                }
            }*/

        }
    }
}