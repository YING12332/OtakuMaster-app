package com.example.otakumaster

import AppNavHost
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.otakumaster.ui.components.AppBottomBar
import com.example.otakumaster.ui.components.SlidingBottomBar
import com.example.otakumaster.ui.navigation.bottomRoutes
import com.example.otakumaster.ui.theme.OtakuMasterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 沉浸式边缘到边缘显示（模板默认），不影响业务逻辑
        lifecycleScope.launch { (application as OtakuMasterApp).appVersionRepository.initOnAppStart() }
        setContent { // Compose UI 入口：后续所有页面/导航都从这里接入
            OtakuMasterTheme { // 项目主题（颜色/字体等），后续 UI 统一在这里配置
                MainApp()
            }
        }
    }
}

@Composable
private fun MainApp(){
    val navController= rememberNavController()
    // ✅ 判断当前页面是否属于底部导航的三个 route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = bottomRoutes.any { route ->
        currentDestination?.hierarchy?.any { it.route == route.route } == true
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {//只在3个主页面时才显示导航栏
                Box(Modifier.navigationBarsPadding()) {
                    SlidingBottomBar(navController = navController)
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
