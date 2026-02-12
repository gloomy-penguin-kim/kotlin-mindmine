package com.kim.minemind.analysis


import com.kim.minemind.analysis.caching.SolverCaches
import com.kim.minemind.analysis.enumeration.ProbabilityEngine
import com.kim.minemind.analysis.frontier.Component
import com.kim.minemind.analysis.frontier.Frontier
import com.kim.minemind.analysis.rules.RuleEngine
import com.kim.minemind.domain.Board

class Analyzer {


    private val caches = SolverCaches()

    private val frontier = Frontier(caches)
    private val probabilityEngine = ProbabilityEngine(caches)
    private val ruleEngine = RuleEngine(caches)


    fun analyze(board: Board): AnalyzerOverlay {

        // 1️⃣ Frontier
        val comps = frontier.build(board)

        // 2️⃣ Rules
        val ruleResult = ruleEngine.evaluate(board, comps)

        // 3️⃣ Probabilities
        val probabilities =
            if (shouldEnumerate(comps))
                probabilityEngine.computeProbabilities(board, comps)
            else emptyMap()

        // 4️⃣ Reconcile conflicts
        val conflicts =
            ConflictResolver.merge(
                ruleResult,
                probabilities
            )

        // 5️⃣ Build overlay
        return AnalyzerOverlay(
            probabilities = probabilities,
            forcedFlags = ruleResult.forcedFlags,
            forcedOpens = ruleResult.forcedOpens,
            conflicts = conflicts,
            reasons = ruleResult.reasons
        )
    }

    private fun shouldEnumerate(comps: List<Component>): Boolean {
        val totalK = comps.sumOf { it.k }
        val maxK = comps.maxOfOrNull { it.k } ?: 0

        return totalK > 0 &&
                maxK <= AnalysisConfig.maxKPerComponent &&
                comps.size <= AnalysisConfig.maxComponents &&
                totalK <= AnalysisConfig.maxTotalK
    }
}
