package app.orchard.tv.settings

import org.json.JSONArray
import org.json.JSONObject

/** Stable IDs persist independently of translated labels and firmware capabilities. */
enum class ControlKind(val key: String) {
    Inputs("inputs"), Network("network"), Care("care"), Volume("volume"),
    Brightness("brightness"), Settings("settings"), Personalize("personalize"), Live("live")
}

enum class ControlSize(val columns: Int, val rows: Int) {
    Compact(2, 1), Wide(4, 1), Large(4, 2)
}

data class ControlSpec(val kind: ControlKind, val size: ControlSize = ControlSize.Compact, val visible: Boolean = true)
data class ControlPlacement(val spec: ControlSpec, val column: Int, val row: Int)

/** Four-column first-fit packing preserves order and never overlaps differently sized controls. */
object ControlLayout {
    fun defaults() = ControlKind.entries.map { ControlSpec(it, if (it == ControlKind.Care) ControlSize.Wide else ControlSize.Compact) }

    fun decode(json: String?): List<ControlSpec> = runCatching {
        val array = JSONArray(json ?: return defaults())
        val seen = mutableSetOf<ControlKind>()
        buildList {
            for (i in 0 until array.length().coerceAtMost(ControlKind.entries.size * 2)) {
                val item = array.optJSONObject(i) ?: continue
                val kind = ControlKind.entries.firstOrNull { it.key == item.optString("id") } ?: continue
                if (!seen.add(kind)) continue
                val size = ControlSize.entries.firstOrNull { it.name == item.optString("size") } ?: ControlSize.Compact
                add(ControlSpec(kind, size, item.optBoolean("visible", true)))
            }
            // Future controls are added without destroying an owner's layout.
            addAll(defaults().filter { it.kind !in seen })
        }
    }.getOrElse { defaults() }

    fun encode(specs: List<ControlSpec>): String = JSONArray().apply {
        specs.forEach { put(JSONObject().put("id", it.kind.key).put("size", it.size.name).put("visible", it.visible)) }
    }.toString()

    fun place(specs: List<ControlSpec>): List<ControlPlacement> {
        val occupied = mutableSetOf<Pair<Int, Int>>()
        var cursor = 0
        return specs.filter { it.visible }.map { spec ->
            while (true) {
                val x = cursor % 4
                val y = cursor / 4
                val cells = (y until y + spec.size.rows).flatMap { r -> (x until x + spec.size.columns).map { c -> c to r } }
                if (x + spec.size.columns <= 4 && cells.none { it in occupied }) {
                    occupied.addAll(cells)
                    break
                }
                cursor++
            }
            ControlPlacement(spec, cursor % 4, cursor / 4).also { cursor += spec.size.columns }
        }
    }

    fun move(specs: List<ControlSpec>, kind: ControlKind, delta: Int): List<ControlSpec> {
        val from = specs.indexOfFirst { it.kind == kind }
        if (from < 0) return specs
        val to = (from + delta).coerceIn(0, specs.lastIndex)
        return specs.toMutableList().apply { add(to, removeAt(from)) }
    }
}
