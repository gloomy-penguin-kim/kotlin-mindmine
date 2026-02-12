package com.kim.minemind.analysis.rules

import com.kim.minemind.domain.Action

enum class RuleType { SAFE, MINE, UNKNOWN }

data class Rule (
    val gid: Int,
    val type: RuleType,
    val reasons: MutableSet<String> = mutableSetOf()
) {
    fun toAction(): Action {
        return when (type) {
            RuleType.SAFE -> Action.OPEN
            RuleType.MINE -> Action.FLAG
            else -> Action.OPEN
        }
    }
}

