package com.kim.minemind.analysis.enumeration


import com.kim.minemind.analysis.caching.ComponentSignature
import com.kim.minemind.analysis.caching.LruCache
import com.kim.minemind.analysis.caching.componentSignatureStable
import com.kim.minemind.analysis.frontier.Component
import com.kim.minemind.analysis.AnalysisConfig
import com.kim.minemind.analysis.caching.FrontierCache
import com.kim.minemind.analysis.caching.SolverCaches
import com.kim.minemind.domain.Board
import kotlin.math.max

private const val UNKNOWN = -1
private const val SAFE = 0
private const val MINE = 1

// TODO: add testing for enumeration
class ProbabilityEngine (
    private val caches: SolverCaches
) {

//    ProbabilityEngine (analysis/enumeration)
//        Input: board + frontier components
//        Output: Map<gid, pMine> (plus maybe “unknown/outside frontier” if you do global accounting)
//        Pure-ish (except cache)

    companion object {
        private const val TAG = "analysis.enumeration.probability"
    }


    fun getComponentResult(comp: Component): ProbabilityResult? {

        val sig = componentSignatureStable(comp)

        caches.probability.get(sig)?.let {
            return it
        }

        val res = enumerateComponentWithPropagation(comp)
        caches.probability.put(sig, res)
        return res
    }


    fun enumerateComponentWithPropagation(comp: Component): ProbabilityResult {
        val k = comp.k
        val constraints = comp.constraints
        val cCount = constraints.size

        // constraint state
        val rem = IntArray(cCount) { i -> constraints[i].remaining }
        val unk = IntArray(cCount) { i -> constraints[i].mask.cardinality() }

        // variable state
        val state = IntArray(k) { UNKNOWN }

        // var -> constraints adjacency
        val varToConstraints: Array<IntArray> = run {
            val temp = Array(k) { ArrayList<Int>() }

            // For each constraint, record which variables (mask indices) it touches
            for (cid in 0 until cCount) {
                val m = constraints[cid].mask
                var v = m.nextSetBit(0)
                while (v >= 0) {
                    temp[v].add(cid)
                    v = m.nextSetBit(v + 1)
                }
            }
            // Freeze into arrays (faster in DFS)
            Array(k) { v -> temp[v].toIntArray() }
        }

        // also keep constraint -> vars list (for propagation)
        val consVars: Array<IntArray> = Array(cCount) { cid ->
            val m = constraints[cid].mask
            val list = ArrayList<Int>(m.cardinality())
            var v = m.nextSetBit(0)
            while (v >= 0) {
                list.add(v)
                v = m.nextSetBit(v + 1)
            }
            list.toIntArray()
        }

        // variable ordering heuristic (degree)
        val order: IntArray =
            (0 until k)
                .sortedByDescending { v -> varToConstraints[v].size }
                .toIntArray()

        val mineCounts = IntArray(k)
        var total = 0L

        // For leaf update: track mines chosen
        val mineStack = IntArray(k)
        var mineTop = 0

        // Undo stacks
        data class ConsChange(val cid: Int, val oldRem: Int, val oldUnk: Int)
        data class VarChange(val v: Int, val oldState: Int)

        // We modify arrays in place, so we must be able to revert changes exactly.
        val consUndo = ArrayList<ConsChange>(max(16, cCount))
        val varUndo = ArrayList<VarChange>(k)

        // This function:
        //    - sets variable state
        //    - updates all affected constraints
        //    - enqueues constraints that might now force moves
        fun applyVar(v: Int, newState: Int, queue: ArrayDeque<Int>): Boolean {
            val old = state[v]

            // If already set inconsistently → conflict
            if (old == newState) return true
            if (old != UNKNOWN && old != newState) return false // conflict

            // record var change
            varUndo.add(VarChange(v, old))
            state[v] = newState

            // Track mines for leaf counting
            if (newState == MINE) mineStack[mineTop++] = v

            // update only constraints that touch v
            for (cid in varToConstraints[v]) {

                // Record old constraint state for undo
                consUndo.add(ConsChange(cid, rem[cid], unk[cid]))

                // One unknown removed from this constraint
                unk[cid] = unk[cid] - 1

                // If we placed a mine, one remaining mine is satisfied
                if (newState == MINE) rem[cid] = rem[cid] - 1

                // prune
                val r = rem[cid]
                val u = unk[cid]
                if (r < 0 || r > u) return false
                if (u == 0 && r != 0) return false

                // this constraint might now force assignments
                queue.addLast(cid)
            }
            return true
        }

        fun propagate(queue: ArrayDeque<Int>): Boolean {
            while (queue.isNotEmpty()) {
                val cid = queue.removeFirst()
                val r = rem[cid]
                val u = unk[cid]
                if (u == 0) continue

                // If rem == 0 -> all unassigned vars in this constraint are SAFE
                if (r == 0) {
                    for (v in consVars[cid]) {
                        if (state[v] == UNKNOWN) {
                            if (!applyVar(v, SAFE, queue)) return false
                        }
                    }
                }
                // If rem == unk -> all unassigned vars are MINE
                else if (r == u) {
                    for (v in consVars[cid]) {
                        if (state[v] == UNKNOWN) {
                            if (!applyVar(v, MINE, queue)) return false
                        }
                    }
                }
            }
            return true
        }

        fun undoTo(consUndoSize: Int, varUndoSize: Int, mineTopSize: Int) {
            // undo constraints
            for (i in consUndo.size - 1 downTo consUndoSize) {
                val ch = consUndo[i]
                rem[ch.cid] = ch.oldRem
                unk[ch.cid] = ch.oldUnk
            }
            while (consUndo.size > consUndoSize) consUndo.removeAt(consUndo.size - 1)

            // undo vars (and mine stack)
            while (varUndo.size > varUndoSize) {
                val ch = varUndo.removeAt(varUndo.size - 1)
                state[ch.v] = ch.oldState
            }

            // Restore mine stack
            mineTop = mineTopSize
        }

        fun dfs(pos: Int) {
            // find next unassigned variable according to order
            var p = pos
            while (p < k && state[order[p]] != UNKNOWN) p++

            if (p == k) {
                // all assigned: must satisfy all constraints
                for (cid in 0 until cCount) if (rem[cid] != 0) return
                total += 1
                for (i in 0 until mineTop) mineCounts[mineStack[i]] += 1
                return
            }

            val v = order[p]

            // Try SAFE then MINE
            for (s in intArrayOf(SAFE, MINE)) {
                val consMark = consUndo.size
                val varMark = varUndo.size
                val mineMark = mineTop
                val queue = ArrayDeque<Int>()

                val ok = applyVar(v, s, queue) && propagate(queue)
                if (ok) dfs(p + 1)

                undoTo(consMark, varMark, mineMark)
            }
        }

        // Optional: initial propagation (if any constraints already force)
        run {
            val q = ArrayDeque<Int>()
            for (cid in 0 until cCount) q.addLast(cid)
            if (!propagate(q)) return ProbabilityResult(0L, IntArray(k))
        }

        dfs(0)
        return ProbabilityResult(total, mineCounts)
    }

    fun computeProbabilities(
        board: Board,
        components: List<Component>
    ): Map<Int, Float> {

        // "skip" means the cell is already known (revealed or flagged)
        fun skipGid(gid: Int): Boolean = board.isCellVisible(gid)

        val out = HashMap<Int, Float>(256)

        for (comp in components) {
            val res = getComponentResult(comp) ?: continue
            if (res.solutions == 0L) continue

            probsForComponentGid(comp, res, board.cols, ::skipGid, out)

            // If you want invariants/debug checks, call them here per cell
            // (see notes below)
        }

        return out
    }

    /**
     * Fills `dest` with probabilities for a single component.
     *
     * Probability for local index i is:
     *   mineCounts[i] / solutions
     */
    fun probsForComponentGid(
        comp: Component,
        res: ProbabilityResult,
        cols: Int,
        skipGid: (Int) -> Boolean,
        dest: MutableMap<Int, Float>
    ) {
        val sols = res.solutions
        if (sols == 0L) return

        val denom = sols.toFloat()
        val locals = comp.localToGlobal
        val counts = res.mineCounts

        // locals.size should be k; counts.size should be k
        val n = locals.size

        for (i in 0 until n) {
            val gid = locals[i]
            if (skipGid(gid)) continue

            val p = counts[i] / denom
            dest[gid] = p

            // Optional debug/invariant hook:
            // val r = gid / cols
            // val c = gid % cols
            // testInvariantsOfTheMines(board, p, r, c, comp)
        }
    }

    fun gidMapToRcMap(probsByGid: Map<Int, Float>, cols: Int): Map<Pair<Int, Int>, Float> {
        val out = HashMap<Pair<Int, Int>, Float>(probsByGid.size)
        for ((gid, p) in probsByGid) {
            out[(gid / cols) to (gid % cols)] = p
        }
        return out
    }
}