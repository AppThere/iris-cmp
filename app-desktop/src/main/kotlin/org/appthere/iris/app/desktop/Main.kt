package org.appthere.iris.app.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.appthere.iris.ui.IrisApp

/** Desktop entry point (Windows, macOS, Linux): one window showing the shared Iris UI. */
fun main() =
    application {
        Window(onCloseRequest = ::exitApplication, title = "Iris") {
            IrisApp()
        }
    }
