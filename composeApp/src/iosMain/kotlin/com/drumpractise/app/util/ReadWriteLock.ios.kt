package com.drumpractise.app.util

import platform.Foundation.NSLock

/**
 * iOS implementation: uses a single exclusive lock.
 * This keeps correctness (thread-safety) while staying dependency-free.
 */
actual class ReadWriteLock actual constructor() {
    private val lock = NSLock()

    actual fun <T> read(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }

    actual fun <T> write(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }
}

