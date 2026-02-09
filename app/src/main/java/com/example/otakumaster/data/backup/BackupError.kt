package com.example.otakumaster.data.backup

/**
 * 备份/导入导出相关的“业务异常”。
 *
 * 设计目标：
 * - 导入失败通常不是程序崩溃，而是业务失败（文件损坏、校验不通过、替换数据库失败等）。
 * - UI 层可以直接展示 userMessage 给用户，而不是把堆栈/英文错误抛给用户。
 */
sealed class BackupException(
    open val userMessage: String,
    cause: Throwable? = null
) : Exception(userMessage, cause)

/** 未找到备份文件（例如 backups 目录为空） */
class BackupNotFoundException(
    override val userMessage: String
) : BackupException(userMessage)

/** 备份文件结构/内容损坏（例如缺少 manifest.json 或 zip 无法读取） */
class BackupCorruptedException(
    override val userMessage: String,
    cause: Throwable? = null
) : BackupException(userMessage, cause)

/** manifest.json 无法解析或字段缺失 */
class BackupManifestInvalidException(
    override val userMessage: String,
    cause: Throwable? = null
) : BackupException(userMessage, cause)

/** 校验失败（例如 size/sha256 不匹配） */
class BackupVerificationFailedException(
    override val userMessage: String
) : BackupException(userMessage)

/** 替换数据库文件失败（文件占用/复制失败/长度不一致等） */
class BackupDatabaseReplaceFailedException(
    override val userMessage: String,
    cause: Throwable? = null
) : BackupException(userMessage, cause)

