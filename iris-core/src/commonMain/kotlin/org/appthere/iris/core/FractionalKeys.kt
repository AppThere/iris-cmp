package org.appthere.iris.core

// Base-62 fractional index keys (docs/architecture.md section 3), written for Iris.
// A key is an integer part followed by an optional fraction. The integer part's head character
// encodes its length: 'a'..'z' are non-negative integers of 2..27 characters, 'A'..'Z' negative
// ones of 27..2 characters. Digits are 0-9, A-Z, a-z, which ASCII orders by value, so keys compare
// as plain strings. A fraction never ends in '0', so there is always room between two keys.

internal const val DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
private const val BASE = 62
private const val ZERO_KEY = "a0"
private const val MAX_INTEGER_LENGTH = 27
private val SMALLEST_INTEGER = "A" + "0".repeat(MAX_INTEGER_LENGTH - 1)

private fun digitValue(c: Char): Int = DIGITS.indexOf(c)

private fun integerLength(head: Char): Int? =
    when (head) {
        in 'a'..'z' -> head - 'a' + 2
        in 'A'..'Z' -> 'Z' - head + 2
        else -> null
    }

private fun integerPart(key: String): String = key.take(integerLength(key[0]) ?: error("Invalid key head in '$key'"))

/** True for keys this scheme can produce or accept from a file. */
internal fun isValidKey(key: String): Boolean {
    val length = key.firstOrNull()?.let(::integerLength) ?: return false
    return key.length >= length &&
        key.all { digitValue(it) >= 0 } &&
        key != SMALLEST_INTEGER &&
        (key.length == length || key.last() != '0')
}

/**
 * A key strictly between [a] and [b] (null bounds are open ends).
 * Appending and prepending change only the integer part, so keys grow slowly at both ends.
 */
internal fun keyBetween(
    a: String?,
    b: String?,
): String =
    if (a == null) {
        if (b == null) ZERO_KEY else keyBefore(b)
    } else {
        if (b == null) keyAfter(a) else keyInside(a, b)
    }

private fun keyBefore(b: String): String {
    val ib = integerPart(b)
    return decrementInteger(ib) ?: (ib + midpoint("", b.drop(ib.length)))
}

private fun keyAfter(a: String): String {
    val ia = integerPart(a)
    return incrementInteger(ia) ?: (ia + midpoint(a.drop(ia.length), null))
}

private fun keyInside(
    a: String,
    b: String,
): String {
    val ia = integerPart(a)
    val ib = integerPart(b)
    if (ia == ib) return ia + midpoint(a.drop(ia.length), b.drop(ib.length))
    val next = incrementInteger(ia)
    return if (next != null && next < b) next else ia + midpoint(a.drop(ia.length), null)
}

/** A fraction strictly between fractions [a] and [b] (null is 1). Neither may end in '0'. */
internal fun midpoint(
    a: String,
    b: String?,
): String {
    if (b != null) {
        var n = 0
        while (a.getOrElse(n) { '0' } == b[n]) n++
        if (n > 0) return b.take(n) + midpoint(a.drop(n), b.drop(n))
    }
    val digitA = if (a.isEmpty()) 0 else digitValue(a[0])
    val digitB = if (b == null) BASE else digitValue(b[0])
    return when {
        digitB - digitA > 1 -> DIGITS[(digitA + digitB) / 2].toString()
        b != null && b.length > 1 -> b.take(1)
        else -> DIGITS[digitA] + midpoint(a.drop(1), null)
    }
}

private fun incrementInteger(x: String): String? {
    val digits = x.drop(1).toCharArray()
    for (i in digits.indices.reversed()) {
        val d = digitValue(digits[i]) + 1
        if (d < BASE) {
            digits[i] = DIGITS[d]
            return x[0] + digits.concatToString()
        }
        digits[i] = '0'
    }
    // Every digit carried: move to the next head, which changes the length by one.
    return when (val head = x[0]) {
        'Z' -> {
            ZERO_KEY
        }

        'z' -> {
            null
        }

        else -> {
            (head + 1).let {
                if (it >
                    'a'
                ) {
                    it + digits.concatToString() + "0"
                } else {
                    it + digits.concatToString().drop(1)
                }
            }
        }
    }
}

private fun decrementInteger(x: String): String? {
    val digits = x.drop(1).toCharArray()
    for (i in digits.indices.reversed()) {
        val d = digitValue(digits[i]) - 1
        if (d >= 0) {
            digits[i] = DIGITS[d]
            return x[0] + digits.concatToString()
        }
        digits[i] = 'z'
    }
    // Every digit borrowed: move to the previous head, which changes the length by one.
    return when (val head = x[0]) {
        'a' -> {
            "Zz"
        }

        'A' -> {
            null
        }

        else -> {
            (head - 1).let {
                if (it <
                    'Z'
                ) {
                    it + digits.concatToString() + "z"
                } else {
                    it + digits.concatToString().drop(1)
                }
            }
        }
    }
}
