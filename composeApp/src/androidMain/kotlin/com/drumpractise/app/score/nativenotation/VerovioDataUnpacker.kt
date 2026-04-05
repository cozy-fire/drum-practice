package com.drumpractise.app.score.nativenotation

import android.content.Context
import android.content.res.AssetManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 将 assets 中 `verovio/data` 解压到 filesDir；与 [DrumApplication] 后台预解压、[VerovioScoreViewModel] 共用 [Mutex]，避免并发重复拷贝。
 */
object VerovioDataUnpacker {
    private val mutex = Mutex()

    sealed class Result {
        object Skipped : Result()

        object Copied : Result()
    }

    suspend fun ensureUnpacked(context: Context): Result {
        val app = context.applicationContext
        val targetDir = File(app.filesDir, "verovio/data")
        val sentinel = File(targetDir, "Leipzig.xml")
        if (sentinel.isFile && sentinel.length() > 0L) {
            return Result.Skipped
        }
        return mutex.withLock {
            if (sentinel.isFile && sentinel.length() > 0L) {
                return@withLock Result.Skipped
            }
            withContext(Dispatchers.IO) {
                copyAssetFolder(app.assets, "verovio/data", targetDir)
            }
            Result.Copied
        }
    }

    private fun copyAssetFolder(assetManager: AssetManager, fromAssetPath: String, toPath: File) {
        val files = assetManager.list(fromAssetPath) ?: return
        if (!toPath.exists()) toPath.mkdirs()
        for (filename in files) {
            val assetPath = "$fromAssetPath/$filename"
            val outFile = File(toPath, filename)
            if (assetManager.list(assetPath)?.isNotEmpty() == true) {
                copyAssetFolder(assetManager, assetPath, outFile)
            } else {
                assetManager.open(assetPath).use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
}
