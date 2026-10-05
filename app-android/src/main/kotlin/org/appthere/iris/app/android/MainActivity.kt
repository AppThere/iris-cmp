package org.appthere.iris.app.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.appthere.iris.ui.IrisApp

/** Android and ChromeOS entry point: shows the shared Iris UI. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IrisApp() }
    }
}
