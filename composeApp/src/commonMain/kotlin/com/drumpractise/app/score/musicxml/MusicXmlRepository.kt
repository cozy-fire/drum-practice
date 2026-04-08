package com.drumpractise.app.score.musicxml

import drumhero.composeapp.generated.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Loads MusicXML from composeResources/files/ with a small in-memory LRU cache.
 *
 * Note: this caches decoded String content to avoid repeated readBytes + UTF-8 decoding
 * across UI preview cards and playback.
 */
object MusicXmlRepository {
    private const val MAX_ENTRIES = 32
    private val mutex = Mutex()

    // accessOrder=true => LRU
    private val lru =
        object : LinkedHashMap<String, String>(MAX_ENTRIES, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean {
                return size > MAX_ENTRIES
            }
        }

    suspend fun getXml(relativePath: String): String {
        mutex.withLock {
            lru[relativePath]?.let { return it }
        }

        val xml =
            runCatching {
                Res.readBytes("files/$relativePath").decodeToString().trim()
            }.getOrElse { "" }

        mutex.withLock {
            lru[relativePath] = xml
        }
        return xml
    }

    suspend fun prefetch(relativePaths: List<String>) {
        for (p in relativePaths) {
            getXml(p)
        }
    }

    suspend fun invalidate(relativePath: String) {
        mutex.withLock {
            lru.remove(relativePath)
        }
    }

    suspend fun clear() {
        mutex.withLock {
            lru.clear()
        }
    }
}

