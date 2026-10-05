package org.appthere.iris.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IrisResultTest {
    /** How a module defines its errors: a sealed hierarchy implementing [IrisError]. */
    private sealed interface ParseError : IrisError {
        data class BadDigit(
            val index: Int,
        ) : ParseError {
            override val message: String get() = "bad digit at $index"
        }

        data object Empty : ParseError {
            override val message: String get() = "empty input"
        }
    }

    private fun parse(text: String): IrisResult<Int> =
        when {
            text.isEmpty() -> IrisResult.Failure(ParseError.Empty)
            text.all { it.isDigit() } -> IrisResult.Success(text.toInt())
            else -> IrisResult.Failure(ParseError.BadDigit(text.indexOfFirst { !it.isDigit() }))
        }

    @Test
    fun mapTransformsSuccessAndKeepsFailure() {
        assertEquals(IrisResult.Success(84), parse("42").map { it * 2 })
        assertEquals(IrisResult.Failure(ParseError.Empty), parse("").map { it * 2 })
    }

    @Test
    fun flatMapChainsAndStopsAtTheFirstFailure() {
        val half: (Int) -> IrisResult<Int> = { if (it % 2 == 0) IrisResult.Success(it / 2) else IrisResult.Failure(ParseError.BadDigit(0)) }

        assertEquals(IrisResult.Success(21), parse("42").flatMap(half))
        assertEquals(IrisResult.Failure(ParseError.BadDigit(0)), parse("7").flatMap(half))
        assertEquals(IrisResult.Failure(ParseError.BadDigit(1)), parse("4x").flatMap(half))
    }

    @Test
    fun valuesAndErrorsCanBeReadBack() {
        assertEquals(42, parse("42").getOrNull())
        assertNull(parse("").getOrNull())
        assertEquals(ParseError.Empty, parse("").errorOrNull())
        assertNull(parse("42").errorOrNull())
        assertEquals(-1, parse("").getOrElse { -1 })
    }

    @Test
    fun foldHandlesBothCases() {
        val describe: (IrisResult<Int>) -> String = { result -> result.fold({ "value $it" }, { "error: ${it.message}" }) }

        assertEquals("value 42", describe(parse("42")))
        assertEquals("error: bad digit at 2", describe(parse("12a")))
    }

    @Test
    fun onSuccessAndOnFailureRunOnlyForTheirCase() {
        val seen = mutableListOf<String>()

        parse("42").onSuccess { seen += "ok $it" }.onFailure { seen += "failed" }
        parse("").onSuccess { seen += "ok $it" }.onFailure { seen += it.message }

        assertEquals(listOf("ok 42", "empty input"), seen)
    }
}
