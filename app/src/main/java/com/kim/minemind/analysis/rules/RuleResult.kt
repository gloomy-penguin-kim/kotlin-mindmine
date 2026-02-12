package com.kim.minemind.analysis.rules


import com.kim.minemind.analysis.Conflict
import com.kim.minemind.domain.Action

data class RuleResult (
    val forcedFlags: Set<Int>,
    val forcedOpens: Set<Int>,
    val conflicts: Map<Int, Conflict> = LinkedHashMap(),
    val reasons: Map<Int, Rule> = LinkedHashMap()
)
