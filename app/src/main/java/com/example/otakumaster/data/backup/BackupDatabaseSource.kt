package com.example.otakumaster.data.backup

import android.content.Context
import android.util.Log
import com.example.otakumaster.data.db.OtakuDatabase
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object BackupDatabaseSource {
    private const val DB_FILE_NAME = "otaku_master.db"
    private const val TAG = "BackupDatabaseSource"

    //定位到 otaku_master.db 的真实文件路径
    fun dbFile(context: Context): File {
        return context.getDatabasePath(DB_FILE_NAME)
    }

    //对 Room/SQLite 执行 wal_checkpoint ，把 WAL 内容刷回主库（导出时更一致）
    fun checkpoint(database: OtakuDatabase) {
        Log.d(TAG, "checkpoint start")
        val supportDb = database.openHelper.writableDatabase
        supportDb.query("PRAGMA wal_checkpoint(FULL)").use { c ->
            if (c.moveToFirst()) {
                val busy = c.getInt(0)
                val log = c.getInt(1)
                val checkpointed = c.getInt(2)
                Log.d(TAG, "checkpoint FULL busy=$busy log=$log checkpointed=$checkpointed")
            }
        }
        supportDb.query("PRAGMA wal_checkpoint(TRUNCATE)").use { c ->
            if (c.moveToFirst()) {
                val busy = c.getInt(0)
                val log = c.getInt(1)
                val checkpointed = c.getInt(2)
                Log.d(TAG, "checkpoint TRUNCATE busy=$busy log=$log checkpointed=$checkpointed")
            }
        }
        Log.d(TAG, "checkpoint done")
    }

    //先 checkpoint，再返回可用于读取的 db 文件（并检查文件存在）
    fun dbFileForExport(context: Context, database: OtakuDatabase): File {
        checkpoint(database)

        val file = dbFile(context)
        check(file.exists()) { "数据库文件不存在：${file.absolutePath}" }
        return file
    }

    /**
     * 为导出创建一个“数据库快照副本”。
     *
     * 重要：
     * - 直接对真实 db 文件算 sha256 再打包，理论上可能不一致（导出期间若仍有写入）。
     * - 最稳的方式是：先复制出快照文件，然后对“同一个快照”计算 hash 与打包。
     */
    fun copyDbForExport(context: Context, database: OtakuDatabase, dstFile: File): File {
        val srcFile = dbFile(context)
        val supportDb = database.openHelper.writableDatabase

        Log.d(
            TAG,
            "copyDbForExport start src=${srcFile.absolutePath} dst=${dstFile.absolutePath}"
        )

        checkpoint(database)

        deleteSqliteSidecars(dstFile)
        if (dstFile.exists()) dstFile.delete()
        dstFile.parentFile?.mkdirs()

        val vacuumSucceeded = try {
            val escapedPath = dstFile.absolutePath.replace("'", "''")
            supportDb.execSQL("VACUUM INTO '$escapedPath'")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "VACUUM INTO failed, fallback to file copy", e)
            false
        }

        if (!vacuumSucceeded) {
            FileInputStream(srcFile).use { input ->
                FileOutputStream(dstFile).use { output ->
                    BufferedOutputStream(output).use { buffered ->
                        input.copyTo(buffered)
                        output.fd.sync()
                    }
                }
            }
        }

        check(dstFile.exists() && dstFile.length() > 0L) {
            "数据库快照生成失败：${dstFile.absolutePath}"
        }

        Log.d(TAG, "copyDbForExport done dstSize=${dstFile.length()}")
        return dstFile
    }

    private fun deleteSqliteSidecars(dbFile: File) {
        val parent = dbFile.parentFile ?: return
        val name = dbFile.name
        File(parent, "$name-wal").delete()
        File(parent, "$name-shm").delete()
        File(parent, "$name-journal").delete()
    }
}

