package com.drumpractise.app

import android.app.Application
import com.drumpractise.app.data.initDrumAndroid
import com.drumpractise.app.data.openDrumDatabase
import com.drumpractise.app.score.nativenotation.VerovioDataUnpacker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class DrumApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        System.loadLibrary("verovio-android")
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

        applicationScope.launch {
            runCatching { VerovioDataUnpacker.ensureUnpacked(this@DrumApplication) }
        }
    }
}
