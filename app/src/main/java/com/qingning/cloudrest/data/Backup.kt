package com.qingning.cloudrest.data

import android.content.Context
import android.content.SharedPreferences
import com.qingning.cloudrest.ui.AppSettings
import org.json.JSONObject

/** 数据备份与还原：导出 / 导入本地全部记录与设置（JSON 文件） */
object Backup {

    private const val STORE_PREFS = "cloud_rest"
    private const val SETTINGS_PREFS = "cloud_rest_settings"

    /** 导出全部数据为一个 JSON 字符串 */
    fun exportJson(context: Context): String {
        val root = JSONObject()
        root.put("app", "云朵休息室")
        root.put("format", 1)
        root.put("exported_at", System.currentTimeMillis())
        root.put("store", dump(context.getSharedPreferences(STORE_PREFS, Context.MODE_PRIVATE)))
        root.put("settings", dump(context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)))
        return root.toString(2)
    }

    /** 导入备份；成功返回 true */
    fun importJson(context: Context, text: String): Boolean {
        return try {
            val root = JSONObject(text)
            if (!root.has("store") && !root.has("settings")) return false
            write(context.getSharedPreferences(STORE_PREFS, Context.MODE_PRIVATE), root.optJSONObject("store") ?: JSONObject())
            write(context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE), root.optJSONObject("settings") ?: JSONObject())
            AppSettings.refresh()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun dump(p: SharedPreferences): JSONObject {
        val o = JSONObject()
        p.all.forEach { (k, v) ->
            when (v) {
                is Boolean -> o.put(k, v)
                is Int -> o.put(k, v)
                is Long -> o.put(k, v)
                is Float -> o.put(k, v.toDouble())
                is String -> o.put(k, v)
                else -> o.put(k, v.toString())
            }
        }
        return o
    }

    private fun write(p: SharedPreferences, o: JSONObject) {
        val e = p.edit().clear()
        val it = o.keys()
        while (it.hasNext()) {
            val k = it.next()
            when (val v = o.get(k)) {
                is Boolean -> e.putBoolean(k, v)
                is Int -> e.putInt(k, v)
                is Long -> e.putLong(k, v)
                is Double -> e.putFloat(k, v.toFloat())
                is String -> e.putString(k, v)
            }
        }
        e.apply()
    }
}