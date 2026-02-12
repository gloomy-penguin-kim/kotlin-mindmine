package com.kim.minemind.analysis.frontier

data class Scope(val gids: IntArray, val remaining: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Scope

        if (remaining != other.remaining) return false
        if (!gids.contentEquals(other.gids)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = remaining
        result = 31 * result + gids.contentHashCode()
        return result
    }
}