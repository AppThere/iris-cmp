package org.appthere.iris.core

import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

/** Identifies a document; stable for the document's whole life. Its string form is the lowercase canonical UUID. */
@JvmInline
public value class DocumentId(
    public val uuid: Uuid,
) {
    override fun toString(): String = uuid.toString()

    public companion object {
        /** The id written as [text], or null unless [text] is a lowercase canonical UUID. */
        public fun parseOrNull(text: String): DocumentId? = parseCanonicalUuidOrNull(text)?.let(::DocumentId)

        /** A new id from [source]. */
        public fun next(source: IdSource): DocumentId = DocumentId(source.newUuid())
    }
}
