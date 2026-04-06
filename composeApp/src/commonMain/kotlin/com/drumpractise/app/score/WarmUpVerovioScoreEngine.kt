package com.drumpractise.app.score

/** Android：解压数据并在主线程初始化 Verovio toolkit 与示例谱；其他平台 no-op。 */
expect suspend fun warmUpVerovioScoreEngine()
