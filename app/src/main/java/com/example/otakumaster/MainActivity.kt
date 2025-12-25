package com.example.otakumaster

import AppNavHost
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.otakumaster.core.AppInfo
import com.example.otakumaster.data.network.NetworkModule
import com.example.otakumaster.data.network.model.VersionCheckResponse
import com.example.otakumaster.ui.components.SlidingBottomBar
import com.example.otakumaster.ui.navigation.bottomRoutes
import com.example.otakumaster.ui.theme.OtakuMasterTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

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
private fun MainApp() {
    val navController = rememberNavController()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val app = context.applicationContext as OtakuMasterApp

    var versionCheckResult by remember { mutableStateOf<VersionCheckResponse?>(null) }
    var showOptionalUpdate by remember { mutableStateOf(1) } // 默认 1，避免空表导致逻辑断
    var lastVersionCode by remember { mutableStateOf(AppInfo.VERSION_CODE) }
    var showUpdateSuccessDialog by remember { mutableStateOf(false) }

    var versionCheckError by remember { mutableStateOf<String?>(null) }

    var shouldShowUpdateDialog by remember { mutableStateOf(false) }
    var isForceUpdate by remember { mutableStateOf(false) }
    var updateDialogVisible by remember { mutableStateOf(false) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadId by remember { mutableStateOf<Long?>(null) }
    var downloadedLocalUri by remember { mutableStateOf<String?>(null) } // 例如 file://... 或 content://...
    var downloadError by remember { mutableStateOf<String?>(null) }

    var verifyError by remember { mutableStateOf<String?>(null) }
    var verifiedOk by remember { mutableStateOf(false) }

    var installLaunchedOnce by remember { mutableStateOf(false) }

    fun computeShouldShowUpdateDialog(
        currentVersionCode: Int,
        remote: VersionCheckResponse,
        showOptionalUpdate: Int
    ): Pair<Boolean, Boolean> {
        val needUpdate = currentVersionCode < remote.latestVersionCode
        val force = currentVersionCode < remote.minSupportedVersionCode

        val shouldShow = when {
            !needUpdate -> false
            force -> true
            showOptionalUpdate == 1 -> true
            else -> false
        }
        return shouldShow to force
    }

    LaunchedEffect(Unit) {
        // 1) 读本地版本表（拿 showOptionalUpdate / lastVersionCode）
        val local = withContext(Dispatchers.IO) { app.appVersionRepository.get() }
        if (local != null) {
            showOptionalUpdate = local.showOptionalUpdate
            lastVersionCode = local.lastVersionCode
            // ✅ 更新成功：升级后首次启动
            if (local.lastVersionCode < local.versionCode) {
                showUpdateSuccessDialog = true
            }
        }

        // 2) 每次启动都请求后端版本接口
        try {
            val remote = withContext(Dispatchers.IO) {
                NetworkModule.versionApi.checkVersion(
                    platform = "android",
                    channel = "stable",
                    currentVersionCode = AppInfo.VERSION_CODE
                )
            }
            versionCheckResult = remote

            val current = AppInfo.VERSION_CODE
            val (shouldShow, force) = computeShouldShowUpdateDialog(
                currentVersionCode = current,
                remote = remote,
                showOptionalUpdate = showOptionalUpdate
            )

            isForceUpdate = force
            shouldShowUpdateDialog = shouldShow

            // ✅ 更新成功弹窗优先
            updateDialogVisible = shouldShow && !showUpdateSuccessDialog


            Toast.makeText(
                context,
                "version ok: current=$current latest=${remote.latestVersionCode} min=${remote.minSupportedVersionCode} show=$showOptionalUpdate dialog=$updateDialogVisible",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            updateDialogVisible = false
            shouldShowUpdateDialog = false
            isForceUpdate = false

            Toast.makeText(
                context,
                "version check failed: ${e.javaClass.simpleName}: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }

        // 后续步骤我们会基于：
        // - lastVersionCode < versionCode 先弹“更新成功”
        // - showOptionalUpdate==0 时跳过非强更
        // - minSupportedVersionCode 强更拦截 + Back 退出
    }
    val activity = context as? android.app.Activity

    fun exitApp() {
        activity?.finishAffinity()
    }

    fun startApkDownload(url: String, versionName: String) {
        downloadError = null
        downloadedLocalUri = null
        verifyError = null
        verifiedOk = false
        installLaunchedOnce = false

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("OtakuMaster 更新包 $versionName")
            .setDescription("正在下载更新…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            // ✅ 存到 App 专属外部目录：不需要存储权限，最稳
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "OtakuMaster_$versionName.apk"
            )

        isDownloading = true
        downloadId = null
        val id = dm.enqueue(request)
        downloadId = id
    }

    suspend fun verifyDownloadedApk(
        localUriString: String,
        expectedSizeBytes: Long,
        expectedSha256: String?
    ): Boolean {
        val uri = Uri.parse(localUriString)

        // 用 ContentResolver 读（file:// / content:// 都支持）
        val resolver = context.contentResolver

        // 1) Size 校验（用 openAssetFileDescriptor 更可靠）
        val actualSize = withContext(Dispatchers.IO) {
            resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        }
        val size = if (actualSize > 0) actualSize else expectedSizeBytes // ✅ 临时兜底（先保证流程跑通）

        if (actualSize <= 0L) {
            verifyError = "校验失败：无法读取文件大小"
            return false
        }
        if (actualSize != expectedSizeBytes) {
            verifyError = "校验失败：文件大小不一致（实际=$actualSize, 期望=$expectedSizeBytes）"
            return false
        }

        // 2) SHA-256 校验（如果后端给了 checksum 才校验）
        val expected = expectedSha256?.trim()?.lowercase()
        if (!expected.isNullOrEmpty()) {
            val digestHex = withContext(Dispatchers.IO) {
                val md = MessageDigest.getInstance("SHA-256")
                resolver.openInputStream(uri)?.use { input ->
                    val buffer = ByteArray(1024 * 64)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        md.update(buffer, 0, read)
                    }
                } ?: return@withContext null

                md.digest().joinToString("") { b -> "%02x".format(b) }
            }

            if (digestHex == null) {
                verifyError = "校验失败：无法读取文件内容"
                return false
            }
            if (digestHex != expected) {
                verifyError = "校验失败：SHA-256 不一致"
                return false
            }
        }
        // 都通过
        verifyError = null
        return true
    }

    LaunchedEffect(downloadedLocalUri) {
        val info = versionCheckResult ?: return@LaunchedEffect
        val uriStr = downloadedLocalUri ?: return@LaunchedEffect

        installLaunchedOnce = false  // ✅ 确保不被锁死

        // 防止重复触发
        verifyError = null
        verifiedOk = false

        val ok = verifyDownloadedApk(
            localUriString = uriStr,
            expectedSizeBytes = info.apkSizeBytes,
            expectedSha256 = info.checksumSha256
        )

        verifiedOk = ok
    }

    fun launchApkInstall(localUriString: String) {
        val uri = Uri.parse(localUriString)

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setDataAndType(uri, "application/vnd.android.package-archive")
        }

        try {
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开安装器：${e.message ?: "未知错误"}", Toast.LENGTH_LONG).show()
        }
    }
    fun getApkContentUriFromDownloadManager(id: Long): Uri? {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.getUriForDownloadedFile(id) // 通常是 content://downloads/...
    }
    fun launchApkInstallPreferContentUri() {
        val s = downloadedLocalUri
        if (s.isNullOrEmpty()) {
            Toast.makeText(context, "安装失败：未找到下载文件 Uri", Toast.LENGTH_LONG).show()
            return
        }
        val uriToUse = Uri.parse(s)

        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uriToUse
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开安装器：${e.message ?: "未知错误"}", Toast.LENGTH_LONG).show()
        }
    }

    fun ensureInstallPermissionOrOpenSettings(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onGranted()
            return
        }
        val pm = context.packageManager
        val granted = pm.canRequestPackageInstalls()
        if (granted) {
            onGranted()
        } else {
            // 跳转到“允许安装未知来源应用”
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "请允许安装未知来源应用后再返回继续安装", Toast.LENGTH_LONG).show()
        }
    }


    BackHandler(enabled = updateDialogVisible && isForceUpdate) {
        exitApp()
    }

    if (showUpdateSuccessDialog) {
        AlertDialog(
            onDismissRequest = { /* 不做返回关闭，仅点确定 */ },
            title = { Text("更新成功") },
            text = { Text("已更新到 ${AppInfo.VERSION_NAME}（${AppInfo.VERSION_CODE}）") },
            confirmButton = {
                TextButton(onClick = { showUpdateSuccessDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
    if (updateDialogVisible && versionCheckResult != null) {
        val info = versionCheckResult!!

        AlertDialog(
            onDismissRequest = {
                // ✅ 强制更新：禁止关闭
                if (!isForceUpdate) updateDialogVisible = false
            },
            title = {
                Text(if (isForceUpdate) "需要更新" else "发现新版本")
            },
            text = {
                Column(
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    Text("最新版本：${info.latestVersionName}（${info.latestVersionCode}）")
                    Text("更新内容：${info.releaseNotes}")
                    if (isDownloading) {
                        Text(
                            "正在下载更新包…（可在通知栏查看进度）",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (downloadError != null) {
                        Text(
                            downloadError!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // 这里只是提示，真正的“校验+自动安装”在第8/9步做
                    if (downloadedLocalUri != null) {
                        Text(
                            "下载完成，正在准备安装…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (verifiedOk) {
                        Text("校验通过，准备安装…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (verifyError != null) {
                        Text(verifyError!!, color = MaterialTheme.colorScheme.error)
                    }
                    if (isForceUpdate) {
                        Text(
                            text = info.forceUpdateMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isDownloading,
                    onClick = {
                        val info = versionCheckResult ?: return@TextButton

                        when {
                            verifiedOk -> {
                                ensureInstallPermissionOrOpenSettings {
                                    installLaunchedOnce = true
                                    launchApkInstallPreferContentUri()
                                }
                            }
                            downloadedLocalUri == null -> {
                                startApkDownload(info.downloadUrl, info.latestVersionName)
                            }
                            else -> {
                                // 下载完但还没校验通过/或校验失败：这里先不做额外动作
                                // 用户可等待自动校验结果，或你后续加“重新下载”按钮
                            }
                        }
                    }
                ) {
                    Text(
                        when {
                            isDownloading -> "下载中…"
                            verifiedOk -> "安装"
                            downloadedLocalUri != null -> "校验中…"
                            else -> "立即更新"
                        }
                    )
                }
            },
            dismissButton = {
                if (isForceUpdate) {
                    TextButton(onClick = { exitApp() }) {
                        Text("退出应用")
                    }
                } else {
                    TextButton(onClick = { updateDialogVisible = false }) {
                        Text("稍后更新")
                    }
                }
            }
        )
    }

    LaunchedEffect(verifiedOk) {
        if (!verifiedOk) return@LaunchedEffect
        if (downloadId == null) return@LaunchedEffect

        ensureInstallPermissionOrOpenSettings {
            // 防止重复拉起（如果你有 installLaunchedOnce）
            installLaunchedOnce = true
            launchApkInstallPreferContentUri()
        }
    }



    // ✅ 判断当前页面是否属于底部导航的三个 route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = bottomRoutes.any { route ->
        currentDestination?.hierarchy?.any { it.route == route.route } == true
    }

    //注册下载完成监听（BroadcastReceiver）
    DisposableEffect(downloadId) {
        val id = downloadId
        if (id == null) return@DisposableEffect onDispose { }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (completedId != id) return

                val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val query = DownloadManager.Query().setFilterById(id)
                val cursor = dm.query(query)

                cursor.use {
                    if (it != null && it.moveToFirst()) {
                        val status =
                            it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                // ✅ 用 DownloadManager 给的 content:// Uri（安装器最认可）
                                val contentUri = dm.getUriForDownloadedFile(id)?.toString()
                                downloadedLocalUri = contentUri
                                isDownloading = false
                            }

                            DownloadManager.STATUS_FAILED -> {
                                val reason =
                                    it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                                downloadError = "下载失败（reason=$reason）"
                                isDownloading = false
                            }
                        }
                    } else {
                        downloadError = "下载状态查询失败"
                        isDownloading = false
                    }
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        onDispose { context.unregisterReceiver(receiver) }
    }

    DisposableEffect(lifecycleOwner, verifiedOk, downloadId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver

            // ✅ 从设置页回来/回到前台时：如果已校验通过，就尝试继续安装
            if (!verifiedOk) return@LifecycleEventObserver
            if (downloadId == null) return@LifecycleEventObserver
            if (installLaunchedOnce) return@LifecycleEventObserver

            // Android 8+ 权限检查：有权限才继续
            val canInstall =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.packageManager.canRequestPackageInstalls()
                } else true

            if (canInstall) {
                installLaunchedOnce = true
                launchApkInstallPreferContentUri()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
