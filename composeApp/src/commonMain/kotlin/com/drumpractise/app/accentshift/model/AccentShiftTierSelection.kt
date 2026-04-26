package com.drumpractise.app.accentshift.model

import kotlinx.serialization.Serializable

@Serializable
enum class AccentShiftTierSelection {
    Tier1,
    Tier2,
    Combo12,
    ;

    val label: String
        get() =
            when (this) {
                Tier1 -> "1点位"
                Tier2 -> "2点位"
                Combo12 -> "1点位 + 2点位"
            }
}

