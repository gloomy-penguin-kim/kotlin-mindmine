package com.kim.minemind.analysis.frontier

import java.util.BitSet

data class Constraint(
    val mask: BitSet,
    val remaining: Int
) {
    companion object {
        fun of(mask: BitSet, remaining: Int): Constraint =
            Constraint(mask.clone() as BitSet, remaining)
    }
}