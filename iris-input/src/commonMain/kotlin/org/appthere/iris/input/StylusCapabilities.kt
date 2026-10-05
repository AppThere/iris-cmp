package org.appthere.iris.input

/** What a stylus source can report. */
public data class StylusCapabilities(
    val pressure: Boolean,
    val tilt: Boolean,
    val hover: Boolean,
    val eraser: Boolean,
    val rotation: Boolean,
    val predicted: Boolean,
)
