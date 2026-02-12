package com.kim.minemind

import com.kim.minemind.analysis.enumeration.ProbabilityEngine
import com.kim.minemind.analysis.caching.SolverCaches
import com.kim.minemind.analysis.frontier.Component
import com.kim.minemind.analysis.frontier.Constraint
import org.junit.Assert.*
import org.junit.Test
import java.util.BitSet

class EnumerationTest {

    private fun engine() =
        ProbabilityEngine(SolverCaches())

    private fun mask(vararg idx: Int): BitSet {
        val b = BitSet()
        idx.forEach { b.set(it) }
        return b
    }

    // ------------------------------------------------------------
    // Forced mine
    // ------------------------------------------------------------
    @Test
    fun forcedMine() {

        val comp = Component(
            k = 1,
            constraints = listOf(
                Constraint.of(mask(0), 1)
            ),
            localToGlobal = intArrayOf(99)
        )

        val res = engine().enumerateComponentWithPropagation(comp)

        assertEquals(1, res.solutions)
        assertArrayEquals(intArrayOf(1), res.mineCounts)
    }

    // ------------------------------------------------------------
    // Forced safe
    // ------------------------------------------------------------
    @Test
    fun forcedSafe() {

        val comp = Component(
            k = 1,
            constraints = listOf(
                Constraint.of(mask(0), 0)
            ),
            localToGlobal = intArrayOf(5)
        )

        val res = engine().enumerateComponentWithPropagation(comp)

        assertEquals(1, res.solutions)
        assertArrayEquals(intArrayOf(0), res.mineCounts)
    }

    // ------------------------------------------------------------
    // Symmetric pair
    // x0 + x1 = 1
    // ------------------------------------------------------------
    @Test
    fun symmetricPair() {

        val comp = Component(
            k = 2,
            constraints = listOf(
                Constraint.of(mask(0,1), 1)
            ),
            localToGlobal = intArrayOf(0,1)
        )

        val res = engine().enumerateComponentWithPropagation(comp)

        assertEquals(2, res.solutions)
        assertArrayEquals(intArrayOf(1,1), res.mineCounts)
    }

    // ------------------------------------------------------------
    // Propagation collapse
    // x0 + x1 = 2
    // ------------------------------------------------------------
    @Test
    fun propagationCollapse() {

        val comp = Component(
            k = 2,
            constraints = listOf(
                Constraint.of(mask(0,1), 2)
            ),
            localToGlobal = intArrayOf(10,11)
        )

        val res = engine().enumerateComponentWithPropagation(comp)

        assertEquals(1, res.solutions)
        assertArrayEquals(intArrayOf(1,1), res.mineCounts)
    }

    // ------------------------------------------------------------
    // Contradiction
    // ------------------------------------------------------------
    @Test
    fun contradiction() {

        val comp = Component(
            k = 1,
            constraints = listOf(
                Constraint.of(mask(0), 1),
                Constraint.of(mask(0), 0)
            ),
            localToGlobal = intArrayOf(1)
        )

        val res = engine().enumerateComponentWithPropagation(comp)

        assertEquals(0, res.solutions)
    }

    // ------------------------------------------------------------
    // Probability mapping
    // ------------------------------------------------------------
    @Test
    fun probabilityMapping() {

        val comp = Component(
            k = 2,
            constraints = listOf(
                Constraint.of(mask(0,1), 1)
            ),
            localToGlobal = intArrayOf(7,8)
        )

        val eng = engine()
        val res = eng.enumerateComponentWithPropagation(comp)

        val map = mutableMapOf<Int,Float>()
        eng.probsForComponentGid(comp, res, 10, { false }, map)

        assertEquals(0.5f, map[7]!!, 0.0001f)
        assertEquals(0.5f, map[8]!!, 0.0001f)
    }

    // ------------------------------------------------------------
    // 🔥 Important: cache hit sanity
    // ------------------------------------------------------------
    @Test
    fun cacheReuse() {

        val eng = engine()

        val comp = Component(
            k = 2,
            constraints = listOf(
                Constraint.of(mask(0,1), 1)
            ),
            localToGlobal = intArrayOf(0,1)
        )

        val r1 = eng.getComponentResult(comp)
        val r2 = eng.getComponentResult(comp)

        assertSame(r1, r2)
    }
}
