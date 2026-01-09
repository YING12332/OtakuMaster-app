package com.example.otakumaster.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.otakumaster.R
import com.example.otakumaster.ui.navigation.AppRoute
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 设计稿基准：宽 430，高 70
 * 横向尺寸：按 maxWidth * (设计值/430)
 * 纵向尺寸：按 barHeight * (设计值/70)
 *
 * 注意：设计稿中的 item 宽 24 / 高 38 在真实设备上会裁切文字，
 * 所以这里对 itemW 做最小兜底，itemH 用整个 barHeight 让内部按 top/bottom 定位。
 */
@Composable
fun SlidingBottomBar(
    navController: NavController,
    modifier: Modifier = Modifier,
    themeColor: Color = Color(0xFF39C5BB),
) {
    val routes = remember { listOf(AppRoute.Home, AppRoute.NewFeature, AppRoute.Profile) }

    // 当前路由 -> 选中 index（支持外部导航变化时同步）
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedIndex = remember(currentRoute) {
        routes.indexOfFirst { it.route == currentRoute }.let { if (it == -1) 0 else it }
    }

    // 固定高度（你设计稿高 70）
    val barHeight = 55.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
    ) {
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current

        val maxW = maxWidth

        fun w(design: Float): Dp = maxW * (design / 430f)
        fun h(design: Float): Dp = barHeight * (design / 70f)

        // 滑块参数（按比例）
        val sliderW = w(110f)
        val sliderH = h(60f)
        val sliderTop = h(5f)          // 距顶部 5
        val sliderRadius = 15.dp       // 你确认的圆角

        // icon / text 的垂直约束（按比例）
        val iconTop = h(18f) - 5.dp         // icon 距顶部 18
        val textBottom = h(16f) - 5.dp        // text 距底部 16

        // item 尺寸：设计稿给 24x38，但真实中文+12sp会裁切，所以给最小兜底
        val itemW = maxOf(w(24f), 44.dp)
        val itemH = barHeight
        val iconSize = h(28f)  // barHeight 固定 70dp，这样平板也不会变大


        // 默认左右距离边缘 50
        val sideMarginDefault = w(50f)

        // 三档停靠位置（滑块距离左右 20；中间居中）
        val leftStop = w(20f)
        val centerStop = (maxW - sliderW) / 2
        val rightStop = maxW - sliderW - w(20f)

        // slider offset 用 px 更稳（拖动）
        val sliderX = remember {
            val initial = when (selectedIndex) {
                0 -> leftStop
                1 -> centerStop
                else -> rightStop
            }
            Animatable(with(density) { initial.toPx() })
        }

        // 当外部切换 tab 时，滑块自动滑到对应位置
        LaunchedEffect(selectedIndex, maxW) {
            // 如果已经在目标位置附近（说明是重组/初始化），直接 snap 避免动画
            // 但考虑到 LaunchedEffect 是在 Composition 后执行，如果是首次进入，上面的 remember 已经处理了初始值。
            // 只有当 selectedIndex 真正变化时才需要 animateTo。
            
            val targetDp = when (selectedIndex) {
                0 -> leftStop
                1 -> centerStop
                else -> rightStop
            }
            val targetPx = with(density) { targetDp.toPx() }

            // 如果当前位置和目标位置差距很大（说明是切换了 tab），才播放动画
            // 如果差距很小（说明是旋转屏幕/重组），直接 snap 或者不做
            if (abs(sliderX.value - targetPx) > 1f) {
                sliderX.animateTo(
                    targetValue = targetPx,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                )
            } else {
                sliderX.snapTo(targetPx)
            }
        }

        // 背景（白底）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        )

        // 滑块（10% 透明度，拖拽+吸附）
        Box(
            modifier = Modifier
                .absoluteOffset(
                    x = with(density) { sliderX.value.toDp() },
                    y = sliderTop
                )
                .size(sliderW, sliderH)
                .clip(RoundedCornerShape(sliderRadius))
                .background(themeColor.copy(alpha = 0.10f))
                .zIndex(10f) // 关键：层级高于 icon/文字
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val minPx = with(density) { leftStop.toPx() }
                        val maxPx = with(density) { rightStop.toPx() }
                        val newX = (sliderX.value + delta).coerceIn(minPx, maxPx)
                        scope.launch { sliderX.snapTo(newX) }
                    },
                    onDragStopped = {
                        val pxLeft = with(density) { leftStop.toPx() }
                        val pxCenter = with(density) { centerStop.toPx() }
                        val pxRight = with(density) { rightStop.toPx() }

                        val cur = sliderX.value
                        val nearestIndex = listOf(
                            0 to abs(cur - pxLeft),
                            1 to abs(cur - pxCenter),
                            2 to abs(cur - pxRight),
                        ).minBy { it.second }.first

                        val targetPx = when (nearestIndex) {
                            0 -> pxLeft
                            1 -> pxCenter
                            else -> pxRight
                        }

                        scope.launch {
                            sliderX.animateTo(
                                targetValue = targetPx,
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = Spring.DampingRatioMediumBouncy
                                )
                            )
                        }

                        // 吸附后切页
                        navController.navigate(routes[nearestIndex].route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
        )

        // 计算 icon 的绝对 X（关键：这是“绝对坐标”，必须用 absoluteOffset）
        val sliderXdp = with(density) { sliderX.value.toDp() }
        val sliderCenterIconX = sliderXdp + (sliderW - itemW) / 2

        // ✅ icon 位置由滑块停靠点决定：保证吸附到左右时 icon 在滑块水平中心
        val leftIconX = leftStop + (sliderW - itemW) / 2
        val centerIconX = centerStop + (sliderW - itemW) / 2
        val rightIconX = rightStop + (sliderW - itemW) / 2


        // 左：首页
        BottomBarItem(
            modifier = Modifier
                .absoluteOffset(x = leftIconX, y = 0.dp)
                .size(itemW, itemH),
            label = "首页",
            color = themeColor,
            iconTop = iconTop,
            textBottom = textBottom,
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_home),
                    contentDescription = "首页",
                    tint = themeColor,
                    modifier = Modifier.size(iconSize)
                )
            },
            onClick = {
                navController.navigate(AppRoute.Home.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        // 中：备用（第二页）
        BottomBarItem(
            modifier = Modifier
                .absoluteOffset(x = centerIconX, y = 0.dp)
                .size(itemW, itemH),
            label = "备用",
            color = themeColor,
            iconTop = iconTop,
            textBottom = textBottom,
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_home),
                    contentDescription = "备用",
                    tint = themeColor,
                    modifier = Modifier.size(iconSize)
                )
            },
            onClick = {
                navController.navigate(AppRoute.NewFeature.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        // 右：我的（第三页）
        BottomBarItem(
            modifier = Modifier
                .absoluteOffset(x = rightIconX, y = 0.dp)
                .size(itemW, itemH),
            label = "我的",
            color = themeColor,
            iconTop = iconTop,
            textBottom = textBottom,
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_profile),
                    contentDescription = "我的",
                    tint = themeColor,
                    modifier = Modifier.size(iconSize)
                )
            },
            onClick = {
                navController.navigate(AppRoute.Profile.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

@Composable
private fun BottomBarItem(
    modifier: Modifier,
    label: String,
    color: Color,
    iconTop: Dp,
    textBottom: Dp,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier.clickableNoRipple(onClick),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = iconTop)
        ) {
            icon()
        }

        Text(
            text = label,
            color = color,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = textBottom)
        )
    }
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    return this.then(
        Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick
        )
    )
}
