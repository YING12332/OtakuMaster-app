package com.example.otakumaster.data.backup

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupPaths {
    /**
     * 把时间戳格式化为文件名可用的字符串。
     *
     * 说明：
     * - 以前只到分钟（yyyyMMdd_HHmm），连续多次导出可能重名。
     * - 现在增加毫秒（SSS），显著降低重名概率。
     */
    fun formatTimestamp(nowMillis: Long): String {
        return SimpleDateFormat("yyyyMMdd_HHmm_SSS", Locale.US).format(Date(nowMillis))
    }

    fun backupFileName(nowMillis: Long): String {
        val ts = formatTimestamp(nowMillis)
        return "OtakuMaster_${ts}.otaku_backup"
    }

    fun backupsDir(context: Context): File {
        val baseExternalDir = checkNotNull(context.getExternalFilesDir(null)) { "外部文件目录不可用" }
        return File(baseExternalDir, "backups").apply { mkdirs() }
    }

    /**
     * 生成一个不覆盖旧文件的备份输出路径。
     *
     * 说明：即使极端情况下发生同名（例如 nowMillis 被手动传入相同值），也会自动追加序号。
     */
    fun uniqueBackupFile(context: Context, nowMillis: Long): File {
        val dir = backupsDir(context)
        val baseName = backupFileName(nowMillis)

        var candidate = File(dir, baseName)
        if (!candidate.exists()) return candidate

        val nameWithoutExt = baseName.removeSuffix(".otaku_backup")
        var i = 1
        while (true) {
            candidate = File(dir, "${nameWithoutExt}_$i.otaku_backup")
            if (!candidate.exists()) return candidate
            i++
        }
    }

    /**
     * 生成临时输出文件路径（用于先写入，再 rename 成正式备份文件）。
     *
     * 说明：临时文件与正式文件放在同目录，rename 时更稳定。
     */
    fun tmpBackupFileFor(outFile: File): File {
        return File(outFile.parentFile, "${outFile.name}.tmp")
    }

    /**
     * 查找 backups 目录下最近的一个备份文件（按 lastModified 倒序）。
     *
     * 说明：
     * - 仅用于当前“测试导入：从本地文件夹直接导入最近存档”。
     * - 后续如果改为从网络获取存档，这个方法可以保留给“本地导入/离线导入”。
     */
    fun findLatestBackupFile(context: Context): File? {
        val dir = backupsDir(context)
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".otaku_backup", ignoreCase = true) } ?: return null
        return files.maxByOrNull { it.lastModified() }
    }
}

