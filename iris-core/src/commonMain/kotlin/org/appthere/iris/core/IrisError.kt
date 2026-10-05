package org.appthere.iris.core

/**
 * An expected failure: bad file, unsupported feature, over budget (docs/architecture.md section 16).
 * Each module defines its own sealed hierarchy implementing this interface; a sealed type here
 * could not be extended outside `iris-core`.
 */
public interface IrisError {
    /** For developers: the data needed to diagnose (counts, ids), never document content or user file paths. */
    public val message: String
}
