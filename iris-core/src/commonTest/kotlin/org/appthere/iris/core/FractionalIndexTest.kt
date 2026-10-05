package org.appthere.iris.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FractionalIndexTest {
    private val site = SiteId(0)

    @Test
    fun theFirstKeyIsTheZeroIntegerWithTheSiteTail() {
        assertEquals("a00001", FractionalIndex.between(null, null, site).key)
        assertEquals("a00011", FractionalIndex.between(null, null, SiteId(1)).key)
    }

    @Test
    fun appendingAndPrependingStayOutsideTheExistingKey() {
        val first = FractionalIndex.between(null, null, site)

        val after = FractionalIndex.between(first, null, site)
        val before = FractionalIndex.between(null, first, site)

        assertTrue(before < first && first < after, "$before < $first < $after")
    }

    @Test
    fun insertingBetweenGivesAKeyStrictlyBetween() {
        val low = FractionalIndex.parseOrNull("a0")!!
        val high = FractionalIndex.parseOrNull("a1")!!

        val middle = FractionalIndex.between(low, high, site)

        assertTrue(low < middle && middle < high, "$low < $middle < $high")
    }

    @Test
    fun aKeyThatIsAPrefixOfTheUpperBoundStillFitsItsTail() {
        val low = FractionalIndex.parseOrNull("a0")!!
        val high = FractionalIndex.parseOrNull("a1V")!!

        val middle = FractionalIndex.between(low, high, site)

        assertTrue(low < middle && middle < high, "$low < $middle < $high")
    }

    @Test
    fun aMidpointThatIsAlsoAPrefixIsExtendedUntilItIsNot() {
        // Found by FractionalIndexProperties: "a2" + midpoint "001" is still a prefix of the upper bound,
        // so the site tail "0001" would reproduce it exactly.
        val low = FractionalIndex.parseOrNull("a1")!!
        val high = FractionalIndex.parseOrNull("a20010001")!!

        val middle = FractionalIndex.between(low, high, SiteId(0))

        assertTrue(low < middle && middle < high, "$low < $middle < $high")
    }

    @Test
    fun distinctSitesNeverCollideInTheSameGap() {
        val low = FractionalIndex.parseOrNull("a0")!!
        val high = FractionalIndex.parseOrNull("a1")!!

        assertNotEquals(FractionalIndex.between(low, high, SiteId(7)), FractionalIndex.between(low, high, SiteId(8)))
    }

    @Test
    fun boundsMustBeInOrder() {
        val low = FractionalIndex.parseOrNull("a0")!!
        val high = FractionalIndex.parseOrNull("a1")!!

        assertFailsWith<IllegalArgumentException> { FractionalIndex.between(high, low, site) }
        assertFailsWith<IllegalArgumentException> { FractionalIndex.between(low, low, site) }
    }

    @Test
    fun validKeysParseAndRoundTrip() {
        listOf("a0", "a1", "az", "b00", "Zz", "a0V", "a00001", "Xzzz0V1").forEach {
            assertEquals(it, assertNotNull(FractionalIndex.parseOrNull(it), it).key)
        }
    }

    @Test
    fun malformedKeysAreRejected() {
        val rejected =
            listOf(
                "",
                "a",
                "b0",
                "a0!",
                "a00",
                "a0V0",
                "0a",
                "A" + "0".repeat(26),
            )

        rejected.forEach { assertNull(FractionalIndex.parseOrNull(it), "'$it' should be rejected") }
    }

    @Test
    fun siteIdsMustFitInThreeBase62Digits() {
        assertEquals(238_327, SiteId(238_327).value)
        assertFailsWith<IllegalArgumentException> { SiteId(238_328) }
        assertFailsWith<IllegalArgumentException> { SiteId(-1) }
    }
}
