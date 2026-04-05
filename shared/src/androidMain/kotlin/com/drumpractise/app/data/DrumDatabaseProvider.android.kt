package com.drumpractise.app.data

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.drumpractise.db.DrumDatabase

private lateinit var appContext: Context

fun initDrumAndroid(context: Context) {
    appContext = context.applicationContext
}

actual fun openDrumDatabase(): DrumDatabase {
    val driver = AndroidSqliteDriver(DrumDatabase.Schema, appContext, "drum.db")
    return DrumDatabase(driver)
}
