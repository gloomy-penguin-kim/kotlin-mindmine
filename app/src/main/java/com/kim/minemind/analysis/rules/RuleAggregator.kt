package com.kim.minemind.analysis.rules

import com.kim.minemind.analysis.Conflict
import com.kim.minemind.analysis.ConflictSource
import com.kim.minemind.domain.Board
import java.util.BitSet

class RuleAggregator(
    val board: Board
) {

    // gid -> decision
    private var decisions = mutableMapOf<Int, RuleType>()

    // gid -> explanation reasons
    private var reasons = mutableMapOf<Int, Rule>()

    // gid -> conflict
    private var conflicts = mutableMapOf<Int, Conflict>()


    fun getReasons(): Map<Int, Rule> = reasons

    fun getConflicts(): Map<Int, Conflict> = conflicts

    fun forcedFlags(): Set<Int> =
        decisions.filterValues { it == RuleType.MINE }.keys

    fun forcedOpens(): Set<Int> =
        decisions.filterValues { it == RuleType.SAFE }.keys

    fun combine(other: RuleAggregator) {
        for (dec in other.forcedFlags()) {
            decisions[dec] = RuleType.MINE
        }
        for (dec in other.forcedOpens()) {
            decisions[dec] = RuleType.SAFE
        }
        conflicts += other.getConflicts()
        reasons += other.getReasons()
    }
    fun combine(other: RuleResult) {
        for (dec in other.forcedFlags) {
            decisions[dec] = RuleType.MINE
        }
        for (dec in other.forcedOpens) {
            decisions[dec] = RuleType.SAFE
        }
        conflicts += other.conflicts
        reasons += other.reasons
    }

    fun addRule(mask: BitSet, localToGlobal: IntArray, rule: Rule) {

        val gid = rule.gid

        if (board.isCellVisible(gid)) return
        if (gid in conflicts) return

        val existing = decisions[gid]

        // First proposal
        if (existing == null) {
            decisions[gid] = rule.type
            reasons.getOrPut(gid) { Rule(gid, rule.type, mutableSetOf()) }
                .reasons.addAll(rule.reasons)
            return
        }

        // Same decision → merge explanation
        if (existing == rule.type) {
            reasons[gid]?.reasons?.addAll(rule.reasons)
            return
        }

        // CONTRADICTION detected
        addConflicts(mask, localToGlobal, rule.reasons)
    }

    fun addConflict(gid: Int, newReasons: Set<String>) {

        if (board.isCellVisible(gid)) return

        val conflict = conflicts.getOrPut(gid) { Conflict(gid, ConflictSource.RULE, mutableSetOf()) }
        conflict.reasons.addAll(newReasons)

        decisions.remove(gid)
        reasons.remove(gid)
    }

    fun addConflicts(
        mask: BitSet,
        localToGlobal: IntArray,
        newReasons: Set<String>
    ) {
        var bit = mask.nextSetBit(0)

        while (bit >= 0) {
            addConflict(localToGlobal[bit], newReasons)
            bit = mask.nextSetBit(bit + 1)
        }
    }

    fun isNotEmpty(): Boolean =
        decisions.isNotEmpty() || conflicts.isNotEmpty()
}
