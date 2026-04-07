# MusicXML 谱面播放（Android）说明

## 视觉

- 练习页 [`MusicXmlScoreScreenContent`](composeApp/src/commonMain/kotlin/com/drumpractise/app/score/MusicXmlScoreScreenContent.kt) 在 **播放本段** 时，由 [`StaffPreview.android.kt`](composeApp/src/androidMain/kotlin/com/drumpractise/app/score/StaffPreview.android.kt) 对 **WebView（AndroidView）** 使用 **`Modifier.clip(RoundedCornerShape(20.dp))` + `border`**：高亮为 **3.dp、`#38BDF8`**；未播放时为 **1.dp、10% 黑**。
- 已移除红色竖线播放头。

## 时间线与解析

- [`MusicXmlDrumTimelineParser`](composeApp/src/commonMain/kotlin/com/drumpractise/app/score/musicxml/MusicXmlDrumTimeline.kt) 解析 `divisions`、`backup`/`forward`、和弦；输出 [`DrumNoteHit`](composeApp/src/commonMain/kotlin/com/drumpractise/app/score/musicxml/MusicXmlDrumTimeline.kt)（`displayStep` / `displayOctave` / **`instrumentId`**，来自 `<instrument id="..."/>`）。
- 同拍多音合并为 [`DrumHitGroup`](composeApp/src/commonMain/kotlin/com/drumpractise/app/score/musicxml/MusicXmlDrumTimeline.kt) 时 **保留每个音符**，以便分别触发采样。
- [`buildDrumScorePlaybackSchedule`](composeApp/src/commonMain/kotlin/com/drumpractise/app/score/musicxml/DrumScorePlaybackSchedule.kt) 将解析结果与 BPM、**48 kHz** 采样率结合，得到 **loop 样本长** 与 **按采样偏移排序的击打序列**（`samplesPerQuarter = sampleRate × 60 / bpm`，与节拍器四分音符时长一致）。

## 播放归属与生命周期

- **界面** 仅在 `LaunchedEffect(scorePlaybackPart, bpm, …)` 中：在有效谱面 XML 上调用 [`ScoreHitSoundPlayer.startPlayback`](composeApp/src/commonMain/kotlin/com/drumpractise/app/score/ScoreHitSoundPlayer.kt)，并用 **`try { awaitCancellation() } finally { stopPlayback() }`**，使 keys 变化、协程取消或离开组合时 **总会 stop**。
- **`scorePlaybackPart == null`** 或 **XML 为空** 时先 **`stopPlayback()`**，不启动播放。
- **全局约束**：任意时刻只播一段；**`startPlayback` 会先停掉上一轮**（幂等 `stopPlayback` + 新写出任务）。

## 采样、解码与出声

- Android [`AndroidScoreHitSoundPlayer`](composeApp/src/androidMain/kotlin/com/drumpractise/app/score/ScoreHitSoundPlayer.android.kt) **不再使用 SoundPool**；与节拍器同属 **`RawResourceMonoPcmDecoder` → 48 kHz mono `ShortArray`**，在 **独立 [`AudioTrack`](https://developer.android.com/reference/android/media/AudioTrack)（`MODE_STREAM`、低延迟属性对齐 [`MetronomeEngine.android.kt`](composeApp/src/androidMain/kotlin/com/drumpractise/app/metronome/MetronomeEngine.android.kt)）** 的写出线程里 **按输出采样推进**，在 **事件时刻** 将多路打击乐 **叠加混音** 后 `write`。计时模型见仓库根目录 [`docs/节拍器方案说明.md`](../节拍器方案说明.md) **§4**。
- **`instrument id` → `R.raw`** 表见实现文件顶部 **`DRUM_INSTRUMENT_TO_RAW`**；未映射或空 id 时 **fallback `tr707_weak`**。
- 占位文件位于 [`composeApp/src/androidMain/res/raw/`](composeApp/src/androidMain/res/raw/)：`drum_p1_i36.mp3` … `drum_p1_i52.mp3`（当前为复制 `tr707_weak` 的占位，可直接替换为真实鼓采样，**保持文件名与 `R.raw` 命名规则**）。

## 预加载

- 练习页 `LaunchedEffect(hitSound) { hitSound.warmup() }` 在进入时于 **IO** 上 **解码** 映射表内全部 raw（及默认 `tr707_weak`），避免首次 `startPlayback` 在音频线程里同步解码。

## 与节拍器

- 谱面速度仅使用顶栏 **BPM**；**不**因播放谱面自动开启节拍器。谱面与节拍器可同时开：**两路 `AudioTrack`**，系统混音。
- 详细计时与解码链路仍见 [`docs/节拍器方案说明.md`](../节拍器方案说明.md)。

## 多平台

- **expect** [`rememberScoreHitSoundPlayer`](composeApp/src/commonMain/kotlin/com/drumpractise/app/score/ScoreHitSoundPlayer.kt) 在 **Desktop / iOS** 返回 [`NoOpScoreHitSoundPlayer`](composeApp/src/commonMain/kotlin/com/drumpractise/app/score/ScoreHitSoundPlayer.kt)（不打断、不发声）；Compose 侧仍统一调用 **`startPlayback` / `stopPlayback` / `warmup`**。

## 后续可选

- Verovio MIDI / 逐音符 SVG 高亮。
- 从 `part-list` / `midi-unpitched` 自动生成映射表。
- iOS 上实现与 `AVAudioEngine` 对齐的击打调度（参见节拍器方案说明 §5）。
