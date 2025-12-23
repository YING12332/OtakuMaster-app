package com.example.otakumaster.ui.screens.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.otakumaster.R
import com.example.otakumaster.ui.theme.OtakuPrimary

@Composable
fun HomeSearchRow(
    query: String,
    onQueryChange: (String) -> Unit,

    onAddAnimeClick: () -> Unit,
    onAddSeriesClick: () -> Unit,

    modifier: Modifier = Modifier
) {
    var rotated by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (rotated) 135f else 0f)
    Row(
        modifier = modifier//使用HomeScreen传回的modifier
            .fillMaxWidth()
            .height(55.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeight = 40.sp
            ),
            modifier = Modifier.fillMaxHeight(),
            cursorBrush = SolidColor(OtakuPrimary),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                        .width(330.dp)
                        .border(
                            width = 1.dp,
                            color = Color(0xFF989898),
                            shape = RoundedCornerShape(32.dp)
                        )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = "搜索",
                        modifier = Modifier
                            .height(32.dp)
                            .width(32.dp)
                            .padding(start = 10.dp),
                        tint = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "搜索番剧名称…",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeight = 40.sp
                                ),
                                color = Color(0xFF888888)
                            )
                        }
                        innerTextField()
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier
                                .width(32.dp)
                                .height(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_text_clean),
                                contentDescription = "清空",
                                modifier = Modifier
                                    .height(14.dp)
                                    .width(14.dp),
                                tint = Color(0xFF666666)
                            )
                        }
                    }
                }
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        // 控制菜单展开/关闭
        var addMenuExpanded by remember { mutableStateOf(false) }

        Box {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = "添加",
                tint = Color(0xFF666666),
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // 点击 + ：旋转 + 打开菜单
                        rotated = !rotated
                        addMenuExpanded = true
                    }
            )

            DropdownMenu(
                expanded = addMenuExpanded,
                onDismissRequest = {
                    // 点击空白处/返回键：关闭菜单，并把 + 转回去（可选）
                    addMenuExpanded = false
                    rotated = false
                },
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.surface)
                    .width(100.dp)
            ) {
                DropdownMenuItem(
                    text = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "添加番剧",
                                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            )
                        }
                    },
                    onClick = {
                        addMenuExpanded = false
                        rotated = false
                        onAddAnimeClick()
                    },
                    modifier = Modifier.height(44.dp),
                    contentPadding = PaddingValues(horizontal = 1.dp, vertical = 1.dp)
                )
                DropdownMenuItem(
                    text = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "添加系列",
                                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            )
                        }
                    },
                    onClick = {
                        addMenuExpanded = false
                        rotated = false
                        onAddSeriesClick()
                    },
                    contentPadding = PaddingValues(horizontal = 1.dp, vertical = 1.dp)
                )
            }
        }

    }
}

/*OutlinedTextField(
            value = query,//输入框中的值
            onValueChange = onQueryChange,
            placeholder = { Text(text = "搜索番剧名称..") },
            singleLine = true,//只允许单行
            modifier = Modifier.weight(1f),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = "搜索",
                    modifier = Modifier
                        .height(20.dp)
                        .width(20.dp)
                )
            },
            textStyle = MaterialTheme.typography.bodySmall.copy(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )*/
/*IconButton(
            onClick = {
                rotated = !rotated
                onAddClick()
            }, modifier = Modifier
                .height(48.dp)
                .width(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = "添加",
                tint = Color(0xFF666666),
                modifier = Modifier
                    .height(24.dp)
                    .width(24.dp)
                    .rotate(rotation)
            )
        }*/