package app.orchard.tv.wallpaper

/** Conservative caption-based topics; these are local hints, not Bing metadata. */
object PhotoThemes {
    private val words = linkedMapOf(
        "space" to listOf("galaxy", "milky way", "nebula", "aurora", "星空", "银河", "星云", "极光", "宇宙"),
        "animals" to listOf("bird", "penguin", "bear", "fox", "tiger", "lion", "whale", "elephant", "panda", "wildlife", "wolf", "wolves", "sloth", "deer", "butterfly", "eagle", "鸟", "企鹅", "熊", "狐狸", "老虎", "狮", "鲸", "大象", "动物", "狼", "树懒", "鹿", "蝶", "鹰"),
        "city" to listOf("city", "skyline", "bridge", "castle", "cathedral", "temple", "palace", "village", "town", "library", "reading room", "城市", "大桥", "城堡", "教堂", "寺", "宫殿", "小镇", "村庄", "建筑", "桥", "阅览室", "图书馆"),
        "water" to listOf("ocean", "sea ", "coast", "beach", "lake", "river", "waterfall", "reef", "island", "海", "湖", "河", "瀑布", "岛", "礁"),
        "nature" to listOf("mountain", "forest", "park", "desert", "flower", "valley", "tree", "canyon", "meadow", "glacier", "山", "森林", "公园", "沙漠", "花", "峡谷", "树", "草原", "冰川"),
    )
    fun of(title: String, caption: String): String {
        val text = "$title $caption".lowercase(java.util.Locale.ROOT)
        return words.entries.firstOrNull { (_, terms) -> terms.any { it in text } }?.key ?: "other"
    }
}
