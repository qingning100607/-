package com.qingning.cloudrest.update

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 检查更新：从 GitHub 仓库读取最新版本号。
 *
 * 为了在「无认证 API 被限流」「raw 域名不可达」「只有 tag 没有 Release」等各种情况下都能用，
 * 这里做多级兜底，任何一级拿到结果就返回：
 *   1. raw 上的 version.json（最稳、最快）
 *   2. tags 页面 HTML 里解析（github.com 主域，通常可达）
 *   3. GitHub API：latest release → tags 列表（可能被限流，所以放最后）
 * 全部失败则返回 ok=false，由界面给出友好提示，绝不影响使用。
 */
object UpdateChecker {

    /** GitHub 仓库（owner/repo） */
    const val REPO = "qingning100607/-"

    /** 下载页（Release 列表） */
    const val PAGE = "https://github.com/$REPO/releases/latest"

    private const val RAW_VERSION = "https://raw.githubusercontent.com/$REPO/main/version.json"
    private const val TAGS_HTML = "https://github.com/$REPO/tags"
    private const val RELEASE_API = "https://api.github.com/repos/$REPO/releases/latest"
    private const val TAGS_API = "https://api.github.com/repos/$REPO/tags"

    private const val UA = "Mozilla/5.0 (Linux; Android 14) CloudRest"

    data class Result(
        val ok: Boolean,
        val hasUpdate: Boolean,
        val latest: String,
        val url: String,
    )

    /** 网络请求（务必在 IO 线程调用） */
    fun check(current: String): Result {
        val tag = fetchLatestTag() ?: return Result(false, false, "", PAGE)
        val latest = tag.trim().removePrefix("v").removePrefix("V").trim()
        val has = compare(latest, current.trim()) > 0
        return Result(true, has, latest.ifBlank { tag.trim() }, PAGE)
    }

    private fun fetchLatestTag(): String? {
        // 1) raw version.json
        httpGet(RAW_VERSION)?.let { body ->
            try {
                val v = JSONObject(body).optString("version", "").trim()
                if (v.isNotBlank()) return v
            } catch (_: Exception) {
                // 继续下一级
            }
        }
        // 2) tags 页面 HTML
        httpGet(TAGS_HTML)?.let { html ->
            val best = maxFromHtml(html)
            if (best != null) return best
        }
        // 3) latest release
        httpGet(RELEASE_API)?.let { body ->
            try {
                val t = JSONObject(body).optString("tag_name", "")
                if (t.isNotBlank()) return t
            } catch (_: Exception) {
                // 继续下一级
            }
        }
        // 4) tags 接口
        return maxFromApiTags()
    }

    private val TAG_RX = Regex("""/(?:releases/tag|tree)/([A-Za-z0-9._-]+)""")

    /** 从 tags 页面 HTML 里挑出版本号最大的 tag */
    private fun maxFromHtml(html: String): String? {
        var best: String? = null
        for (m in TAG_RX.findAll(html)) {
            val raw = m.groupValues[1]
            if (raw.isEmpty() || !raw.any { it.isDigit() }) continue
            val cur = raw.removePrefix("v").removePrefix("V")
            val old = best?.removePrefix("v")?.removePrefix("V")
            if (old == null || compare(cur, old) > 0) best = raw
        }
        return best
    }

    /** 从 tags 接口里挑出版本号最大的 tag */
    private fun maxFromApiTags(): String? {
        val tags = httpGet(TAGS_API) ?: return null
        return try {
            val arr = JSONArray(tags)
            var best: String? = null
            for (i in 0 until arr.length()) {
                val n = arr.getJSONObject(i).optString("name", "").trim()
                if (n.isBlank()) continue
                val cur = n.removePrefix("v").removePrefix("V")
                val old = best?.removePrefix("v")?.removePrefix("V")
                if (old == null || compare(cur, old) > 0) best = n
            }
            best
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
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github+json, text/html, */*")
                setRequestProperty("User-Agent", UA)
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