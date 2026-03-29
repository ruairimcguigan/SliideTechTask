package com.sliide.usermanager.data.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific SQLDelight driver factory.
 * Uses KMP expect/actual to provide the correct driver per platform.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
