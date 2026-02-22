package com.kim.minemind.domain

import kotlin.random.Random


class RNG(seed: Long) {
    private val rng = Random(seed)

    fun <T> shuffle(list: MutableList<T>) {
        list.shuffle(rng)
    }
}



//class RNG(
//    seed: Long,
//    rows: Int,
//    cols: Int,
//    mineCount: Int,
//    firstClickGid: Int
//) {
//    private val random: Random
//
//    init {
//        val combinedSeed =
//            seed
//                .toLong()
//                .xor(rows.toLong() shl 32)
//                .xor(cols.toLong() shl 16)
//                .xor(mineCount.toLong() shl 16)
//                .xor(firstClickGid.toLong())
//
//        random = Random(combinedSeed)
//    }
//
//    fun <T> shuffle(list: MutableList<T>) {
//        list.shuffle(random)
//    }
//
//    fun nextInt(bound: Int): Int {
//        return random.nextInt(bound)
//    }
//}
