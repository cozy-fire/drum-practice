package com.drumpractise.app.data

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.drumpractise.db.DrumDatabase

actual fun openDrumDatabase(): DrumDatabase {
    val driver = NativeSqliteDriver(DrumDatabase.Schema, "drum.db")
    return DrumDatabase(driver)
}
