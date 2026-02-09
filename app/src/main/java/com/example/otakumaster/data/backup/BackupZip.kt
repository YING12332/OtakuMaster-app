package com.example.otakumaster.data.backup

import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * 负责把需要的内容写入 zip（.otaku_backup 本质就是 zip）。
 *
 * 目标结构（固定，不套根目录）：
 * manifest.json
 * db/otaku_master.db
 */
object BackupZip {

    private const val TAG = "BackupZip"

    /** zip 内数据库文件固定路径：导入时无需猜测文件名，兼容性更强。 */
    private const val ZIP_DB_PATH = "db/otaku_master.db"

    /** zip 内 manifest 固定路径。 */
    private const val ZIP_MANIFEST_PATH = "manifest.json"

    /**
     * 从备份文件中读取 manifest.json 内容。
     *
     * 业务失败场景：
     * - 备份文件不是合法 zip
     * - 缺少 manifest.json
     */
    fun readManifestJson(backupFile: File): String {
        try {
            Log.d(TAG, "read manifest start file=${backupFile.absolutePath} size=${backupFile.length()}")
            ZipFile(backupFile).use { zip ->
                val entry = zip.getEntry(ZIP_MANIFEST_PATH)
                    ?: throw BackupCorruptedException("备份文件损坏：缺少 $ZIP_MANIFEST_PATH")

                zip.getInputStream(entry).use { input ->
                    val bytes = input.readBytes()
                    Log.d(TAG, "read manifest success bytes=${bytes.size}")
                    return bytes.toString(Charsets.UTF_8)
                }
            }
        } catch (e: BackupException) {
            Log.e(TAG, "read manifest business failed", e)
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "read manifest failed", e)
            throw BackupCorruptedException("备份文件损坏：无法读取 manifest.json", e)
        }
    }

    /**
     * 从备份文件中解压数据库到指定目标文件。
     *
     * @param dstFile 目标文件（通常是 cacheDir 下的临时文件）
     */
    fun extractDbToFile(backupFile: File, dstFile: File) {
        try {
            Log.d(TAG, "extract db start file=${backupFile.absolutePath} dst=${dstFile.absolutePath}")
            if (dstFile.exists()) dstFile.delete()
            dstFile.parentFile?.mkdirs()

            ZipFile(backupFile).use { zip ->
                val entry = zip.getEntry(ZIP_DB_PATH)
                    ?: throw BackupCorruptedException("备份文件损坏：缺少 $ZIP_DB_PATH")

                zip.getInputStream(entry).use { input ->
                    FileOutputStream(dstFile).use { output ->
                        input.copyTo(output)
                        output.fd.sync()
                    }
                }
            }
            Log.d(TAG, "extract db success dstSize=${dstFile.length()}")
        } catch (e: BackupException) {
            Log.e(TAG, "extract db business failed", e)
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "extract db failed", e)
            throw BackupCorruptedException("备份文件损坏：无法解压数据库文件", e)
        }
    }

    /**
     * 写入备份压缩包。
     *
     * @param tmpFile 临时输出文件（建议 *.tmp），写完后由上层 rename 成正式文件
     * @param manifestJson manifest.json 的内容（UTF-8）
     * @param dbFile 要打包的数据库文件（otaku_master.db）
     */
    fun writeBackupZip(
        tmpFile: File,
        manifestJson: String,
        dbFile: File
    ) {
        Log.d(TAG, "write zip start tmp=${tmpFile.absolutePath} db=${dbFile.absolutePath} dbSize=${dbFile.length()} manifestLen=${manifestJson.length}")
        if (tmpFile.exists()) tmpFile.delete()
        tmpFile.parentFile?.mkdirs()

        ZipOutputStream(BufferedOutputStream(FileOutputStream(tmpFile))).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifestJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(ZIP_DB_PATH))
            FileInputStream(dbFile).use { input ->
                input.copyTo(zip)
            }
            zip.closeEntry()
        }
        Log.d(TAG, "write zip success tmpSize=${tmpFile.length()}")
    }
}

