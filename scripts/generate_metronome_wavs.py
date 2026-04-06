#!/usr/bin/env python3
"""Generate placeholder TR-707 metronome WAVs for iOS bundle.

Android 使用 `composeApp/src/androidMain/res/raw/tr707_strong.mp3` 等（与 WAV 二选一，勿同名双扩展名）。
新增其它预设时：在此增加条目，并补齐 Kotlin 中 [MetronomeSoundPreset] 与各平台映射。
"""
from __future__ import annotations

import math
import struct
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
IOS_RESOURCES = ROOT / "composeApp" / "src" / "iosMain" / "resources"

# (base_name_without_tier, frequency Hz) — 仅占位音色，与真实 TR-707 无关
TR707_STRONG = ("tr707_strong", 920.0)
TR707_WEAK = ("tr707_weak", 620.0)


def write_wav(path: Path, freq: float, amp: float) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    sample_rate = 44100
    duration_s = 0.045
    n = int(sample_rate * duration_s)
    with wave.open(str(path), "w") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sample_rate)
        for i in range(n):
            t = i / sample_rate
            env = math.exp(-t * 38.0)
            sample = int(amp * env * 32767.0 * math.sin(2.0 * math.pi * freq * t))
            sample = max(-32768, min(32767, sample))
            w.writeframes(struct.pack("<h", sample))


def main() -> None:
    write_wav(IOS_RESOURCES / f"{TR707_STRONG[0]}.wav", TR707_STRONG[1], 0.9)
    write_wav(IOS_RESOURCES / f"{TR707_WEAK[0]}.wav", TR707_WEAK[1], 0.55)
    print(f"Wrote TR-707 placeholders to {IOS_RESOURCES}")


if __name__ == "__main__":
    main()
