package com.axveer.lancar.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.axveer.lancar.db.LancarDatabase

actual class DriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(LancarDatabase.Schema, "lancar.db")
}
