package com.example.otakumaster.ui.screens.middle.components

import android.annotation.SuppressLint
import android.icu.text.ListFormatter.Width
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.otakumaster.OtakuMasterApp
import com.example.otakumaster.data.db.entities.AnimeEntity
import com.example.otakumaster.ui.theme.OtakuPrimary
import com.example.otakumaster.utils.lightAndDarkColor
import com.example.otakumaster.utils.nowTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun EpisodeList(

) {
    val context = LocalContext.current
    val app = context.applicationContext as OtakuMasterApp
    val animeRepo = app.animeRepository
    val scope = rememberCoroutineScope()

    var animeWatchingList by remember { mutableStateOf<List<AnimeEntity>>(emptyList()) }

    fun reloadWatchingAnime() {
        scope.launch {
            try {
                val list = withContext(Dispatchers.IO) { animeRepo.getWatchingAnime() }
                animeWatchingList = list
            } catch (e: Exception) {
                Toast.makeText(context, "获取计划失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    LaunchedEffect(Unit) { reloadWatchingAnime() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp, end = 20.dp)
    ) {
        items(animeWatchingList) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp, bottom = 5.dp),
                colors = CardDefaults.cardColors(
                    containerColor = lightAndDarkColor(
                        lightColor = Color(
                            0xFFFFFFFF
                        ), darkColor = Color(0xFF222222)
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 5.dp, end = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 5.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        TextButton(onClick = {
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO){
                                        animeRepo.changeStatus(
                                            item.id,
                                            "completed",
                                            nowTime()
                                        )
                                    }
                                    Toast.makeText(context,"移动成功",Toast.LENGTH_SHORT).show()
                                    reloadWatchingAnime()
                                }catch (e:Exception){
                                    Toast.makeText(context,"移动失败",Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Text(text = "移动到看完")
                        }
                    }
                    BoxWithConstraints (modifier = Modifier.fillMaxWidth()){
                        val rowWidth=maxWidth
                        val spacing=4.dp
                        val textWidthPlaceholder=40.dp
                        val totalSpacing=spacing*7

                        val buttonWidth=(rowWidth-totalSpacing-textWidthPlaceholder)/6

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NumberButton(item.id, false, 25, item.episode,buttonWidth) { reloadWatchingAnime() }
                            NumberButton(item.id, false, 5, item.episode,buttonWidth) { reloadWatchingAnime() }
                            NumberButton(item.id, false, 1, item.episode,buttonWidth) { reloadWatchingAnime() }
                            Text(
                                text = item.episode.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            NumberButton(item.id, true, 1, item.episode,buttonWidth) { reloadWatchingAnime() }
                            NumberButton(item.id, true, 5, item.episode,buttonWidth) { reloadWatchingAnime() }
                            NumberButton(item.id, true, 25, item.episode,buttonWidth) { reloadWatchingAnime() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NumberButton(
    animeId: String,
    isAdd: Boolean,
    number: Int,
    nowEpisode: Int?,
    buttonWidth: Dp,
    onUpdateSuccess: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OtakuMasterApp
    val animeRepo = app.animeRepository
    val scope = rememberCoroutineScope()
    fun changeEpisode(animeId: String, toEpisode: Int) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    animeRepo.changeEpisode(animeId, toEpisode)
                }
                onUpdateSuccess()
            } catch (e: Exception) {
                Toast.makeText(context, "修改失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(if(buttonWidth>50.dp) 50.dp else buttonWidth)
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(OtakuPrimary.copy(alpha = 0.1f))
            .clickable {
                var episode: Int? = nowEpisode
                if (episode != null) {
                    if (isAdd) {
                        episode += number
                    } else {
                        if (episode - number < 0) {
                            Toast
                                .makeText(context, "无法小于0", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            episode -= number
                        }
                    }
                }
                episode?.let { changeEpisode(animeId, it) }
            }
    ) {
        Text(text = if (isAdd) "+$number" else "-$number", fontSize = 14.sp)
    }
}