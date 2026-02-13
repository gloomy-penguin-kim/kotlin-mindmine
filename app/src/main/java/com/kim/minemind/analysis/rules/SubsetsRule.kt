package com.kim.minemind.analysis.rules


import com.kim.minemind.analysis.frontier.Component
import com.kim.minemind.domain.Action
import java.util.BitSet

fun subsetsRule (
    comp: Component,
    moves: RuleAggregator
) {
    fun processMovesForMask(mask: BitSet, action: Action, reasons: MutableSet<String>) {
        var bit = mask.nextSetBit(0)
        while (bit >= 0) {
            val gid = comp.localToGlobal[bit]  // IMPORTANT: bit is the local index
            // optionally skip if already revealed/flagged
            if (action == Action.OPEN) {
                moves.addRule(mask.clone() as BitSet,
                    comp.localToGlobal,
                    Rule(gid=gid,
                        type=RuleType.SAFE,
                        reasons=reasons
                    )
                )
            } else if (action == Action.FLAG) {
                moves.addRule(mask.clone() as BitSet,
                    comp.localToGlobal,
                    Rule(gid=gid,
                        type=RuleType.MINE,
                        reasons=reasons
                    )
                )
            }
            bit = mask.nextSetBit(bit + 1)
        }
    }

    val constraints = comp.constraints
    for (i in constraints.indices) {
        for (j in i + 1 until constraints.size) {

            val a = constraints[i].mask
            val b = constraints[j].mask
            val remA = constraints[i].remaining
            val remB = constraints[j].remaining

            // if (A ⊆ B) and A != B
            if (isProperSubset(a, b)) {
                // diff = B \ A
                val diff = difference(b, a)
                val diffSize = diff.cardinality()

                if (diff.isEmpty) continue

                // if all in B\A are SAFE
                if (remA == remB) {
                    val reasons = mutableSetOf(
                        "Subset: A⊆B, a==b -> B\\A SAFE",
//                        "${a} is a subset of",
//                        "${b}"
                    )
                    processMovesForMask((diff.clone() as BitSet), Action.OPEN, reasons)
                }
                // all in B\A are MINES
                else if (remB - remA == diffSize) {
                    val reasons = mutableSetOf(
                        "Subset: A⊆B, b-a==|B\\A| -> B\\A MINES",
//                        "${a} is a subset of",
//                        "${b}"
                    )

                    processMovesForMask((diff.clone() as BitSet), Action.FLAG, reasons)
                }
            }

            // if (B ⊆ A) and A != B
            if (isProperSubset(b, a)) {
                // diff = A \ B
                val diff = difference(a, b)
                val diffSize = diff.cardinality()

                // if all in A\B are SAFE
                if (remA == remB) {
                    val reasons = mutableSetOf(
                        "Subset: B⊆A, a==b -> A\\B SAFE",
//                        "${b} is a subset of",
//                        "${a}"
                    )
                    processMovesForMask((diff.clone() as BitSet), Action.OPEN, reasons)
                }
                // all in A\B are MINES
                else if (remA - remB == diffSize) {
                    val reasons = mutableSetOf(
                        "Subset: B⊆A, a-b==|A\\B| -> A\\B MINES",
//                        "${b} is a subset of",
//                        "${a}"
                    )
                    processMovesForMask((diff.clone() as BitSet), Action.FLAG, reasons)
                }
            }
        }
    }
}