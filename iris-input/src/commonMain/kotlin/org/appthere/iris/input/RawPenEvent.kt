package org.appthere.iris.input

import kotlin.time.Duration

/**
 * One pointer sample as a platform adapter delivers it, before normalization (docs/architecture.md section 7).
 * Minimal Phase 0 shape; Phase 3 adds buttons and whatever the adapter spikes show is needed.
 *
 * @property x surface pixels.
 * @property y surface pixels.
 * @property timestamp hardware time since the source's own origin.
 * @property pressure 0..1, meaningful only when [axes] contains [PenAxes.PRESSURE]; likewise for the other axes.
 * @property tilt radians from vertical.
 * @property azimuth radians around the vertical axis.
 * @property twist barrel rotation in radians.
 * @property hoverDistance device units above the surface.
 */
public data class RawPenEvent(
    val action: PenAction,
    val tool: PenTool,
    val x: Float,
    val y: Float,
    val timestamp: Duration,
    val pressure: Float = 0f,
    val tilt: Float = 0f,
    val azimuth: Float = 0f,
    val twist: Float = 0f,
    val hoverDistance: Float = 0f,
    val axes: PenAxes = PenAxes.NONE,
)
