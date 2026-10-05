package org.appthere.iris.core

import io.kotest.common.ExperimentalKotest
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.pair
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FractionalIndexProperties {
    // A fixed seed keeps the run deterministic; Kotest marks seeding experimental (single opt-in site, as D-021 asks).
    @OptIn(ExperimentalKotest::class)
    private val config = PropTestConfig(seed = 20261005L)

    /** Insertions as (position seed, site): each goes into a random gap of the list built so far. */
    private val insertions = Arb.list(Arb.pair(Arb.int(0, 1_000_000), Arb.int(0, 4)), 1..100)

    @Test
    fun everyKeyLandsStrictlyBetweenItsNeighbours() =
        runTest {
            checkAll(config, insertions) { ops ->
                val keys = build(ops)
                assertEquals(keys.sorted(), keys, "keys are not in insertion order")
                assertEquals(keys.size, keys.toSet().size, "keys are not unique")
            }
        }

    @Test
    fun distinctSitesInsertingIntoTheSameGapGetDistinctKeys() =
        runTest {
            checkAll(config, insertions, Arb.int(0, 1_000_000), Arb.int(0, 238_326)) { ops, gapSeed, firstSite ->
                val keys = build(ops)
                val gap = gapSeed % (keys.size + 1)
                val before = keys.getOrNull(gap - 1)
                val after = keys.getOrNull(gap)

                val one = FractionalIndex.between(before, after, SiteId(firstSite))
                val other = FractionalIndex.between(before, after, SiteId(firstSite + 1))

                assertNotEquals(one, other)
                listOf(one, other).forEach { assertBetween(before, it, after) }
            }
        }

    @Test
    fun keysGrowSlowlyWhenAlwaysAppending() {
        var last = FractionalIndex.between(null, null, SiteId(3))
        repeat(10_000) { last = FractionalIndex.between(last, null, SiteId(3)) }

        assertTrue(last.key.length <= MAX_END_KEY_LENGTH, "after 10 000 appends: ${last.key}")
    }

    @Test
    fun keysGrowSlowlyWhenAlwaysPrepending() {
        var first = FractionalIndex.between(null, null, SiteId(3))
        repeat(10_000) { first = FractionalIndex.between(null, first, SiteId(3)) }

        assertTrue(first.key.length <= MAX_END_KEY_LENGTH, "after 10 000 prepends: ${first.key}")
    }

    private fun build(ops: List<Pair<Int, Int>>): List<FractionalIndex> {
        val keys = mutableListOf<FractionalIndex>()
        ops.forEach { (positionSeed, site) ->
            val position = positionSeed % (keys.size + 1)
            val before = keys.getOrNull(position - 1)
            val after = keys.getOrNull(position)
            val key = FractionalIndex.between(before, after, SiteId(site))
            assertBetween(before, key, after)
            keys.add(position, key)
        }
        return keys
    }

    private fun assertBetween(
        before: FractionalIndex?,
        key: FractionalIndex,
        after: FractionalIndex?,
    ) {
        assertTrue(before == null || before < key, "$key is not after $before")
        assertTrue(after == null || key < after, "$key is not before $after")
        assertEquals(key, FractionalIndex.parseOrNull(key.key), "$key is not a valid key")
    }

    private companion object {
        /** Integer part of up to 4 characters for 10 000 steps, plus the 4-character site tail. */
        const val MAX_END_KEY_LENGTH = 8
    }
}
