package com.drumpractise.app.accentshift.model

import com.drumpractise.app.constance.MetronomeConst
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class AccentShiftTierParams(
    val cardLoopCount: Int,
    val listLoopCount: Int,
    val bpm: Int,
    val mode: SeparationPracticeMode,
) {
    companion object {
        fun default(): AccentShiftTierParams =
            AccentShiftTierParams(
                cardLoopCount = 4,
                listLoopCount = 1,
                bpm = 110,
                mode = SeparationPracticeMode.Sequential,
            )
    }
}

/**
 * 重音移位练习持久化状态：点位选择（1 / 2 / 1+2），每个选项独立 BPM / 循环 / 模式。
 */
@Serializable(with = AccentShiftPracticeStateSerializer::class)
data class AccentShiftPracticeState(
    val selectedTier: AccentShiftTierSelection,
    val tiers: List<AccentShiftTierParams>,
) {
    fun normalized(): AccentShiftPracticeState {
        val base =
            when (tiers.size) {
                3 -> tiers
                else -> List(3) { AccentShiftTierParams.default() }
            }
        return copy(
            tiers =
                base.map { t ->
                    t.copy(
                        cardLoopCount = t.cardLoopCount.coerceIn(1, 99),
                        listLoopCount = t.listLoopCount.coerceIn(1, 99),
                        bpm = t.bpm.coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX),
                    )
                },
        )
    }

    private fun indexFor(selection: AccentShiftTierSelection): Int =
        when (selection) {
            AccentShiftTierSelection.Tier1 -> 0
            AccentShiftTierSelection.Tier2 -> 1
            AccentShiftTierSelection.Combo12 -> 2
        }

    fun paramsFor(selection: AccentShiftTierSelection): AccentShiftTierParams =
        tiers.getOrNull(indexFor(selection)) ?: AccentShiftTierParams.default()

    fun currentParams(): AccentShiftTierParams = paramsFor(selectedTier)

    fun selectTier(selection: AccentShiftTierSelection): AccentShiftPracticeState = copy(selectedTier = selection)

    fun updateCurrentTier(transform: (AccentShiftTierParams) -> AccentShiftTierParams): AccentShiftPracticeState {
        val idx = indexFor(selectedTier)
        val next = tiers.toMutableList()
        next[idx] = transform(next.getOrElse(idx) { AccentShiftTierParams.default() })
        return copy(tiers = next).normalized()
    }

    companion object {
        fun default(): AccentShiftPracticeState =
            AccentShiftPracticeState(
                selectedTier = AccentShiftTierSelection.Tier1,
                tiers = List(3) { AccentShiftTierParams.default() },
            )
    }
}

/**
 * Backward-compatible JSON decode for legacy state:
 * - legacy shape: { selectedTier: Int(1..4), tiers: [4x AccentShiftTierParams] }
 * - new shape:    { selectedTier: AccentShiftTierSelection, tiers: [3x AccentShiftTierParams] }
 *
 * Per product decision: only preserve legacy 1/2; legacy 3/4 falls back to defaults.
 * Combo12 always starts with default params.
 */
internal object AccentShiftPracticeStateSerializer : KSerializer<AccentShiftPracticeState> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("AccentShiftPracticeState", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AccentShiftPracticeState) {
        val jsonEncoder = encoder as? JsonEncoder
        if (jsonEncoder == null) {
            encoder.encodeString(value.toString())
            return
        }
        val json = jsonEncoder.json
        val element =
            json.encodeToJsonElement(
                AccentShiftPracticeStateSurrogate.serializer(),
                AccentShiftPracticeStateSurrogate(
                    selectedTier = value.selectedTier,
                    tiers = value.tiers,
                ),
            )
        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): AccentShiftPracticeState {
        val jsonDecoder = decoder as? JsonDecoder ?: return AccentShiftPracticeState.default()
        val element = jsonDecoder.decodeJsonElement()
        val obj = (element as? JsonObject) ?: return AccentShiftPracticeState.default()

        val tiersEl = obj["tiers"]
        val tiersList = (tiersEl as? JsonArray)?.toList().orEmpty()

        fun decodeTierParams(el: JsonElement?): AccentShiftTierParams? =
            el?.let {
                runCatching { jsonDecoder.json.decodeFromJsonElement(AccentShiftTierParams.serializer(), it) }.getOrNull()
            }

        val legacySelectedTierInt = obj["selectedTier"]?.jsonPrimitive?.intOrNull
        val selectedTierEnum =
            when (obj["selectedTier"]?.jsonPrimitive?.contentOrNull) {
                AccentShiftTierSelection.Tier1.name -> AccentShiftTierSelection.Tier1
                AccentShiftTierSelection.Tier2.name -> AccentShiftTierSelection.Tier2
                AccentShiftTierSelection.Combo12.name -> AccentShiftTierSelection.Combo12
                else -> null
            }

        // New shape (preferred): enum + 3 tiers
        if (selectedTierEnum != null && tiersList.size == 3) {
            val decodedTiers =
                tiersList.map { decodeTierParams(it) ?: AccentShiftTierParams.default() }
            return AccentShiftPracticeState(selectedTier = selectedTierEnum, tiers = decodedTiers).normalized()
        }

        // Legacy shape: int + 4 tiers (only keep 1/2)
        if (legacySelectedTierInt != null && tiersList.size == 4) {
            val tier1 = decodeTierParams(tiersList.getOrNull(0)) ?: AccentShiftTierParams.default()
            val tier2 = decodeTierParams(tiersList.getOrNull(1)) ?: AccentShiftTierParams.default()
            val combo = AccentShiftTierParams.default()
            val sel =
                when (legacySelectedTierInt) {
                    1 -> AccentShiftTierSelection.Tier1
                    2 -> AccentShiftTierSelection.Tier2
                    else -> AccentShiftPracticeState.default().selectedTier
                }
            return AccentShiftPracticeState(selectedTier = sel, tiers = listOf(tier1, tier2, combo)).normalized()
        }

        // Fallback: try decoding surrogate (in case tiers size mismatch but schema is close)
        runCatching {
            val s = jsonDecoder.json.decodeFromJsonElement(AccentShiftPracticeStateSurrogate.serializer(), obj)
            return AccentShiftPracticeState(selectedTier = s.selectedTier, tiers = s.tiers).normalized()
        }

        return AccentShiftPracticeState.default()
    }

    @Serializable
    private data class AccentShiftPracticeStateSurrogate(
        val selectedTier: AccentShiftTierSelection = AccentShiftTierSelection.Tier1,
        val tiers: List<AccentShiftTierParams> = List(3) { AccentShiftTierParams.default() },
    )
}
