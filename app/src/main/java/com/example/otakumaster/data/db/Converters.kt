package com.example.otakumaster.data.db

import androidx.room.TypeConverter
import org.json.JSONArray

class Converters {

    @TypeConverter
    fun fromString(value: String?): List<String> { // 数据库(String) -> Kotlin(List)
        if (value.isNullOrBlank()) return listOf()
        val arr = JSONArray(value)
        val result = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) result.add(arr.optString(i))
        return result
    }

    @TypeConverter
    fun toString(list: List<String>?): String { // Kotlin(List) -> 数据库(String)
        if (list.isNullOrEmpty()) return "[]"
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }
}
