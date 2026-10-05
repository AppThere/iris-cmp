package org.appthere.iris.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class IdTest {
    private val canonical = "3b1f2c0e-8d4a-4f6b-9c2e-1a2b3c4d5e6f"

    @Test
    fun layerIdRoundTripsThroughItsCanonicalString() {
        val id = LayerId.parseOrNull(canonical)

        assertEquals(canonical, id?.toString())
    }

    @Test
    fun everyIdTypeParsesTheSameCanonicalForm() {
        assertEquals(canonical, DocumentId.parseOrNull(canonical)?.toString())
        assertEquals(canonical, ArtboardId.parseOrNull(canonical)?.toString())
    }

    @Test
    fun onlyLowercaseCanonicalUuidsParse() {
        val rejected =
            listOf(
                "3B1F2C0E-8D4A-4F6B-9C2E-1A2B3C4D5E6F",
                "{3b1f2c0e-8d4a-4f6b-9c2e-1a2b3c4d5e6f}",
                "3b1f2c0e8d4a4f6b9c2e1a2b3c4d5e6f",
                "3b1f2c0e-8d4a-4f6b-9c2e-1a2b3c4d5e6",
                "not-a-uuid",
                "",
            )

        rejected.forEach { assertNull(LayerId.parseOrNull(it), it) }
    }

    @Test
    fun idsComeFromTheInjectedSource() {
        val uuids = ArrayDeque(listOf(canonical, "00000000-0000-4000-8000-000000000001"))
        val source = IdSource { kotlin.uuid.Uuid.parse(uuids.removeFirst()) }

        val first = LayerId.next(source)
        val second = LayerId.next(source)

        assertEquals(canonical, first.toString())
        assertNotEquals(first, second)
    }

    @Test
    fun theRandomSourceGivesDistinctLowercaseIds() {
        val ids = List(100) { LayerId.next(RandomIdSource) }

        assertEquals(100, ids.toSet().size)
        ids.forEach { assertEquals(it.toString(), LayerId.parseOrNull(it.toString())?.toString()) }
    }
}
