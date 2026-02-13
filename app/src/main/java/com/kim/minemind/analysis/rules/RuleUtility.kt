package com.kim.minemind.analysis.rules

import java.util.BitSet


fun equalMask(a: BitSet, b: BitSet): Boolean {
    val x = a.clone() as BitSet
    x.xor(b)
    return x.isEmpty
}

fun isProperSubset(a: BitSet, b: BitSet): Boolean {
    // a ⊆ b  <=>  (a ∩ b) == a
    val inter = (a.clone() as BitSet).apply { and(b) } // inter = a & b
    if (inter != a) return false
    return a != b // proper subset (not equal)
}

fun difference(b: BitSet, a: BitSet): BitSet {
    // b \ a
    return (b.clone() as BitSet).apply { andNot(a) }
}