package com.sliide.usermanager

import android.app.Application
import com.sliide.usermanager.di.platformModule
import com.sliide.usermanager.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SliideApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@SliideApp)
            modules(sharedModule, platformModule())
        }
    }
}
