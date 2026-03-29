package com.sliide.usermanager.di

import com.sliide.usermanager.data.db.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory(get()) }
}
