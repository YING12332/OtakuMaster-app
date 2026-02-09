package com.example.otakumaster.data.backup

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.util.Log
import com.example.otakumaster.data.db.OtakuDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

object BackupRepository {

    private const val TAG = "BackupRepository"

    /**
     * 导出互斥锁：避免用户连续点击“导出”导致多个导出任务同时写同一目录/同一临时文件。
     */
    private val exportMutex = Mutex()

    /**
     * 一键导出备份（推荐入口）。
     *
     * 设计目的：
     * - UI 层只需要拿到 Context（例如 LocalContext.current），不用关心 database 的获取。
     * - 内部自动复用 Room 的单例数据库实例。
     */
    suspend fun exportBackup(
        context: Context,
        nowMillis: Long = System.currentTimeMillis()
    ): File {
        val appContext = context.applicationContext
        val database = OtakuDatabase.get(appContext)
        return exportBackup(appContext, database, nowMillis)
    }

    /**
     * 一键导出备份（底层实现，便于测试/注入）。
     *
     * 重要约束：
     * - 不删除旧备份：每次导出都会生成一个新的备份文件并保留。
     * - 一致性：先复制出数据库快照副本，再对“同一个副本”计算 sha256 并打包，避免导出期间写入导致校验失败。
     *
     * @param context 任意 Context 均可，函数内部会自动转换为 applicationContext，避免 Activity 泄漏。
     * @param database Room 数据库实例（用于 checkpoint、读取统计、读取 db 元信息）。
     * @param nowMillis 当前时间戳（默认取系统时间；测试时可注入固定值）。
     */
    suspend fun exportBackup(
        context: Context,
        database: OtakuDatabase,
        nowMillis: Long = System.currentTimeMillis()
    ): File = exportMutex.withLock {
        withContext(Dispatchers.IO) {
            val appContext = context.applicationContext

            // 1) 生成“不会覆盖旧备份”的输出文件名（必要时自动追加 _1/_2...）
            val outFile = BackupPaths.uniqueBackupFile(appContext, nowMillis)

            // 2) 临时文件：先写入 .tmp，写完再 rename 为正式备份文件，避免半成品
            val tmpFile = BackupPaths.tmpBackupFileFor(outFile)
            if (tmpFile.exists()) tmpFile.delete()

            // 3) 生成 backupId：manifest 与快照文件都使用同一个 ID，便于排错与追踪
            val backupId = BackupManifest.newBackupId()

            // 4) 导出一致性关键点：复制数据库快照副本
            //    - 后续 sha256 与 zip 内内容都基于这个“快照副本”，两者必然一致
            val backupsDir = BackupPaths.backupsDir(appContext)
            val snapshotDbFile = File(backupsDir, "otaku_master_snapshot_${backupId}.db")

            try {
                Log.d(TAG, "export start nowMillis=$nowMillis outFile=${outFile.absolutePath} tmpFile=${tmpFile.absolutePath} backupId=$backupId")
                Log.d(TAG, "export snapshot path=${snapshotDbFile.absolutePath}")

                BackupDatabaseSource.copyDbForExport(appContext, database, snapshotDbFile)
                Log.d(TAG, "export snapshot copied size=${snapshotDbFile.length()}")

                // 5) 读取 App 版本信息，写入 manifest.app
                val pkgInfo = getPackageInfoCompat(appContext.packageManager, appContext.packageName)

                val versionName = pkgInfo.versionName ?: ""
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkgInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    pkgInfo.versionCode
                }
                Log.d(TAG, "export app versionName=$versionName versionCode=$versionCode")

                // 6) 基于“快照副本”计算文件信息，写入 manifest.files（导入校验会用到）
                val dbSize = snapshotDbFile.length()
                val dbSha256 = sha256Hex(snapshotDbFile)
                Log.d(TAG, "export db size=$dbSize sha256=$dbSha256")

                val files = listOf(
                    BackupFileEntry(
                        path = "db/otaku_master.db",
                        size = dbSize,
                        sha256 = dbSha256
                    )
                )

                // 7) 生成 manifest.json
                val snapshotStats = queryStatisticsFromSQLiteFile(snapshotDbFile)
                Log.d(
                    TAG,
                    "export snapshotStats anime=${snapshotStats.animeCount} series=${snapshotStats.seriesCount} event=${snapshotStats.eventCount} text=${snapshotStats.textEntryCount}"
                )

                val manifest = BackupManifestData(
                    formatVersion = BackupManifest.FORMAT_VERSION,
                    backupId = backupId,
                    createdAt = BackupManifest.createdAt(nowMillis),
                    app = BackupManifest.appInfo(appContext, versionName, versionCode),
                    database = BackupManifest.databaseInfo(appContext, database),
                    statistics = snapshotStats,
                    files = files
                )

                val manifestJson = BackupManifest.toJsonString(manifest)
                Log.d(TAG, "export manifest jsonLength=${manifestJson.length}")

                // 8) 写入 zip：结构固定为 manifest.json + db/otaku_master.db
                BackupZip.writeBackupZip(
                    tmpFile = tmpFile,
                    manifestJson = manifestJson,
                    dbFile = snapshotDbFile
                )
                Log.d(TAG, "export zip written tmpSize=${tmpFile.length()}")

                // 9) 原子替换：把临时文件改名为正式备份文件
                check(tmpFile.renameTo(outFile)) {
                    "备份文件生成失败：无法将临时文件移动为正式文件：${tmpFile.absolutePath} -> ${outFile.absolutePath}"
                }
                Log.d(TAG, "export success outFile=${outFile.absolutePath} size=${outFile.length()}")

                outFile
            } catch (e: Throwable) {
                Log.e(TAG, "export failed backupId=$backupId", e)
                throw e
            } finally {
                // 10) 清理快照文件：它只用于生成备份，不应长期占用空间
                deleteSqliteSidecars(snapshotDbFile)
                if (snapshotDbFile.exists()) snapshotDbFile.delete()

                // 如果中途失败，tmpFile 可能残留；这里尽量清理
                if (tmpFile.exists()) tmpFile.delete()
            }
        }
    }

    /**
     * 导入互斥锁：避免用户连续点击“导入”导致并发替换数据库文件。
     */
    private val importMutex = Mutex()

    /**
     * 导入预览信息：用于 UI 展示“存档 vs 本地”的数据对比。
     */
    data class ImportPreview(
        val backupFile: File,
        val backupId: String,
        val createdAt: BackupCreatedAt,
        val backupStatistics: BackupStatistics,
        val localStatistics: BackupStatistics
    )

    /**
     * 导入结果：测试阶段我们返回 requiresRestart=true，提示重启以确保所有 DAO/缓存读取到新库。
     */
    data class ImportResult(
        val backupFile: File,
        val backupId: String,
        val requiresRestart: Boolean
    )

    /**
     * 读取“最近的本地备份”的预览数据（不修改本地数据库）。
     */
    suspend fun prepareLatestImportPreview(context: Context): ImportPreview = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext

        val backupFile = BackupPaths.findLatestBackupFile(appContext)
            ?: throw BackupNotFoundException("未找到本地备份文件（请先导出备份）")

        Log.d(TAG, "import preview start file=${backupFile.absolutePath} lastModified=${backupFile.lastModified()} size=${backupFile.length()}")

        val manifestJson = BackupZip.readManifestJson(backupFile)
        Log.d(TAG, "import preview manifestJson length=${manifestJson.length}")
        val manifest = BackupManifest.fromJsonString(manifestJson)
        Log.d(TAG, "import preview parsed backupId=${manifest.backupId} createdAt=${manifest.createdAt.formatted}")

        val expectedDbEntry = manifest.files.firstOrNull { it.path == "db/otaku_master.db" }
            ?: throw BackupCorruptedException("备份文件损坏：manifest.files 缺少 db/otaku_master.db")

        val importTmpDir = File(appContext.cacheDir, "backup_preview").apply { mkdirs() }
        val tmpDbFile = File(importTmpDir, "otaku_master_preview_${manifest.backupId}.db")

        val archiveStats = try {
            BackupZip.extractDbToFile(backupFile, tmpDbFile)
            val actualSize = tmpDbFile.length()
            if (actualSize != expectedDbEntry.size) {
                throw BackupVerificationFailedException(
                    "校验失败：数据库文件大小不一致（期望 ${expectedDbEntry.size}，实际 $actualSize）"
                )
            }
            val actualSha256 = sha256Hex(tmpDbFile)
            if (!actualSha256.equals(expectedDbEntry.sha256, ignoreCase = true)) {
                throw BackupVerificationFailedException(
                    "校验失败：数据库文件 sha256 不一致（期望 ${expectedDbEntry.sha256}，实际 $actualSha256）"
                )
            }
            queryStatisticsFromSQLiteFile(tmpDbFile)
        } finally {
            deleteSqliteSidecars(tmpDbFile)
            if (tmpDbFile.exists()) tmpDbFile.delete()
        }

        Log.d(
            TAG,
            "import preview archiveStats anime=${archiveStats.animeCount} series=${archiveStats.seriesCount} event=${archiveStats.eventCount} text=${archiveStats.textEntryCount}"
        )

        val localDb = OtakuDatabase.get(appContext)
        val localStats = BackupManifest.statistics(localDb)
        Log.d(TAG, "import preview localStats anime=${localStats.animeCount} series=${localStats.seriesCount} event=${localStats.eventCount} text=${localStats.textEntryCount}")

        ImportPreview(
            backupFile = backupFile,
            backupId = manifest.backupId,
            createdAt = manifest.createdAt,
            backupStatistics = archiveStats,
            localStatistics = localStats
        )
    }

    /**
     * 测试版导入：直接导入 backups 目录下最近的一个存档。
     *
     * 核心思路：
     * 1) 解压 db/otaku_master.db 到临时文件
     * 2) 对临时文件做 size/sha256 校验（与 manifest.files 对比）
     * 3) 关闭 Room 单例并替换本地数据库文件
     */
    suspend fun importLatestBackup(context: Context): ImportResult = importMutex.withLock {
        withContext(Dispatchers.IO) {
            val appContext = context.applicationContext

            val backupFile = BackupPaths.findLatestBackupFile(appContext)
                ?: throw BackupNotFoundException("未找到本地备份文件（请先导出备份）")

            Log.d(TAG, "import start file=${backupFile.absolutePath} lastModified=${backupFile.lastModified()} size=${backupFile.length()}")

            val manifestJson = BackupZip.readManifestJson(backupFile)
            Log.d(TAG, "import manifestJson length=${manifestJson.length}")
            val manifest = BackupManifest.fromJsonString(manifestJson)
            Log.d(TAG, "import manifest backupId=${manifest.backupId} createdAt=${manifest.createdAt.formatted}")

            val expectedDbEntry = manifest.files.firstOrNull { it.path == "db/otaku_master.db" }
                ?: throw BackupCorruptedException("备份文件损坏：manifest.files 缺少 db/otaku_master.db")

            val importTmpDir = File(appContext.cacheDir, "backup_import").apply { mkdirs() }
            val tmpDbFile = File(importTmpDir, "otaku_master_import_${manifest.backupId}.db")

            try {
                Log.d(TAG, "import expected size=${expectedDbEntry.size} sha256=${expectedDbEntry.sha256} tmpDb=${tmpDbFile.absolutePath}")
                BackupZip.extractDbToFile(backupFile, tmpDbFile)

                val actualSize = tmpDbFile.length()
                val actualSha256 = sha256Hex(tmpDbFile)
                Log.d(TAG, "import extracted size=$actualSize sha256=$actualSha256")

                if (actualSize != expectedDbEntry.size) {
                    throw BackupVerificationFailedException(
                        "校验失败：数据库文件大小不一致（期望 ${expectedDbEntry.size}，实际 $actualSize）"
                    )
                }

                if (!actualSha256.equals(expectedDbEntry.sha256, ignoreCase = true)) {
                    throw BackupVerificationFailedException(
                        "校验失败：数据库文件 sha256 不一致（期望 ${expectedDbEntry.sha256}，实际 $actualSha256）"
                    )
                }

                // 关闭数据库连接，确保文件可被替换
                Log.d(TAG, "import close Room instance before replace")
                OtakuDatabase.closeInstance()

                Log.d(TAG, "import replace database file")
                replaceDatabaseFile(appContext, tmpDbFile)
                Log.d(TAG, "import replace done")

                try {
                    Log.d(TAG, "import reopen Room instance after replace")
                    OtakuDatabase.get(appContext)
                } catch (e: Throwable) {
                    Log.e(TAG, "import reopen Room failed", e)
                }

                ImportResult(
                    backupFile = backupFile,
                    backupId = manifest.backupId,
                    requiresRestart = true
                )
            } catch (e: Throwable) {
                Log.e(TAG, "import failed file=${backupFile.absolutePath}", e)
                throw e
            } finally {
                deleteSqliteSidecars(tmpDbFile)
                if (tmpDbFile.exists()) tmpDbFile.delete()
            }
        }
    }

    /**
     * 用导入得到的 db 文件替换应用当前数据库文件。
     */
    private fun replaceDatabaseFile(context: Context, srcDbFile: File) {
        try {
            val targetDbFile = context.getDatabasePath("otaku_master.db")
            targetDbFile.parentFile?.mkdirs()

            val walFile = File(targetDbFile.parentFile, "${targetDbFile.name}-wal")
            val shmFile = File(targetDbFile.parentFile, "${targetDbFile.name}-shm")

            Log.d(TAG, "replace db target=${targetDbFile.absolutePath} src=${srcDbFile.absolutePath}")

            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()
            if (targetDbFile.exists()) targetDbFile.delete()

            FileInputStream(srcDbFile).use { input ->
                FileOutputStream(targetDbFile).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }

            if (!targetDbFile.exists() || targetDbFile.length() != srcDbFile.length()) {
                throw BackupDatabaseReplaceFailedException(
                    "替换数据库失败：复制后文件校验不一致"
                )
            }
        } catch (e: BackupException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "replace db failed", e)
            throw BackupDatabaseReplaceFailedException("替换数据库失败", e)
        }
    }

    private fun getPackageInfoCompat(pm: PackageManager, packageName: String): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        FileInputStream(file).use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { b -> "%02x".format(b) }
    }

    private fun queryStatisticsFromSQLiteFile(dbFile: File): BackupStatistics {
        val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        try {
            fun queryCount(sql: String): Long {
                db.rawQuery(sql, null).use { c ->
                    return if (c.moveToFirst()) c.getLong(0) else 0L
                }
            }

            return BackupStatistics(
                animeCount = queryCount("SELECT COUNT(*) FROM anime WHERE isDeleted=0"),
                seriesCount = queryCount("SELECT COUNT(*) FROM anime_series WHERE isDeleted=0"),
                eventCount = queryCount("SELECT COUNT(*) FROM anime_status_event"),
                textEntryCount = queryCount("SELECT COUNT(*) FROM anime_text_entry WHERE isDeleted=0")
            )
        } finally {
            db.close()
        }
    }

    private fun deleteSqliteSidecars(dbFile: File) {
        val parent = dbFile.parentFile ?: return
        val name = dbFile.name
        File(parent, "$name-wal").delete()
        File(parent, "$name-shm").delete()
        File(parent, "$name-journal").delete()
    }
}

