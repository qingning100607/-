package com.qingning.cloudrest.data

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

/** 本地数据：功德、心情记录、计数、每日句 */
object Store {

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences("cloud_rest", Context.MODE_PRIVATE)
        }
    }

    private fun p(): SharedPreferences = prefs!!

    // 电子木鱼功德
    var karma: Long
        get() = p().getLong("karma", 0)
        set(v) { p().edit().putLong("karma", v).apply() }

    // 解压统计
    var bubbles: Long
        get() = p().getLong("bubbles", 0)
        set(v) { p().edit().putLong("bubbles", v).apply() }

    var fireworks: Int
        get() = p().getInt("fireworks", 0)
        set(v) { p().edit().putInt("fireworks", v).apply() }

    var shredded: Int
        get() = p().getInt("shredded", 0)
        set(v) { p().edit().putInt("shredded", v).apply() }

    var iceBroken: Int
        get() = p().getInt("ice_broken", 0)
        set(v) { p().edit().putInt("ice_broken", v).apply() }

    // 木鱼累计敲击次数
    var muyu: Long
        get() = p().getLong("muyu", 0)
        set(v) { p().edit().putLong("muyu", v).apply() }

    // 呼吸引导完成轮次
    var breathRounds: Long
        get() = p().getLong("breath_rounds", 0)
        set(v) { p().edit().putLong("breath_rounds", v).apply() }

    // 打水漂最高连跳纪录
    var stoneBest: Int
        get() = p().getInt("stone_best", 0)
        set(v) { p().edit().putInt("stone_best", v).apply() }

    // 功德按日累计（功德打卡日历用）
    fun addKarma(n: Long = 1) {
        val k = "kday_" + todayKey()
        p().edit()
            .putLong("karma", p().getLong("karma", 0) + n)
            .putLong(k, p().getLong(k, 0) + n)
            .apply()
    }

    fun karmaOf(dateKey: String): Long = p().getLong("kday_$dateKey", 0)

    fun karmaMap(year: Int, month: Int): Map<String, Long> {
        val prefix = "kday_%04d-%02d".format(year, month)
        val r = HashMap<String, Long>()
        p().all.filterKeys { it.startsWith(prefix) }.forEach { (k, v) ->
            if (v is Long && v > 0) r[k.removePrefix("kday_")] = v
        }
        return r
    }

    fun karmaDays(): Int = p().all.keys.count { it.startsWith("kday_") }

    fun karmaStreak(): Int {
        var c = 0
        val cal = Calendar.getInstance()
        while (karmaOf(dateKey(cal)) > 0) {
            c++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return c
    }

    fun moodDays(): Int = p().all.keys.count { it.startsWith("mood_") }

    // 心情记录：key = yyyy-MM-dd，value = 1..5
    fun setMood(dateKey: String, mood: Int) {
        p().edit().putInt("mood_$dateKey", mood).apply()
    }

    fun moodOf(dateKey: String): Int = p().getInt("mood_$dateKey", 0)

    // 每日云签：key = yyyy-MM-dd，value = 签文序号（-1 = 未抽）
    fun fortuneOf(dateKey: String): Int = p().getInt("fortune_$dateKey", -1)

    fun setFortune(dateKey: String, idx: Int) {
        p().edit().putInt("fortune_$dateKey", idx).apply()
    }

    /** 累计抽签天数（成就用） */
    fun fortuneDays(): Int = p().all.entries.count { it.key.startsWith("fortune_") && (it.value as? Int ?: -1) >= 0 }

    // 云朵小精灵：上次见面时间戳
    fun petLastSeen(): Long = p().getLong("pet_last_seen", 0L)
    fun setPetLastSeen(t: Long) { p().edit().putLong("pet_last_seen", t).apply() }

    // 音景预设：每行一条 "名字\tspec"
    fun scenes(): List<Pair<String, String>> =
        (p().getString("scenes", "") ?: "").split("\n").mapNotNull { line ->
            val i = line.indexOf('\t')
            if (i <= 0) null else line.substring(0, i) to line.substring(i + 1)
        }

    fun saveScene(name: String, spec: String) {
        val list = scenes().filter { it.first != name }.toMutableList()
        list.add(name to spec)
        p().edit().putString("scenes", list.joinToString("\n") { it.first + "\t" + it.second }).apply()
    }

    fun deleteScene(name: String) {
        val rest = scenes().filter { it.first != name }
        p().edit().putString("scenes", rest.joinToString("\n") { it.first + "\t" + it.second }).apply()
    }

    fun moodMap(year: Int, month: Int): Map<String, Int> {
        val prefix = "mood_%04d-%02d".format(year, month)
        val result = HashMap<String, Int>()
        p().all.filterKeys { it.startsWith(prefix) }.forEach { (k, v) ->
            if (v is Int && v in 1..5) result[k.removePrefix("mood_")] = v
        }
        return result
    }

    fun dateKey(cal: Calendar): String =
        "%04d-%02d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))

    fun todayKey(): String = dateKey(Calendar.getInstance())

    /** 连续打卡天数（从今天往前数，今天没记录则为 0） */
    fun streak(): Int {
        var count = 0
        val cal = Calendar.getInstance()
        while (moodOf(dateKey(cal)) > 0) {
            count++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return count
    }

    /** 当月记录天数 */
    fun monthCount(): Int {
        val cal = Calendar.getInstance()
        return moodMap(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1).size
    }

    // 治愈句库
    val quotes = listOf(
        "今天也要好好吃饭，好好休息。",
        "你已经做得很好了。",
        "慢慢来，比较快。",
        "烦恼交给明天，今晚先睡个好觉。",
        "天空不会一直下雨。",
        "你值得所有温柔。",
        "深呼吸，世界没有你想的那么糟。",
        "把今天的不开心，揉成一团丢掉。",
        "允许自己休息，是了不起的能力。",
        "云会散，风会停，你会好起来。",
        "不必追赶谁的时间表。",
        "小小的进步，也值得庆祝。",
        "累了就躺一会儿，没人扣你分。",
        "你笑起来的样子一定很好看。",
        "宇宙很大，你的烦恼其实很小。",
        "今天没做完的事，明天再做也没关系。",
        "温柔地对待自己，像对待朋友一样。",
        "雨会停，天会晴，路会慢慢顺。",
        "不是每一分钟都要有意义。",
        "发呆也是一种修行。",
        "世界吵的时候，把音量调小一点。",
        "你不需要被所有人喜欢。",
        "生活给你柠檬，就做成柠檬水吧。",
        "天亮了，又是崭新的一天。",
        "慢慢走，也能到达远方。",
        "抱抱自己，你辛苦了。",
        "不要用别人的地图，找自己的路。",
        "当下的每一刻，都值得被珍惜。",
        "难过的时候，就来看一眼这里。",
        "你已经比昨天更好了。",
        "别怕，月亮在陪着你。",
        "风会记得每一朵花的香。",
        "给心情放个假，就现在。",
        "所有的不愉快，都会过期。",
        "你很特别，不需要理由。",
        "愿你的梦里有云朵和糖。",
        "停一停，听一听风的声音。",
        "今天的不开心到此为止，明天依旧光芒万丈。",
        "像云一样，轻轻松松地飘一会儿。",
        "万物皆有裂痕，那是光照进来的地方。",
        "今天也是被云朵接住的一天。",
        "不开心的时候，允许自己不笑。",
        "深呼吸三秒，再看一眼天空。",
        "你已经很努力在生活了。",
        "哪怕只完成了一点点，也是前进。",
        "世界偶尔很吵，你可以很安静。",
        "情绪来了又走，像风一样。",
        "把速度调慢，生活不会跑掉。",
        "今天适合什么也不做。",
        "记得给自己留一盏夜灯。",
        "你不需要解释你的疲惫。",
        "想哭就哭吧，雨也是这么下下来的。",
        "月亮今天也在认真营业。",
        "星星会记得你许过的愿。",
        "生活不是赶路，是感受路。",
        "休息不是偷懒，是充电。",
        "对自己也要常说：辛苦了、谢谢、没关系。",
        "口渴了要喝水，心渴了要发呆。",
        "愿你被这个世界温柔以待。",
        "安静的努力，也在发光。",
        "你不是一个人在扛。",
        "一切都来得及，别慌。",
        "把烦恼折成纸飞机，让它飞走。",
        "慢慢来，花有花期，云有云期。",
        "你的存在本身就有意义。",
        "记得抬头看看今天的晚霞。",
        "生活的糖，要慢慢尝。",
        "今晚月色真美，风也温柔。",
    )

    /** 上一次给出的句子（换句/进入页面时避免连续重复） */
    private var lastQuote: String? = null

    /** 随机一句：每次进入页面独立随机；exclude 可指定要回避的句子 */
    fun randomQuote(exclude: String? = null): String {
        val avoid = exclude ?: lastQuote
        val pool = if (avoid == null) quotes else quotes.filter { it != avoid }
        val picked = pool[(Math.random() * pool.size).toInt()]
        lastQuote = picked
        return picked
    }
}
