package org.appthere.iris.app.ios

import androidx.compose.ui.window.ComposeUIViewController
import org.appthere.iris.ui.IrisApp
import platform.UIKit.UIViewController

/** iOS and iPadOS entry point; Swift calls it as `MainViewControllerKt.mainViewController()`. */
fun mainViewController(): UIViewController = ComposeUIViewController { IrisApp() }
