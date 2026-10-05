package org.appthere.iris.core

import kotlin.jvm.JvmInline

/**
 * Who generates fractional index keys. Distinct sites inserting into the same gap at the same time
 * get distinct keys (D-010). Encoded as three base-62 digits, so values are 0 until [COUNT].
 */
@JvmInline
public value class SiteId(
    public val value: Int,
) {
    init {
        require(value in 0 until COUNT) { "SiteId must be in 0 until $COUNT, was $value" }
    }

    public companion object {
        /** 62^3: the number of distinct site ids. */
        public const val COUNT: Int = 238_328
    }
}
