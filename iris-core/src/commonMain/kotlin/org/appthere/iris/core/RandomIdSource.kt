package org.appthere.iris.core

import kotlin.uuid.Uuid

/** Random (version 4) UUIDs from a cryptographically secure source; the source apps use. */
public object RandomIdSource : IdSource {
    override fun newUuid(): Uuid = Uuid.random()
}
