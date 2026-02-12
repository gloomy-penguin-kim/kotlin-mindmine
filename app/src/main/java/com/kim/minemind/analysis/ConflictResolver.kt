package com.kim.minemind.analysis

import com.kim.minemind.analysis.rules.RuleResult
import com.kim.minemind.domain.Action

object ConflictResolver {

    private const val EPS = 1e-6f

    fun merge(
        rules: RuleResult,
        probs: Map<Int, Float>
    ): MutableMap<Int, Conflict> {

        val conflicts = rules.conflicts.toMutableMap()

        for ((gid, rule) in rules.reasons) {

            val p = probs[gid] ?: continue
            val action = rule.toAction()

            val disagreement =
                (action == Action.FLAG && p < 0.5f - EPS) ||
                        (action == Action.OPEN && p > 0.5f + EPS)

            if (!disagreement) continue

            val conflict = conflicts.getOrPut(gid) {
                Conflict(
                    gid = gid,
                    source = ConflictSource.PROBABILITY,
                    reasons = mutableSetOf()
                )
            }

            conflict.reasons.addAll(rule.reasons)
            conflict.reasons.add(
                "Rule contradicts probability (p=$p)"
            )
        }

        return conflicts
    }
}
