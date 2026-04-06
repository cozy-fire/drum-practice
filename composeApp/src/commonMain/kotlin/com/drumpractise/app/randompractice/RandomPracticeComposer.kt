package com.drumpractise.app.randompractice

import drum_practice.composeapp.generated.resources.Res

data class PracticeComposeItem(
    val rhythmicPath: String,
    val fillPath: String,
    val rhythmicXml: String,
    val fillXml: String,
)

/**
 * 全局随机练习组合器：负责默认谱库清单、资源读取与随机组合。
 *
 * 说明：Compose Resources 不便于运行时列目录，因此使用白名单 paths。
 */
object RandomPracticeComposer {
    private val defaultRhythmicPaths: List<String> =
        listOf(
            "random_practice/rhythmic_01.musicxml",
            "random_practice/rhythmic_02.musicxml",
        )

    private val defaultFillPaths: List<String> =
        listOf(
            "random_practice/fill_01.musicxml",
            "random_practice/fill_02.musicxml",
        )

    suspend fun composeRandom(exclude: PracticeComposeItem? = null): PracticeComposeItem {
        val rhythmicPath = pickRandomAvoiding(defaultRhythmicPaths, exclude?.rhythmicPath)
        val fillPath = pickRandomAvoiding(defaultFillPaths, exclude?.fillPath)
        val rhythmicXml = loadMusicXml(rhythmicPath)
        val fillXml = loadMusicXml(fillPath)
        return PracticeComposeItem(
            rhythmicPath = rhythmicPath,
            fillPath = fillPath,
            rhythmicXml = rhythmicXml,
            fillXml = fillXml,
        )
    }

    private fun pickRandomAvoiding(options: List<String>, exclude: String?): String {
        if (options.isEmpty()) return ""
        if (exclude == null || options.size <= 1) return options.random()
        val candidates = options.filterNot { it == exclude }
        return (if (candidates.isNotEmpty()) candidates else options).random()
    }

    private suspend fun loadMusicXml(path: String): String {
        // Resources are under commonMain/composeResources/files/
        val bytes = Res.readBytes("files/$path")
        return bytes.decodeToString()
    }
}

