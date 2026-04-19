package com.drumpractise.app.util

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

actual class ReadWriteLock actual constructor() {
    private val lock = ReentrantReadWriteLock()

    actual fun <T> read(block: () -> T): T = lock.read(block)

    actual fun <T> write(block: () -> T): T = lock.write(block)
}

