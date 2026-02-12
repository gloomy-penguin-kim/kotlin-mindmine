package com.kim.minemind.analysis.caching

import com.kim.minemind.analysis.frontier.Component
import kotlin.math.min

data class ComponentSignature(
    val k: Int,
    val masks: Array<BitsKey>,
    val rems: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComponentSignature

        if (k != other.k) return false
        if (!masks.contentEquals(other.masks)) return false
        if (!rems.contentEquals(other.rems)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = k
        result = 31 * result + masks.contentHashCode()
        result = 31 * result + rems.contentHashCode()
        return result
    }
}

private fun compareWords(a: LongArray, b: LongArray): Int {
    val n = minOf(a.size, b.size)
    for (i in 0 until n) {
        val ai = a[i]; val bi = b[i]
        if (ai != bi) return if (ai < bi) -1 else 1
    }
    return a.size.compareTo(b.size)
}

fun componentSignatureStable(comp: Component): ComponentSignature {
    // Make pairs (words, remaining), sort, then wrap words into BitsKey
    val pairs = comp.constraints.map { c ->
        c.mask.toLongArray() to c.remaining
    }.sortedWith { p1, p2 ->
        val cmp = compareWords(p1.first, p2.first)
        if (cmp != 0) cmp else p1.second.compareTo(p2.second)
    }

    val masks = Array(pairs.size) { i -> BitsKey(pairs[i].first) }
    val rems = IntArray(pairs.size) { i -> pairs[i].second }
    return ComponentSignature(comp.k, masks, rems)
}
