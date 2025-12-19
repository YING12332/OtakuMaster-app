package com.example.otakumaster.core

import java.util.UUID

object IdGenerator {
    fun newId(): String = UUID.randomUUID().toString() // 生成全局唯一 ID，导入/合并不冲突
}
