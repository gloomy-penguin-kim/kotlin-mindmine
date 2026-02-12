package com.kim.minemind.analysis.rules

import com.kim.minemind.analysis.caching.SolverCaches
import com.kim.minemind.analysis.caching.componentSignatureStable
import com.kim.minemind.analysis.enumeration.ProbabilityResult
import com.kim.minemind.analysis.frontier.Component
import com.kim.minemind.domain.Board

class RuleEngine (
    private val caches: SolverCaches
) {
    fun evaluate(
        board: Board,
        components: List<Component>
    ): RuleResult {

        val rulesCombined = RuleAggregator(board)

        for (comp in components) {
            val sig = componentSignatureStable(comp)
            val res = caches.rules.get(sig)
            if (res != null) {
                rulesCombined.combine(res)
                continue
            }
            val rulesComp = RuleAggregator(board)
            singlesRule(comp, rulesComp)
            subsetsRule(comp, rulesComp)
            equivalenceRule(comp, rulesComp)
            caches.rules.put(sig, RuleResult(
                forcedFlags = rulesComp.forcedFlags(),
                forcedOpens = rulesComp.forcedOpens(),
                conflicts = rulesComp.getConflicts()
            ))
            rulesCombined.combine(rulesComp)
        }

        return RuleResult(
            forcedFlags = rulesCombined.forcedFlags(),
            forcedOpens = rulesCombined.forcedOpens(),
            conflicts = rulesCombined.getConflicts()
        )
    }

}