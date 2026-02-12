package com.kim.minemind.analysis.enumeration


data class  ProbabilityResult(
    val solutions: Long,
    // length == k, counts of mine in each local index across all solutions
    val mineCounts: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProbabilityResult

        if (solutions != other.solutions) return false
        if (!mineCounts.contentEquals(other.mineCounts)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = solutions.hashCode()
        result = 31 * result + mineCounts.contentHashCode()
        return result
    }

}

