package com.example.otakumaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.example.otakumaster.ui.theme.OtakuMasterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 沉浸式边缘到边缘显示（模板默认），不影响业务逻辑
        lifecycleScope.launch { (application as OtakuMasterApp).appVersionRepository.initOnAppStart() }
        setContent { // Compose UI 入口：后续所有页面/导航都从这里接入
            OtakuMasterTheme { // 项目主题（颜色/字体等），后续 UI 统一在这里配置
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding -> // 基础脚手架：方便以后加底部导航/顶部栏
                    Greeting(name = "Android", modifier = Modifier.padding(innerPadding)) // 临时占位页面：后续换成真正首页
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) { // 简单可预览的 Composable：用于验证项目能正常运行
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() { // 预览用：不参与运行时逻辑
    OtakuMasterTheme { Greeting("Android") }
}
