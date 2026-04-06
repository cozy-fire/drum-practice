package com.drumpractise.app.score

import com.drumpractise.app.data.drumApplicationContext
import com.drumpractise.app.score.nativenotation.VerovioScoreRuntime

actual suspend fun warmUpVerovioScoreEngine() {
    VerovioScoreRuntime.warmUp(drumApplicationContext())
}
