package com.kim.minemind.analysis.debug


import com.kim.minemind.analysis.frontier.Component

/**
 * Returns gid -> componentId
 */
fun buildComponentMap(components: List<Component>): Map<Int, Int> {

    val map = HashMap<Int, Int>()

    components.forEachIndexed { compId, comp ->
        comp.localToGlobal.forEach { gid ->
            map[gid] = compId
        }
    }

    return map
}
