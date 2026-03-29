package com.sliide.usermanager

import androidx.compose.ui.window.ComposeUIViewController
import com.sliide.usermanager.ui.App

/**
 * Creates the UIViewController that hosts the shared Compose UI on iOS.
 * Called from Swift via the KMP framework.
 */
fun MainViewController() = ComposeUIViewController { App() }
