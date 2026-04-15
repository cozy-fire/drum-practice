package com.drumpractise.app.accentshift.handmotion

/**
 * 击打侧：四种手型 GIF，与单手「当前击打重/轻 × 下一记重/轻」映射一致（见 [AccentShiftHandMotionPlanner]）。
 * 非击打侧：本拍不播放动画时使用 [Idle]（与击打侧可能出现的 [Up] 区分）。
 */
enum class HandStrokeKind {
    Full,
    Down,
    Tap,
    Up,
    /** 本拍该手不击打动画（静止/不切换 GIF）。 */
    Idle,
}

/**
 * 某一时刻左右手状态；击打侧为 [Full]/[Down]/[Tap]/[Up] 之一，非击打侧为 [Idle]。
 */
data class HandPairState(
    val left: HandStrokeKind,
    val right: HandStrokeKind,
)

data class HandMotionSegment(
    val state: HandPairState,
    val durationMs: Long,
)

data class HandMotionTimeline(
    val segments: List<HandMotionSegment>,
) {
    val totalDurationMs: Long
        get() = segments.sumOf { it.durationMs }
}
