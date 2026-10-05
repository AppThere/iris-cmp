package org.appthere.iris.core

import kotlin.uuid.Uuid

/** Where new ids come from. Engine code takes one as a parameter so tests can supply fixed UUIDs. */
public fun interface IdSource {
    public fun newUuid(): Uuid
}
