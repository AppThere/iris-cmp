package org.appthere.iris.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** Test tag of the root of the (still empty) Iris screen. */
public const val IRIS_EMPTY_SCREEN_TAG: String = "iris-empty-screen"

/** The Iris UI root that every app shell shows. Empty until the editor UI arrives. */
@Composable
public fun IrisApp() {
    Box(Modifier.fillMaxSize().testTag(IRIS_EMPTY_SCREEN_TAG))
}
