package com.example.otakumaster.data.backup

import android.content.Context
import android.util.Log
import androidx.room.Database
import com.example.otakumaster.data.db.OtakuDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 备份清单（manifest.json）的最小可用结构。
 *
 * 设计目标：
 * 1) 导出时写入 zip，导入时用于校验格式、展示统计信息、辅助兼容判断。
 * 2) 字段以“可扩展”为前提；formatVersion 用于未来升级。
 */
object BackupManifest {

    private const val TAG = "BackupManifest"

    /** 当前备份格式版本号：一旦发布后不应随意修改含义，只能递增扩展。 */
    const val FORMAT_VERSION: Int = 1

    /**
     * 创建一个新的 backupId。
     *
     * 使用 UUID 作为全局唯一标识，方便：
     * - 同一设备多次备份区分
     * - 导入时去重/避免重复导入
     */
    fun newBackupId(): String = UUID.randomUUID().toString()

    /**
     * 生成 createdAt 对象。
     *
     * - timestamp：UTC 毫秒时间戳
     * - formatted：用于展示与文件命名的字符串（yyyyMMdd_HHmm_SSS）
     */
    fun createdAt(nowMillis: Long): BackupCreatedAt {
        val formatted = BackupPaths.formatTimestamp(nowMillis)
        return BackupCreatedAt(timestamp = nowMillis, formatted = formatted)
    }

    /**
     * 构造 app 信息对象。
     *
     * 注意：versionName/versionCode 需要由调用方提供（通常来自 BuildConfig 或 PackageManager）。
     */
    fun appInfo(context: Context, versionName: String, versionCode: Int): BackupAppInfo {
        return BackupAppInfo(
            packageName = context.packageName,
            versionName = versionName,
            versionCode = versionCode
        )
    }

    /**
     * 读取数据库元信息，用于导入前的兼容判断与排错。
     *
     * - roomVersion：来自 @Database(version)
     * - sqliteUserVersion：PRAGMA user_version
     * - journalMode：PRAGMA journal_mode
     * - schemaIdentityHash：room_master_table.identity_hash（如果存在）
     */
    fun databaseInfo(context: Context, database: OtakuDatabase): BackupDatabaseInfo {
        val supportDb = database.openHelper.readableDatabase

        val sqliteUserVersion = supportDb.query("PRAGMA user_version;").use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }

        val journalMode = supportDb.query("PRAGMA journal_mode;").use { c ->
            if (c.moveToFirst()) c.getString(0) else "unknown"
        }

        val schemaIdentityHash = try {
            supportDb.query("SELECT identity_hash FROM room_master_table LIMIT 1;").use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Throwable) {
            null
        }

        val roomVersion = OtakuDatabase::class.java.getAnnotation(Database::class.java)?.version ?: 0

