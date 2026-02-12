package com.kim.minemind.analysis

enum class ConflictSource {
    BOARD,
    RULE,
    PROBABILITY
}

data class Conflict(
    val gid: Int,
    val source: ConflictSource,
    val reasons: MutableSet<String>
)

