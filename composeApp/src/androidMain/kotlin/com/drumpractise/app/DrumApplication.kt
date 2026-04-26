package com.drumpractise.app

import android.app.Application
import com.drumpractise.app.data.DrumDatabaseSingleton
import com.drumpractise.app.data.initDrumAndroid
import com.drumpractise.app.score.webview.VerovioWebViewPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class DrumApplication : Application() {

    private val applicationJob = SupervisorJob()
    private val applicationScope = CoroutineScope(applicationJob + Dispatchers.Main)

    override fun onCreate() {
        System.loadLibrary("verovio-android")
        super.onCreate()
        initDrumAndroid(this)
        startKoin {
            androidContext(this@DrumApplication)
            modules(
                module {
                    single { DrumDatabaseSingleton.instance }
                },
            )
        }
        VerovioWebViewPool.install(this, applicationScope)
        VerovioWebViewPool.prewarmAsync()
    }
}
