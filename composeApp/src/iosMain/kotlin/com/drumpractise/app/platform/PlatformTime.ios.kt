package com.drumpractise.app.platform

import platform.Foundation.NSDate

actual fun currentEpochMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
