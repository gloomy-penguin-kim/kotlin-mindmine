package com.kim.minemind.analysis.rules


import com.kim.minemind.analysis.frontier.Component
import java.util.BitSet

fun equivalenceRule(
    comp: Component,
    moves: RuleAggregator
) {
    fun isProperSubset(a: BitSet, b: BitSet): Boolean {
        val inter = (a.clone() as BitSet).apply { and(b) }
        return inter == a && a != b
    }

    fun difference(b: BitSet, a: BitSet): BitSet =
        (b.clone() as BitSet).apply { andNot(a) }

    fun enqueueMovesForMask(mask: BitSet, ruleType: RuleType, reasonList: MutableSet<String>) {
        var bit = mask.nextSetBit(0)
        while (bit >= 0) {
            val gid = comp.localToGlobal[bit]
            moves.addRule(
                (mask.clone() as BitSet),
                comp.localToGlobal,
                Rule(gid=gid,
                    type=ruleType,
                    reasons=reasonList)
            )
            bit = mask.nextSetBit(bit + 1)
        }
    }

    val constraints = comp.constraints
    val n = constraints.size

    for (i in 0 until n) {
        val a = constraints[i].mask
        val remA = constraints[i].remaining
        if (a.isEmpty) continue

        for (j in i + 1 until n) {
            val b = constraints[j].mask
            val remB = constraints[j].remaining
            if (b.isEmpty) continue

            // (1) equality contradiction
            if (a == b && remA != remB) {
                moves.addConflicts(
                    mask = (a.clone() as BitSet),
                    localToGlobal = comp.localToGlobal,
                    newReasons = setOf("Equality Contradiction: A==B but remaining differs",
//                        "A=$a rem=$remA",
//                        "B=$b rem=$remB"
                    )
                )
                continue
            }

            // (2) A ⊂ B and remA == remB => (B\A) safe
            if (remA == remB && isProperSubset(a, b)) {
                val diff = difference(b, a)
                if (!diff.isEmpty) {
                    enqueueMovesForMask(
                        (diff.clone() as BitSet),
                        RuleType.SAFE,
                        mutableSetOf(
                            "Equivalence: A⊂B and rem(A)==rem(B) -> B\\A SAFE",
//                            "A=$a rem=$remA subset of",
//                            "B=$b rem=$remB"
                        )
                    )
                }
            }

            // (2) B ⊂ A and remA == remB => (A\B) safe
            if (remA == remB && isProperSubset(b, a)) {
                val diff = difference(a, b)
                if (!diff.isEmpty) {
                    enqueueMovesForMask(
                        (diff.clone() as BitSet),
                        RuleType.SAFE,
                        mutableSetOf(
                            "Equivalence: B⊂A and rem(A)==rem(B) -> A\\B SAFE",
//                            "B=$b rem=$remB subset of",
//                            "A=$a rem=$remA"
                        )
                    )
                }
            }
        }
    }
}
