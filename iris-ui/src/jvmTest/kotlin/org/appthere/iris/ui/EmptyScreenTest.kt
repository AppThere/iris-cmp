package org.appthere.iris.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

class EmptyScreenTest {
    @OptIn(ExperimentalTestApi::class) // runComposeUiTest is the multiplatform UI test entry point; one opt-in site.
    @Test
    fun theAppShowsAnEmptyScreen() =
        runComposeUiTest {
            setContent { IrisApp() }

            onNodeWithTag(IRIS_EMPTY_SCREEN_TAG).assertIsDisplayed()
        }
}
