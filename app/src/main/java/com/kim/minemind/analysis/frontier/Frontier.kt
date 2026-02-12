package com.kim.minemind.analysis.frontier

import com.kim.minemind.analysis.caching.BoardSignature
import com.kim.minemind.analysis.caching.ComponentSignature
import com.kim.minemind.analysis.caching.FrontierCache
import com.kim.minemind.analysis.caching.LruCache
import com.kim.minemind.analysis.caching.SolverCaches
import com.kim.minemind.analysis.caching.componentSignatureStable
import com.kim.minemind.analysis.enumeration.ProbabilityResult
import com.kim.minemind.domain.Board
import com.kim.minemind.domain.CellState
import java.util.BitSet

class Frontier (
    private val caches: SolverCaches? = null
) {


    fun build(board: Board): List<Component> {

        val sig = board.signature()

        caches?.frontier?.get(sig)?.let {
            return it
        }

        val comps = computeComponents(board)

        caches?.frontier?.put(sig, comps)

        return comps
    }

    private fun constraintsAndUnknownNeighbors(
        board: Board
    ): Pair<List<Scope>, IntArray> {

        val scopes = ArrayList<Scope>()
        val seen = HashSet<String>()
        val allUnknowns = HashSet<Int>()

        for (cell in board.allCells()) {

            if (cell.state != CellState.REVEALED) continue
            if (cell.adjacentMines <= 0) continue

            var flagged = 0
            val unknownNeighbors = ArrayList<Int>()

            for (gid in board.neighborsOf(cell.id)) {

                val n = board.cell(gid)

                when (n.state) {
                    CellState.FLAGGED -> flagged++
                    CellState.REVEALED -> {
                        // ignore revealed cells
                    }
                    else -> {
                        unknownNeighbors.add(gid)
                    }
                }
            }

            if (unknownNeighbors.isEmpty()) continue

            val remaining = cell.adjacentMines - flagged
            if (remaining < 0) continue // safety guard

            val gids = unknownNeighbors.toIntArray()
            gids.sort()

            gids.forEach { allUnknowns.add(it) }

            val key = gids.joinToString(prefix="[", postfix="]") + "|$remaining"
            if (seen.add(key)) {
                scopes.add(Scope(gids, remaining))
            }
        }

        val allUnknownsSorted =
            allUnknowns.toIntArray().also { it.sort() }

        return scopes to allUnknownsSorted
    }

    private fun dsuFind(
        globalMasks: List<BitSet>
    ): Map<Int, List<Int>> {

        val c = globalMasks.size
        val dsu = DisjointSetUnion(c)

        for (i in 0 until c) {
            val mi = globalMasks[i]
            for (j in i + 1 until c) {
                if (mi.intersects(globalMasks[j])) {
                    dsu.union(i, j)
                }
            }
        }

        val components = HashMap<Int, MutableList<Int>>()

        for (cid in 0 until c) {
            val root = dsu.find(cid)
            components.getOrPut(root) { ArrayList() }.add(cid)
        }

        return components
    }


    fun computeComponents(board: Board): List<Component> {

        val (scopes, _) = constraintsAndUnknownNeighbors(board)
        if (scopes.isEmpty()) return emptyList()

        val globalMasks = ArrayList<BitSet>(scopes.size)

        for (s in scopes) {
            val bs = BitSet()
            for (gid in s.gids) bs.set(gid)
            globalMasks.add(bs)
        }

        val compsByRoot = dsuFind(globalMasks)
        val comps = ArrayList<Component>()

        for ((_, scopeIdxs) in compsByRoot) {

            val localSet = HashSet<Int>()
            for (idx in scopeIdxs)
                scopes[idx].gids.forEach { localSet.add(it) }

            val localToGlobal =
                localSet.toIntArray().also { it.sort() }

            val globalToLocal =
                HashMap<Int, Int>(localToGlobal.size * 2)

            for (i in localToGlobal.indices) {
                globalToLocal[localToGlobal[i]] = i
            }

            val constraints = ArrayList<Constraint>()

            for (idx in scopeIdxs) {
                val s = scopes[idx]
                val localMask = BitSet(localToGlobal.size)

                for (gid in s.gids) {
                    val local = globalToLocal[gid]!!
                    localMask.set(local)
                }

                constraints.add(
                    Constraint(localMask, s.remaining)
                )
            }

            constraints.sortWith(
                compareBy<Constraint> { it.mask.cardinality() }
                    .thenBy { it.mask.length() }
            )

            comps.add(
                Component(
                    k = localToGlobal.size,
                    constraints = constraints,
                    localToGlobal = localToGlobal
                )
            )
        }

        comps.sortBy { it.localToGlobal.minOrNull() ?: Int.MAX_VALUE }

        return comps
    }
}
