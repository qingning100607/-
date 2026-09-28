package com.qingning.cloudrest.update

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 检查更新：从 GitHub 仓库读取最新版本号。
 *
 * 取数顺序（前一个成功就不再往后问）：
 *  1. 仓库根目录的 version.json（raw 静态文件，最稳、不限流）
 *  2. GitHub Release 的 latest
 *  3. 仓库 tag 列表
 *
 * 全部失败会返回 ok = false，由界面给出友好提示，绝不崩溃。
 */
object UpdateChecker {

    /** GitHub 仓库（owner/repo） */
    const val REPO = "qingning100607/-"

    const val PAGE = "https://github.com/$REPO"

    private const val RAW_MAIN = "https://raw.githubusercontent.com/$REPO/main/version.json"
    private const val RAW_MASTER = "https://raw.githubusercontent.com/$REPO/master/version.json"
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
        val cur = current.trim()

        val manifest = readManifest()
        if (manifest != null) {
            val (v, u) = manifest
            return Result(true, compare(v, cur) > 0, v, u)
        }

        val tag = latestTag() ?: return Result(false, false, "", PAGE)
        val v = tag.trim().removePrefix("v").removePrefix("V").trim()
        if (v.isBlank()) return Result(false, false, "", PAGE)
        return Result(true, compare(v, cur) > 0, v, PAGE)
    }

    /** 读取仓库里的 version.json，拿到 (版本号, 下载页) */
    private fun readManifest(): Pair<String, String>? {
        for (raw in listOf(RAW_MAIN, RAW_MASTER)) {
            val body = httpGet(raw, "text/plain") ?: continue
            try {
                val json = JSONObject(body)
                val v = json.optString("version", "").trim()
                if (v.isNotBlank()) {
                    val u = json.optString("url", "").trim()
                    return v to u.ifBlank { PAGE }
                }
            } catch (_: Exception) {
                // 换下一个源
            }
        }
        return null
    }

    /** 先取 latest release，失败则退回 tags 列表 */
    private fun latestTag(): String? {
        val rel = httpGet(RELEASE_API, "application/vnd.github+json")
        if (rel != null) {
            try {
                val t = JSONObject(rel).optString("tag_name", "")
                if (t.isNotBlank()) return t
            } catch (_: Exception) {
                // 落到 tags 分支
            }
        }
        val tags = httpGet(TAGS_API, "application/vnd.github+json") ?: return null
        return try {
            val arr = JSONArray(tags)
            if (arr.length() == 0) null else arr.getJSONObject(0).optString("name", "").ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun httpGet(url: String, accept: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("Accept", accept)
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