package com.drumpractise.app.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.drumpractise.db.DrumDatabase
import java.io.File
import java.util.Properties

actual fun openDrumDatabase(): DrumDatabase {
    val dir = File(System.getProperty("user.home"), ".drumpractise")
    dir.mkdirs()
    val dbFile = File(dir, "drum_v2.db")
    val driver = JdbcSqliteDriver(
        url = "jdbc:sqlite:${dbFile.absolutePath}",
        properties = Properties(),
        schema = DrumDatabase.Schema,
    )
    return DrumDatabase(driver)
}
