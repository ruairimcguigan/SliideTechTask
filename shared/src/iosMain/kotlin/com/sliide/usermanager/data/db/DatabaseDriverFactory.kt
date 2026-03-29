package com.sliide.usermanager.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.sliide.usermanager.db.UserDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = UserDatabase.Schema,
            name = "user_manager.db"
        )
    }
}
