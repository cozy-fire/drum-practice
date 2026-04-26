package com.drumpractise.app.data

import com.drumpractise.db.DrumDatabase

object DrumDatabaseSingleton {
    val instance: DrumDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { openDrumDatabase() }
}

