package com.drumpractise.app.util

/**
 * Minimal read/write lock for multiplatform code.
 *
 * - JVM actual uses a real RW lock.
 * - Native actual may fall back to an exclusive lock if needed.
 */
expect class ReadWriteLock() {
    fun <T> read(block: () -> T): T

    fun <T> write(block: () -> T): T
}

