package org.appthere.iris.core

import kotlin.jvm.JvmInline

/**
 * Orders siblings by a string key (docs/architecture.md section 3): inserting between two keys never
 * renumbers others, and keys compare as plain strings. Every generated key ends with a 4-character
 * tail from its [SiteId], so concurrent inserts into the same gap from distinct sites never collide.
 */
@JvmInline
public value class FractionalIndex private constructor(
    public val key: String,
) : Comparable<FractionalIndex> {
    override fun compareTo(other: FractionalIndex): Int = key.compareTo(other.key)

    override fun toString(): String = key

    public companion object {
        private const val SITE_DIGITS = 3

        /** A key strictly between [before] and [after]; a null bound is an open end. Requires [before] < [after]. */
        public fun between(
            before: FractionalIndex?,
            after: FractionalIndex?,
            site: SiteId,
        ): FractionalIndex {
            require(before == null || after == null || before < after) { "Bounds out of order: $before, $after" }
            var key = keyBetween(before?.key, after?.key)
            val upper = after?.key
            // While the key is a prefix of the upper bound, the tail could reach or pass that bound, so extend it.
            // Each round the rest of the bound gets shorter, and the key stays strictly between the bounds.
            while (upper != null && upper.startsWith(key)) key += midpoint("", upper.drop(key.length))
            return FractionalIndex(key + siteTail(site))
        }

        /** The index written as [key], or null if it is not a valid key. */
        public fun parseOrNull(key: String): FractionalIndex? = if (isValidKey(key)) FractionalIndex(key) else null

        /** Three base-62 digits of the site id, then '1' so the key never ends in '0'. */
        private fun siteTail(site: SiteId): String {
            var value = site.value
            val digits = CharArray(SITE_DIGITS)
            for (i in SITE_DIGITS - 1 downTo 0) {
                digits[i] = DIGITS[value % DIGITS.length]
                value /= DIGITS.length
            }
            return digits.concatToString() + "1"
        }
    }
}
