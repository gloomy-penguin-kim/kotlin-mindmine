package com.kim.minemind.analysis.caching

import com.kim.minemind.analysis.enumeration.ProbabilityResult
import com.kim.minemind.analysis.frontier.Component

class FrontierCache(
    capacity: Int
) {

    private val compCache =
        LruCache<BoardSignature, List<Component>>(capacity)

    private val probCache =
        LruCache<ComponentSignature, ProbabilityResult>(capacity)

    // ---------- COMPONENT CACHE ----------

    fun getComponents(sig: BoardSignature)
            = compCache.get(sig)

    fun putComponents(
        sig: BoardSignature,
        comps: List<Component>
    ) {
        compCache.put(sig, comps)
    }

    // ---------- PROB CACHE ----------

    fun getProb(comp: Component)
            = probCache.get(componentSignatureStable(comp))

    fun putProb(comp: Component, res: ProbabilityResult) {
        probCache.put(componentSignatureStable(comp), res)
    }

    fun clear() {
        compCache.clear()
        probCache.clear()
    }
}
