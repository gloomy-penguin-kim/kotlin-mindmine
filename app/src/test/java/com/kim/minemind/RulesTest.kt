package com.kim.minemind
import com.kim.minemind.analysis.caching.SolverCaches
import com.kim.minemind.analysis.frontier.Component
import com.kim.minemind.analysis.frontier.Constraint
import com.kim.minemind.analysis.frontier.Frontier
import com.kim.minemind.analysis.rules.RuleAggregator
import com.kim.minemind.analysis.rules.RuleEngine
import com.kim.minemind.analysis.rules.equivalenceRule
import com.kim.minemind.analysis.rules.singlesRule
import com.kim.minemind.analysis.rules.subsetsRule
import com.kim.minemind.domain.Board
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.BitSet

class RulesTest {

    @Test
    fun equivalenceRule_marksConflicts_whenEqualMaskDifferentRemaining() {

        val board = Board.newGame(
            rows = 3,
            cols = 3,
            mineIds = emptySet()
        )

        val localToGlobal = intArrayOf(0, 1, 2)

        val m = BitSet().apply {
            set(0)
            set(1)
            set(2)
        }

        val comp = Component(
            k = 3,
            constraints = listOf(
                Constraint.of(m, remaining = 1),
                Constraint.of(m, remaining = 2),
            ),
            localToGlobal = localToGlobal
        )

        val agg = RuleAggregator(board)

        equivalenceRule(comp, agg)

        val conflicts = agg.getConflicts()

        assertTrue(
            conflicts.keys.containsAll(listOf(0,1,2))
        )
        assertTrue(conflicts.isNotEmpty())
    }


    fun testAggregator(comp: Component): RuleAggregator {
        val board = Board.newGame(3,3, emptySet())
        val agg = RuleAggregator(board)
        equivalenceRule(comp, agg)
        return agg
    }


    @Test
    fun singles_marksScopeSafe_whenZeroRemaining() {

        val board = Board.newGame(
            rows = 2,
            cols = 2,
            mineIds = emptySet()
        )

        val localToGlobal = intArrayOf(0, 1, 2, 3)

        val m = BitSet().apply {
            set(1)
            set(2)
        }

        val comp = Component(
            k = 3,
            constraints = listOf(
                Constraint.of(m, remaining = 0)
            ),
            localToGlobal = localToGlobal
        )

        val agg = RuleAggregator(board)

        singlesRule(comp, agg)

        val opens = agg.forcedOpens()
        val conflicts = agg.getConflicts()

        println("opens=$opens")
        println("conflicts=$conflicts")

        assertTrue(opens.size == 2)
        assertTrue(1 in opens)
        assertTrue(2 in opens)
        assertTrue(conflicts.isEmpty())
    }

    @Test
    fun singles_marksScopeMines_whenRemainingEqualsScope() {

        val board = Board.newGame(
            rows = 2,
            cols = 2,
            mineIds = emptySet()
        )

        val localToGlobal = intArrayOf(0, 1, 2, 3)

        val m = BitSet().apply {
            set(0)
            set(3)
        }

        val comp = Component(
            k = 2,
            constraints = listOf(
                Constraint.of(m, remaining = 2)
            ),
            localToGlobal = localToGlobal
        )

        val agg = RuleAggregator(board)
        singlesRule(comp, agg)

        val flags = agg.forcedFlags()
        val conflicts = agg.getConflicts()

        println("flags=$flags")

        assertTrue(flags.size == 2)
        assertTrue(0 in flags)
        assertTrue(3 in flags)
        assertTrue(conflicts.isEmpty())
    }


