package org.appthere.iris.core

import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

/** Identifies an artboard in a document. Its string form is the lowercase canonical UUID. */
@JvmInline
public value class ArtboardId(
    public val uuid: Uuid,
) {
    override fun toString(): String = uuid.toString()

    public companion object {
        /** The id written as [text], or null unless [text] is a lowercase canonical UUID. */
        public fun parseOrNull(text: String): ArtboardId? = parseCanonicalUuidOrNull(text)?.let(::ArtboardId)

        /** A new id from [source]. */
        public fun next(source: IdSource): ArtboardId = ArtboardId(source.newUuid())
    }
}
