package com.kim.minemind.analysis.frontier

data class Component(
    val k: Int = 0,
    val constraints: List<Constraint>,
    val localToGlobal: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Component

        if (k != other.k) return false
        if (constraints != other.constraints) return false
        if (!localToGlobal.contentEquals(other.localToGlobal)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = k
        result = 31 * result + constraints.hashCode()
        result = 31 * result + localToGlobal.contentHashCode()
        return result
    }
}