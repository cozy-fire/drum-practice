package com.drumpractise.app.data

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.drumpractise.db.DrumDatabase

private lateinit var appContext: Context

fun initDrumAndroid(context: Context) {
    appContext = context.applicationContext
}

internal fun drumApplicationContext(): Context {
    check(::appContext.isInitialized) { "initDrumAndroid must be called before accessing application context" }
    return appContext
}

actual fun openDrumDatabase(): DrumDatabase {
    val driver = AndroidSqliteDriver(DrumDatabase.Schema, appContext, "drum.db")
    return DrumDatabase(driver)
}
