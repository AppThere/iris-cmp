package org.appthere.iris.core

import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

/**
 * Identifies a layer; never changes, even when the layer moves (D-010).
 * Its string form is the lowercase canonical UUID.
 */
@JvmInline
public value class LayerId(
    public val uuid: Uuid,
) {
    override fun toString(): String = uuid.toString()

    public companion object {
        /** The id written as [text], or null unless [text] is a lowercase canonical UUID. */
        public fun parseOrNull(text: String): LayerId? = parseCanonicalUuidOrNull(text)?.let(::LayerId)

        /** A new id from [source]. */
        public fun next(source: IdSource): LayerId = LayerId(source.newUuid())
    }
}
