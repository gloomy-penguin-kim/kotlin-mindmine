package com.kim.minemind.analysis.caching

import com.kim.minemind.analysis.enumeration.ProbabilityResult
import com.kim.minemind.analysis.frontier.Component
import com.kim.minemind.analysis.rules.RuleResult

class SolverCaches(
    frontierCapacity: Int = 64,
    probabilityCapacity: Int = 256,
    ruleCapacity: Int = 256
) {

    val frontier =
        LruCache<BoardSignature, List<Component>>(frontierCapacity)

    val probability =
        LruCache<ComponentSignature, ProbabilityResult>(probabilityCapacity)

    val rules =
        LruCache<ComponentSignature, RuleResult>(ruleCapacity)

    fun clearAll() {
        frontier.clear()
        probability.clear()
        rules.clear()
    }

    fun clearProbabilities() {
        probability.clear()
    }

    fun clearRules() {
        rules.clear()
    }
}
