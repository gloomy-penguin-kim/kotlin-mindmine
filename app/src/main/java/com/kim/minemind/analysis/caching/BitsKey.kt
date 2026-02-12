package com.kim.minemind.analysis.caching

import java.util.BitSet

class BitsKey(private val words: LongArray) {
    private val hash: Int = run {
        var h = 1
        for (w in words) h = 31 * h + (w xor (w ushr 32)).toInt()
        h
    }
    override fun hashCode(): Int = hash
    override fun equals(other: Any?): Boolean =
        other is BitsKey && words.contentEquals(other.words)

    companion object {
        fun from(bs: BitSet) = BitsKey(bs.toLongArray())
    }
}
