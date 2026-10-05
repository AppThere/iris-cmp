package org.appthere.iris.core

import kotlin.uuid.Uuid

private val CANONICAL_UUID = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

/** Parses only the lowercase hex-and-dash form used for ids in `.iris` files (docs/file-format.md section 2). */
internal fun parseCanonicalUuidOrNull(text: String): Uuid? =
    if (CANONICAL_UUID.matches(text)) Uuid.parse(text) else null