        return BackupDatabaseInfo(
            name = BackupDatabaseSource.dbFile(context).name,
            roomVersion = roomVersion,
            sqliteUserVersion = sqliteUserVersion,
            journalMode = journalMode,
            schemaIdentityHash = schemaIdentityHash
        )
    }

    /**
     * 读取统计信息（只统计“未标注删除”的数据数量），用于写入 manifest 的 statistics 字段。
     *
     * 说明：
     * - anime/anime_series/anime_text_entry 都有 isDeleted 字段，因此统计 isDeleted=0。
     * - anime_status_event 按你的规则不可删除，因此直接统计全表数量。
     */
    suspend fun statistics(database: OtakuDatabase): BackupStatistics {
        try {
            val animeCount = database.animeDao().getAnimeCount()
            val seriesCount = database.animeSeriesDao().getAnimeSeriesCount()
            val eventCount = database.animeStatusEventDao().getAnimeStatusEventCount()
            val textEntryCount = database.animeTextEntryDao().getAnimeTextEntryCount()

            return BackupStatistics(
                animeCount = animeCount,
                seriesCount = seriesCount,
                eventCount = eventCount,
                textEntryCount = textEntryCount
            )
        } catch (e: NoSuchMethodError) {
            Log.e(TAG, "statistics dao method mismatch, fallback to raw SQL", e)
        } catch (e: AbstractMethodError) {
            Log.e(TAG, "statistics dao method mismatch, fallback to raw SQL", e)
        }

        val supportDb = database.openHelper.readableDatabase

        fun queryCount(sql: String): Long {
            return supportDb.query(sql).use { c ->
                if (c.moveToFirst()) c.getLong(0) else 0L
            }
        }

        val animeCount = queryCount("SELECT COUNT(*) FROM anime WHERE isDeleted=0")
        val seriesCount = queryCount("SELECT COUNT(*) FROM anime_series WHERE isDeleted=0")
        val eventCount = queryCount("SELECT COUNT(*) FROM anime_status_event")
        val textEntryCount = queryCount("SELECT COUNT(*) FROM anime_text_entry WHERE isDeleted=0")

        return BackupStatistics(
            animeCount = animeCount,
            seriesCount = seriesCount,
            eventCount = eventCount,
            textEntryCount = textEntryCount
        )
    }

    /**
     * 把 manifest 序列化为 JSON 字符串（UTF-8 写入 zip）。
     */
    fun toJsonString(manifest: BackupManifestData): String {
        return manifest.toJson().toString()
    }

    /**
     * 从 manifest.json 的字符串解析为对象（导入/预览用）。
     *
     * 业务失败场景：
     * - JSON 不合法
     * - 缺字段/字段类型不对
     */
    fun fromJsonString(json: String): BackupManifestData {
        try {
            Log.d(TAG, "fromJsonString start length=${json.length}")
            val obj = JSONObject(json)

            val createdAtObj = obj.getJSONObject("createdAt")
            val appObj = obj.getJSONObject("app")
            val dbObj = obj.getJSONObject("database")
            val statObj = obj.getJSONObject("statistics")
            val filesArr = obj.getJSONArray("files")

            val files = ArrayList<BackupFileEntry>(filesArr.length())
            for (i in 0 until filesArr.length()) {
                val f = filesArr.getJSONObject(i)
                files.add(
                    BackupFileEntry(
                        path = f.getString("path"),
                        size = f.getLong("size"),
                        sha256 = f.getString("sha256")
                    )
                )
            }

            return BackupManifestData(
                formatVersion = obj.getInt("formatVersion"),
                backupId = obj.getString("backupId"),
                createdAt = BackupCreatedAt(
                    timestamp = createdAtObj.getLong("timestamp"),
                    formatted = createdAtObj.getString("formatted")
                ),
                app = BackupAppInfo(
                    packageName = appObj.getString("packageName"),
                    versionName = appObj.getString("versionName"),
                    versionCode = appObj.getInt("versionCode")
                ),
                database = BackupDatabaseInfo(
                    name = dbObj.getString("name"),
                    roomVersion = dbObj.getInt("roomVersion"),
                    sqliteUserVersion = dbObj.getInt("sqliteUserVersion"),
                    journalMode = dbObj.getString("journalMode"),
                    schemaIdentityHash = if (dbObj.has("schemaIdentityHash")) dbObj.optString("schemaIdentityHash", null) else null
                ),
                statistics = BackupStatistics(
                    animeCount = statObj.getLong("animeCount"),
                    seriesCount = statObj.getLong("seriesCount"),
                    eventCount = statObj.getLong("eventCount"),
                    textEntryCount = statObj.getLong("textEntryCount")
                ),
                files = files
            )
        } catch (e: BackupException) {
            Log.e(TAG, "fromJsonString business failed", e)
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "fromJsonString failed", e)
            throw BackupManifestInvalidException("备份文件损坏：manifest.json 无法解析", e)
        }
    }
}

/** manifest.json 顶层结构 */
data class BackupManifestData(
    val formatVersion: Int,
    val backupId: String,
    val createdAt: BackupCreatedAt,
    val app: BackupAppInfo,
    val database: BackupDatabaseInfo,
    val statistics: BackupStatistics,
    val files: List<BackupFileEntry>
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("formatVersion", formatVersion)
        obj.put("backupId", backupId)
        obj.put("createdAt", createdAt.toJson())
        obj.put("app", app.toJson())
        obj.put("database", database.toJson())
        obj.put("statistics", statistics.toJson())

        val filesArr = JSONArray()
        files.forEach { filesArr.put(it.toJson()) }
        obj.put("files", filesArr)

        return obj
    }
}

/** createdAt 字段结构 */
data class BackupCreatedAt(
    val timestamp: Long,
    val formatted: String
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("timestamp", timestamp)
        obj.put("formatted", formatted)
        return obj
    }
}

/** app 字段结构 */
data class BackupAppInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Int
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("packageName", packageName)
        obj.put("versionName", versionName)
        obj.put("versionCode", versionCode)
        return obj
    }
}

/** database 字段结构 */
data class BackupDatabaseInfo(
    val name: String,
    val roomVersion: Int,
    val sqliteUserVersion: Int,
    val journalMode: String,
    val schemaIdentityHash: String?
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("name", name)
        obj.put("roomVersion", roomVersion)
        obj.put("sqliteUserVersion", sqliteUserVersion)
        obj.put("journalMode", journalMode)
        if (schemaIdentityHash != null) obj.put("schemaIdentityHash", schemaIdentityHash)
        return obj
    }
}

/** statistics 字段结构（只统计未标注删除的数据量） */
data class BackupStatistics(
    val animeCount: Long,
    val seriesCount: Long,
    val eventCount: Long,
    val textEntryCount: Long
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("animeCount", animeCount)
        obj.put("seriesCount", seriesCount)
        obj.put("eventCount", eventCount)
        obj.put("textEntryCount", textEntryCount)
        return obj
    }
}

/** files 数组元素结构 */
data class BackupFileEntry(
    val path: String,
    val size: Long,
    val sha256: String
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("path", path)
        obj.put("size", size)
        obj.put("sha256", sha256)
        return obj
    }
}

