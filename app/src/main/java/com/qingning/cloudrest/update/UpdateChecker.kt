package com.qingning.cloudrest.update

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 检查更新：直接问 GitHub 仓库有没有比当前版本更高的 tag / release。
 * 仓库为空或未发布时不会崩，只是返回「检查失败」，由界面给出友好提示。
 */
object UpdateChecker {

    /** GitHub 仓库（owner/repo） */
    const val REPO = "qingning100607/-"

    const val PAGE = "https://github.com/$REPO"

    private const val RELEASE_API = "https://api.github.com/repos/$REPO/releases/latest"
    private const val TAGS_API = "https://api.github.com/repos/$REPO/tags"

    data class Result(
        val ok: Boolean,
        val hasUpdate: Boolean,
        val latest: String,
        val url: String,
    )

    /** 网络请求（务必在 IO 线程调用） */
    fun check(current: String): Result {
        val tag = fetchLatestTag()
        if (tag == null) {
            return Result(false, false, "", PAGE)
        }
        val latest = tag.trim().removePrefix("v").removePrefix("V").trim()
        val has = compare(latest, current.trim()) > 0
        return Result(true, has, latest.ifBlank { tag.trim() }, PAGE)
    }

    /** 先取 latest release，失败则退回 tags 列表 */
    private fun fetchLatestTag(): String? {
        val rel = httpGet(RELEASE_API)
        if (rel != null) {
            try {
                val t = JSONObject(rel).optString("tag_name", "")
                if (t.isNotBlank()) return t
            } catch (_: Exception) {
                // 落到 tags 分支
            }
        }
        val tags = httpGet(TAGS_API) ?: return null
        return try {
            val arr = JSONArray(tags)
            if (arr.length() == 0) null else arr.getJSONObject(0).optString("name", "").ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun httpGet(url: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "CloudRest-Android")
            }
            if (conn.responseCode != 200) {
                null
            } else {
                conn.inputStream.bufferedReader().use { it.readText() }
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Exception) {
                // 忽略
            }
        }
    }

    /** 版本号比较：a > b 返回正数，相等返回 0，a < b 返回负数 */
    fun compare(a: String, b: String): Int {
        val pa = a.split(".").map { it.trim().toIntOrNull() ?: 0 }
        val pb = b.split(".").map { it.trim().toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x - y
        }
        return 0
    }
}
