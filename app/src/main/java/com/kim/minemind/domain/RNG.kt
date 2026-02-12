package com.kim.minemind.domain

import kotlin.random.Random

class RNG(
    seed: Int,
    private val rows: Int,
    private val cols: Int,
    private val mines: Int,
    private val firstClickGid: Int
) {
    private val _rng: Random

    init {
        // Create a hash based on the input parameters
        val hash = (seed.hashCode() * 31 + rows.hashCode()) * 31 + cols.hashCode()
        val fullHash = (hash * 31 + mines.hashCode()) * 31 + firstClickGid.hashCode()
        _rng = Random(fullHash)
    }

    // Shuffle function to randomize the order of elements in a list
    fun <T> shuffle(seq: MutableList<T>) {
        seq.shuffle(_rng)
    }

    // Generate a random integer in the range [a, b]
    fun randint(a: Int, b: Int): Int {
        return _rng.nextInt(a, b + 1)
    }
}