package com.drumpractise.app

import android.app.Application
import com.drumpractise.app.data.initDrumAndroid
import com.drumpractise.app.data.openDrumDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class DrumApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initDrumAndroid(this)
        startKoin {
            androidContext(this@DrumApplication)
            modules(
                module {
                    single { openDrumDatabase() }
                },
            )
        }
    }
}
