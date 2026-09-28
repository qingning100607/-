package com.qingning.cloudrest.data

import android.content.SharedPreferences

/**
 * 容忍类型的 SharedPreferences 读取。
 *
 * 背景：备份导入时 JSON 的数字会**丢失原始宽度**——`193` 这种「小 long」会被解析成 Int，
 * 于是 prefs 里存的类型和代码里 `getLong()` 期待的 Long 不一致，直接抛
 * ClassCastException，表现为「导入备份后 App 一启动就闪退」。
 *
 * 这里把所有读取统一走一遍类型归一：任何数字类型都能安全读出，字符串里的数字也认。
 * 这样即使拿到脏数据（旧版本备份 / 手改过的文件）也只是数值不对，绝不会崩。
 */
object Prefs {

    private fun raw(p: SharedPreferences, k: String): Any? =
        if (p.contains(k)) p.all[k] else null

    fun getLong(p: SharedPreferences, k: String, def: Long): Long = when (val v = raw(p, k)) {
        is Long -> v
        is Int -> v.toLong()
        is Float -> v.toLong()
        is Double -> v.toLong()
        is String -> v.toLongOrNull() ?: def
        else -> def
    }

    fun getInt(p: SharedPreferences, k: String, def: Int): Int = when (val v = raw(p, k)) {
        is Int -> v
        is Long -> v.toInt()
        is Float -> v.toInt()
        is Double -> v.toInt()
        is String -> v.toDoubleOrNull()?.toInt() ?: def
        else -> def
    }

    fun getFloat(p: SharedPreferences, k: String, def: Float): Float = when (val v = raw(p, k)) {
        is Float -> v
        is Int -> v.toFloat()
        is Long -> v.toFloat()
        is Double -> v.toFloat()
        is String -> v.toFloatOrNull() ?: def
        else -> def
    }

    fun getBoolean(p: SharedPreferences, k: String, def: Boolean): Boolean = when (val v = raw(p, k)) {
        is Boolean -> v
        is Int -> v != 0
        is Long -> v != 0L
        is String -> when (v.lowercase()) {
            "true", "1" -> true
            "false", "0" -> false
            else -> def
        }
        else -> def
    }

    fun getString(p: SharedPreferences, k: String, def: String?): String? = when (val v = raw(p, k)) {
        null -> def
        is String -> v
        else -> v.toString()
    }

    /** 把任意数字类型归一成 Long（用于遍历 p.all 的统计逻辑） */
    fun asLong(v: Any?): Long? = when (v) {
        is Long -> v
        is Int -> v.toLong()
        is Float -> v.toLong()
        is Double -> v.toLong()
        is String -> v.toLongOrNull()
        else -> null
    }

    /** 把任意数字类型归一成 Int（用于遍历 p.all 的统计逻辑） */
    fun asInt(v: Any?): Int? = asLong(v)?.toInt()
}
