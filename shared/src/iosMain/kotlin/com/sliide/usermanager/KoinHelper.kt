package com.sliide.usermanager

import com.sliide.usermanager.di.platformModule
import com.sliide.usermanager.di.sharedModule
import org.koin.core.context.startKoin

/**
 * Called from Swift's AppDelegate to initialize Koin.
 * iOS doesn't have an Application class, so we expose this helper.
 */
fun initKoin() {
    startKoin {
        modules(sharedModule, platformModule())
    }
}
