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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.otakumaster.ui.components.AppBottomBar
import com.example.otakumaster.ui.components.SlidingBottomBar
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
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Box(Modifier.navigationBarsPadding()) {
                SlidingBottomBar(navController = navController)
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