    @Test
    fun subsets_ASubB_SameRemaining_MarksDiff_Safe() {

        val board = Board.newGame(2, 2, emptySet())

        val localToGlobal = intArrayOf(0, 1, 2, 3)

        val m1 = BitSet().apply { set(0); set(1) }
        val m2 = BitSet().apply { set(0); set(1); set(2) }

        val comp = Component(
            k = 3,
            constraints = listOf(
                Constraint.of(m1, remaining = 2),
                Constraint.of(m2, remaining = 2)
            ),
            localToGlobal = localToGlobal
        )

        val agg = RuleAggregator(board)
        subsetsRule(comp, agg)

        val opens = agg.forcedOpens()

        println("opens=$opens")

        assertTrue(opens.size == 1)
        assertTrue(2 in opens)
        assertTrue(agg.getConflicts().isEmpty())
    }

    @Test
    fun subsets_ASubB_RemainingDiff_Mines() {

        val board = Board.newGame(2, 2, emptySet())

        val localToGlobal = intArrayOf(0, 1, 2, 3)

        val m1 = BitSet().apply { set(0); set(1) }
        val m2 = BitSet().apply { set(0); set(1); set(2) }

        val comp = Component(
            k = 3,
            constraints = listOf(
                Constraint.of(m1, remaining = 1),
                Constraint.of(m2, remaining = 2)
            ),
            localToGlobal = localToGlobal
        )

        val agg = RuleAggregator(board)
        subsetsRule(comp, agg)

        val flags = agg.forcedFlags()

        println("flags=$flags")

        assertTrue(flags.size == 1)
        assertTrue(2 in flags)
        assertTrue(agg.getConflicts().isEmpty())
    }

    @Test
    fun ruleEngine_pipeline_singlesSafeIntegration() {

        // Mine at 0 so we can flag it
        val board0 = Board.newGame(
            rows = 3,
            cols = 3,
            mineIds = setOf(0)
        )

        // Reveal center (gid 4)
        val board1 = board0.reveal(4)

        // Flag the mine
        val board = board1.toggleFlag(0)

        val frontier = Frontier().build(board)

        val engine = RuleEngine(SolverCaches())
        val result = engine.evaluate(board, frontier)

        println("forcedOpens = ${result.forcedOpens}")
        println("conflicts = ${result.conflicts}")

        // All other neighbors of center should be SAFE
        val expectedSafe = setOf(1,2,3,5,6,7,8) - setOf(0)

        assertTrue(result.forcedOpens.containsAll(expectedSafe))
        assertTrue(result.conflicts.isEmpty())
    }


    @Test
    fun singles_conflict_whenSafeAndMineDisagree() {

        val board = Board.newGame(2, 2, emptySet())

        val localToGlobal = intArrayOf(0,1,2,3)

        val mask = BitSet().apply { set(0) }

        val comp = Component(
            k = 1,
            constraints = listOf(
                Constraint.of(mask, remaining = 0), // SAFE
                Constraint.of(mask, remaining = 1)  // MINE
            ),
            localToGlobal = localToGlobal
        )

        val agg = RuleAggregator(board)

        singlesRule(comp, agg)

        val conflicts = agg.getConflicts()

        println(conflicts)

        assertTrue(conflicts.containsKey(0))
    }

    @Test
    fun subset_conflict_whenDerivedSafeOpposesMine() {

        val board = Board.newGame(2,2, emptySet())

        val localToGlobal = intArrayOf(0,1,2,3)

        val mA = BitSet().apply { set(0) }
        val mB = BitSet().apply { set(0); set(1) }
        val mC = BitSet().apply { set(1) }

        val comp = Component(
            k = 2,
            constraints = listOf(
                Constraint.of(mA, remaining = 0),
                Constraint.of(mB, remaining = 0), // → 1 SAFE
                Constraint.of(mC, remaining = 1)  // → 1 MINE
            ),
            localToGlobal = localToGlobal
        )

        val agg = RuleAggregator(board)

        subsetsRule(comp, agg)
        singlesRule(comp, agg) // needed for final constraint

        val conflicts = agg.getConflicts()

        println(conflicts)

        assertTrue(conflicts.containsKey(1))
    }



}